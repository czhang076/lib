package server;

import common.Constants;
import common.Marshaller;
import common.NetworkUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ServerMain {
    private static BankService bankService = new BankService();
    private static SubscriberManager subscriberManager = new SubscriberManager();

    public static void main(String[] args) {
        System.out.println("Starting Server on port " + Constants.SERVER_PORT + "...");
        try (DatagramSocket socket = new DatagramSocket(Constants.SERVER_PORT)) {
            while (true) {
                System.out.println("Waiting for request...");
                DatagramPacket requestPacket = NetworkUtil.receive(socket);

                // Protocol: [ReqID (int)] [OpCode (int)] [Payload...]
                byte[] data = Arrays.copyOf(requestPacket.getData(), requestPacket.getLength());
                ByteBuffer reqBuf = ByteBuffer.wrap(data);

                if (data.length < 8) {
                    System.out.println("Packet too short");
                    continue;
                }

                int reqID = Marshaller.unpackInt(reqBuf);
                int opCode = Marshaller.unpackInt(reqBuf);

                System.out.println("Received ReqID: " + reqID + ", OpCode: " + opCode);

                // === At-most-once: Check History ===
                String clientIp = requestPacket.getAddress().getHostAddress();
                int clientPort = requestPacket.getPort();

                if (HistoryCache.hasResponse(clientIp, clientPort, reqID)) {
                    System.out.println("Duplicate Request detected (ReqID=" + reqID + "). Resending cached response.");
                    byte[] cachedResponse = HistoryCache.getResponse(clientIp, clientPort, reqID);
                    NetworkUtil.send(socket, requestPacket.getAddress(), requestPacket.getPort(), cachedResponse);
                    continue;
                }

                // Prepare response buffer
                ByteBuffer resBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
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
                            break;
                        }
                        case Constants.OP_DEPOSIT: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            double amount = Marshaller.unpackDouble(reqBuf);
                            double newBal = bankService.deposit(id, pwd, amount);

                            subscriberManager.notifySubscribers(socket, bankService.getAccount(id));
                            
                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Deposit Success. New Balance: " + newBal);
                            Marshaller.packDouble(resBuf, newBal);
                            break;
                        }
                        case Constants.OP_MONITOR: {
                            subscriberManager.addSubscriber(requestPacket.getAddress(), requestPacket.getPort());
                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Monitor Registered");
                            break;
                        }
                        case Constants.OP_CHECK_BALANCE: {
                            int id = Marshaller.unpackInt(reqBuf);
                            String pwd = Marshaller.unpackString(reqBuf);
                            double bal = bankService.checkBalance(id, pwd);

                            Marshaller.packInt(resBuf, Constants.STATUS_OK);
                            Marshaller.packString(resBuf, "Current Balance: " + bal);
                            Marshaller.packDouble(resBuf, bal);
                            break;
                        }
                        default:
                            Marshaller.packInt(resBuf, Constants.STATUS_ERROR);
                            Marshaller.packString(resBuf, "Unknown OpCode");
                    }
                } catch (Exception e) {
                    System.out.println("Error processing request: " + e.getMessage());
                    // Reset buffer to just after ReqID to write error
                    resBuf.position(4); // Skip reqID
                    Marshaller.packInt(resBuf, Constants.STATUS_ERROR);
                    Marshaller.packString(resBuf, "Error: " + e.getMessage());
                }

                byte[] responseData = Arrays.copyOf(resBuf.array(), resBuf.position());
                
                // === At-most-once: Save History ===
                HistoryCache.putResponse(clientIp, clientPort, reqID, responseData);

                NetworkUtil.send(socket, requestPacket.getAddress(), requestPacket.getPort(), responseData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
