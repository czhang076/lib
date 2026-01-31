package server;

import common.Constants;
import common.Marshaller;
import common.NetworkUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ServerMain {
    private static BankService bankService = new BankService();
    private static SubscriberManager subscriberManager = new SubscriberManager();
    private static boolean enableHistory = true;
    private static final byte[] UNKNOWN_RESPONSE = "???".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            if (mode.equals("alo")) {
                enableHistory = false;
            } else if (mode.equals("amo")) {
                enableHistory = true;
            }
        }
        System.out.println("Starting Server on port " + Constants.SERVER_PORT + "...");
        System.out.println("Invocation Semantics: " + (enableHistory ? "At-most-once" : "At-least-once"));
        try (DatagramSocket socket = new DatagramSocket(Constants.SERVER_PORT)) {
            new SubscriberCleanerThread(subscriberManager, 60_000L).start();
            while (true) {
                System.out.println("Waiting for request...");
                DatagramPacket requestPacket = NetworkUtil.receive(socket);
                subscriberManager.cleanupExpired();

                // Protocol: [Length (int)] [ReqID (int)] [OpCode (int)] [Payload...]
                byte[] data = Arrays.copyOf(requestPacket.getData(), requestPacket.getLength());
                ByteBuffer reqBuf = ByteBuffer.wrap(data);

                if (data.length < 12) {
                    System.out.println("Packet too short");
                    sendUnknown(socket, requestPacket);
                    continue;
                }

                int length = Marshaller.unpackInt(reqBuf);
                if (length != data.length) {
                    System.out.println("Length mismatch: header=" + length + ", actual=" + data.length);
                    sendUnknown(socket, requestPacket);
                    continue;
                }

                int reqID = Marshaller.unpackInt(reqBuf);
                int opCode = Marshaller.unpackInt(reqBuf);

                System.out.println("Received ReqID: " + reqID + ", OpCode: " + opCode);

                // === At-most-once: Check History ===
                String clientIp = requestPacket.getAddress().getHostAddress();
                int clientPort = requestPacket.getPort();

                if (enableHistory && HistoryCache.hasResponse(clientIp, clientPort, reqID)) {
                    System.out.println("Duplicate Request detected (ReqID=" + reqID + "). Resending cached response.");
                    byte[] cachedResponse = HistoryCache.getResponse(clientIp, clientPort, reqID);
                    NetworkUtil.send(socket, requestPacket.getAddress(), requestPacket.getPort(), cachedResponse);
                    continue;
                }

                // Prepare response buffer
                ByteBuffer resBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
                Marshaller.packInt(resBuf, 0); // Length placeholder
                Marshaller.packInt(resBuf, reqID); // Header 1

                try {
                    switch (opCode) {
                        case Constants.OP_OPEN_ACCOUNT: {
                            String name = Marshaller.unpackString(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            String curr = Marshaller.unpackString(reqBuf);
                            double bal = Marshaller.unpackDouble(reqBuf);
                            int accId = bankService.createAccount(name, pwd, curr, bal);
                            
                            Marshaller.packInt(resBuf, Constants.STATUS_OK); // Status
                            Marshaller.packString(resBuf, "Account Created. ID: " + accId); // Message
                            Marshaller.packInt(resBuf, accId); // Paylod: AccountID

                            subscriberManager.notifySubscribers(socket, bankService.getAccount(accId));
                            break;
                        }
                        case Constants.OP_DEPOSIT: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String name = Marshaller.unpackString(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            String curr = Marshaller.unpackString(reqBuf);
                            double amount = Marshaller.unpackDouble(reqBuf);
                            double newBal = bankService.deposit(id, name, pwd, curr, amount);

                            subscriberManager.notifySubscribers(socket, bankService.getAccount(id));
                            
                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Deposit Success. New Balance: " + newBal);
                            Marshaller.packDouble(resBuf, newBal);
                            break;
                        }
                        case Constants.OP_WITHDRAW: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String name = Marshaller.unpackString(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            String curr = Marshaller.unpackString(reqBuf);
                            double amount = Marshaller.unpackDouble(reqBuf);
                            double newBal = bankService.withdraw(id, name, pwd, curr, amount);

                            subscriberManager.notifySubscribers(socket, bankService.getAccount(id));

                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Withdraw Success. New Balance: " + newBal);
                            Marshaller.packDouble(resBuf, newBal);
                            break;
                        }
                        case Constants.OP_CLOSE_ACCOUNT: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String name = Marshaller.unpackString(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);

                            Account closed = bankService.closeAccount(id, name, pwd);
                            if (closed != null) {
                                subscriberManager.notifySubscribers(socket, closed);
                            }

                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Account Closed. ID: " + id);
                            break;
                        }
                        case Constants.OP_MONITOR: {
                            int intervalSeconds = Marshaller.unpackInt(reqBuf);
                            subscriberManager.addSubscriber(requestPacket.getAddress(), requestPacket.getPort(), intervalSeconds);
                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Monitor Registered");
                            break;
                        }
                        case Constants.OP_CHECK_BALANCE: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String name = Marshaller.unpackString(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            double bal = bankService.checkBalance(id, name, pwd);

                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Current Balance: " + bal);
                            Marshaller.packDouble(resBuf, bal);
                            break;
                        }
                        case Constants.OP_TRANSFER: {
                            int fromId = Marshaller.unpackInt(reqBuf);
                            String fromName = Marshaller.unpackString(reqBuf);
                            String fromPwd = Marshaller.unpackString(reqBuf);
                            int toId = Marshaller.unpackInt(reqBuf);
                            double amount = Marshaller.unpackDouble(reqBuf);

                            double newBal = bankService.transfer(fromId, fromName, fromPwd, toId, amount);

                            subscriberManager.notifySubscribers(socket, bankService.getAccount(fromId));
                            subscriberManager.notifySubscribers(socket, bankService.getAccount(toId));

                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Transfer Success. New Balance: " + newBal);
                            Marshaller.packDouble(resBuf, newBal);
                            break;
                        }
                        case Constants.OP_GET_ACCOUNT_INFO: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String name = Marshaller.unpackString(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            Account account = bankService.getAccountInfo(id, name, pwd);

                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Account Info");
                            Marshaller.packInt(resBuf, account.getAccountNumber());
                            Marshaller.packString(resBuf, account.getName());
                            Marshaller.packString(resBuf, account.getCurrencyType().name());
                            Marshaller.packDouble(resBuf, account.getBalance());
                            Marshaller.packLong(resBuf, account.getVersion());
                            break;
                        }
                        default:
                            Marshaller.packInt(resBuf, Constants.STATUS_ERROR);
                            Marshaller.packString(resBuf, "Unknown OpCode");
                    }
                } catch (Exception e) {
                    System.out.println("Error processing request: " + e.getMessage());
                    sendUnknown(socket, requestPacket);
                    // Reset buffer to just after ReqID to write error
                    resBuf.position(4); // Skip length placeholder
                    Marshaller.packInt(resBuf, reqID);
                    Marshaller.packInt(resBuf, Constants.STATUS_ERROR);
                    Marshaller.packString(resBuf, "Error: " + e.getMessage());
                }

                resBuf.putInt(0, resBuf.position());
                byte[] responseData = Arrays.copyOf(resBuf.array(), resBuf.position());
                
                // === At-most-once: Save History ===
                if (enableHistory) {
                    HistoryCache.putResponse(clientIp, clientPort, reqID, responseData);
                }

                NetworkUtil.send(socket, requestPacket.getAddress(), requestPacket.getPort(), responseData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendUnknown(DatagramSocket socket, DatagramPacket requestPacket) {
        try {
            NetworkUtil.send(socket, requestPacket.getAddress(), requestPacket.getPort(), UNKNOWN_RESPONSE);
        } catch (Exception e) {
            System.out.println("Failed to send unknown response: " + e.getMessage());
        }
    }
}
