package client;

import common.Constants;
import common.Marshaller;
import common.NetworkUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;

public class ClientMain {
    private static int reqIdCounter = new Random().nextInt(1000);

    public static void main(String[] args) {
        String serverIp = "127.0.0.1";
        int serverPort = Constants.SERVER_PORT;
        if (args.length > 0) {
            String hostArg = args[0];
            if (hostArg.contains(":")) {
                String[] parts = hostArg.split(":", 2);
                serverIp = parts[0];
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    try {
                        serverPort = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port, using default " + Constants.SERVER_PORT);
                        serverPort = Constants.SERVER_PORT;
                    }
                }
            } else {
                serverIp = hostArg;
            }
        }
        String mode = (args.length > 1) ? args[1].trim().toLowerCase() : "amo";
        boolean enableRetry = mode.equals("alo");

        System.out.println("Starting Client. Target Server: " + serverIp + ":" + serverPort);
        System.out.println("Invocation Semantics: " + (enableRetry ? "At-least-once" : "At-most-once"));
        
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress serverAddress = InetAddress.getByName(serverIp);

            while (true) {
                System.out.println("\n--- Bank Client Menu ---");
                System.out.println("1. Open Account");
                System.out.println("2. Close Account");
                System.out.println("3. Check Balance");
                System.out.println("4. Deposit");
                System.out.println("5. Withdraw");
                System.out.println("6. Transfer");
                System.out.println("7. Exchange");
                System.out.println("8. Monitor");
                System.out.println("0. Exit");
                System.out.print("Select an option: ");

                String input = scanner.nextLine();
                int choice = -1;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (choice == 0) break;

                int reqID = reqIdCounter++;
                ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
                int opCode;

                switch (choice) {
                    case 1: // Open Account
                        System.out.print("Enter Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String pwd = scanner.nextLine();
                        String curr = readCurrency(scanner);
                        System.out.print("Enter Initial Balance: ");
                        float bal = Float.parseFloat(scanner.nextLine());

                        opCode = Constants.OP_OPEN_ACCOUNT;
                        Marshaller.packString(payloadBuf, name);
                        Marshaller.packString(payloadBuf, pwd);
                        Marshaller.packFloat(payloadBuf, bal);
                        Marshaller.packString(payloadBuf, curr);
                        break;

                    case 2: // Close Account
                        System.out.print("Enter Account ID: ");
                        int closeId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String closeName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String closePwd = scanner.nextLine();

                        opCode = Constants.OP_CLOSE_ACCOUNT;
                        Marshaller.packInt(payloadBuf, closeId);
                        Marshaller.packString(payloadBuf, closeName);
                        Marshaller.packString(payloadBuf, closePwd);
                        break;

                    case 3: // Check Balance
                        System.out.print("Enter Account ID: ");
                        int balId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String balName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String balPwd = scanner.nextLine();
                        String balCurr = readCurrency(scanner);

                        opCode = Constants.OP_CHECK_BALANCE;
                        Marshaller.packInt(payloadBuf, balId);
                        Marshaller.packString(payloadBuf, balName);
                        Marshaller.packString(payloadBuf, balPwd);
                        Marshaller.packString(payloadBuf, balCurr);
                        break;

                    case 4: // Deposit
                        System.out.print("Enter Account ID: ");
                        int depId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String depName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String depPwd = scanner.nextLine();
                        String depCurr = readCurrency(scanner);
                        System.out.print("Enter Amount: ");
                        float depAmt = Float.parseFloat(scanner.nextLine());

                        opCode = Constants.OP_DEPOSIT;
                        Marshaller.packInt(payloadBuf, depId);
                        Marshaller.packString(payloadBuf, depName);
                        Marshaller.packString(payloadBuf, depPwd);
                        Marshaller.packString(payloadBuf, depCurr);
                        Marshaller.packFloat(payloadBuf, depAmt);
                        break;

                    case 5: // Withdraw
                        System.out.print("Enter Account ID: ");
                        int wId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String wName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String wPwd = scanner.nextLine();
                        String wCurr = readCurrency(scanner);
                        System.out.print("Enter Amount: ");
                        float wAmt = Float.parseFloat(scanner.nextLine());

                        opCode = Constants.OP_WITHDRAW;
                        Marshaller.packInt(payloadBuf, wId);
                        Marshaller.packString(payloadBuf, wName);
                        Marshaller.packString(payloadBuf, wPwd);
                        Marshaller.packString(payloadBuf, wCurr);
                        Marshaller.packFloat(payloadBuf, wAmt);
                        break;

                    case 6: // Transfer
                        System.out.print("Enter Sender Account ID: ");
                        int senderId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String senderName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String senderPwd = scanner.nextLine();
                        String tCurr = readCurrency(scanner);
                        System.out.print("Enter Amount: ");
                        float tAmt = Float.parseFloat(scanner.nextLine());
                        System.out.print("Enter Receiver Account ID: ");
                        int receiverId = Integer.parseInt(scanner.nextLine());

                        opCode = Constants.OP_TRANSFER;
                        Marshaller.packInt(payloadBuf, senderId);
                        Marshaller.packString(payloadBuf, senderName);
                        Marshaller.packString(payloadBuf, senderPwd);
                        Marshaller.packString(payloadBuf, tCurr);
                        Marshaller.packFloat(payloadBuf, tAmt);
                        Marshaller.packInt(payloadBuf, receiverId);
                        break;

                    case 7: // Exchange
                        System.out.print("Enter Account ID: ");
                        int exId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String exName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String exPwd = scanner.nextLine();
                        System.out.print("Enter From Currency: ");
                        String fromCurr = scanner.nextLine().trim().toUpperCase();
                        System.out.print("Enter To Currency: ");
                        String toCurr = scanner.nextLine().trim().toUpperCase();
                        System.out.print("Enter Amount (target currency): ");
                        float exAmt = Float.parseFloat(scanner.nextLine());

                        opCode = Constants.OP_EXCHANGE;
                        Marshaller.packInt(payloadBuf, exId);
                        Marshaller.packString(payloadBuf, exName);
                        Marshaller.packString(payloadBuf, exPwd);
                        Marshaller.packString(payloadBuf, fromCurr);
                        Marshaller.packString(payloadBuf, toCurr);
                        Marshaller.packFloat(payloadBuf, exAmt);
                        break;

                    case 8: // Monitor
                        System.out.print("Enter Duration (milliseconds): ");
                        long durationMillis = Long.parseLong(scanner.nextLine());

                        opCode = Constants.OP_MONITOR;
                        Marshaller.packLong(payloadBuf, durationMillis);
                        break;

                    default:
                        System.out.println("Invalid option.");
                        continue;
                }

                ByteBuffer reqBuf = ByteBuffer.allocate(8 + Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
                reqBuf.putInt(reqID);
                reqBuf.putInt(opCode);
                reqBuf.put(payloadBuf.array());
                byte[] reqData = reqBuf.array();

                DatagramPacket replyPacket = sendWithRetry(socket, serverAddress, serverPort, reqData, enableRetry);
                if (replyPacket == null) {
                    continue;
                }

                byte[] resData = Arrays.copyOf(replyPacket.getData(), replyPacket.getLength());
                if (resData.length < 8) {
                    System.out.println("Invalid response length. Ignoring.");
                    continue;
                }

                ByteBuffer resBuf = ByteBuffer.wrap(resData).order(ByteOrder.LITTLE_ENDIAN);
                int resId = resBuf.getInt();
                int status = resBuf.getInt();
                byte[] payloadBytes = new byte[Math.min(Constants.BUFFER_SIZE, resData.length - 8)];
                resBuf.get(payloadBytes);
                String msg = decodeNullTerminated(payloadBytes);
                System.out.println("Reply [ReqID=" + resId + ", Status=" + status + "]: " + msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static DatagramPacket sendWithRetry(DatagramSocket socket, InetAddress serverAddress, int serverPort, byte[] reqData, boolean enableRetry) throws Exception {
        int retries = 0;
        DatagramPacket replyPacket = null;

        socket.setSoTimeout(1000); // 1.0 second timeout
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                NetworkUtil.send(socket, serverAddress, serverPort, reqData);
                System.out.println("Waiting for reply (Attempt " + (retries + 1) + ")...");
                replyPacket = NetworkUtil.receive(socket);
                return replyPacket;
            } catch (java.net.SocketTimeoutException e) {
                retries++;
                if (!enableRetry) {
                    System.out.println("Error: No response from server.");
                    return null;
                }
                if (System.currentTimeMillis() - startTime >= Constants.RETRY_TIMEOUT_MS) {
                    System.out.println("Error: No response from server within retry timeout.");
                    return null;
                }
                System.out.println("Timeout! Retrying (" + retries + ")...");
            }
        }
    }

    private static String readCurrency(Scanner scanner) {
        while (true) {
            System.out.print("Enter Currency (USD/RMB/SGD/JPY/BPD): ");
            String curr = scanner.nextLine();
            String norm = curr.trim().toUpperCase();
            if ("USD".equals(norm) || "RMB".equals(norm) || "SGD".equals(norm) || "JPY".equals(norm) || "BPD".equals(norm)) {
                return norm;
            }
            System.out.println("Invalid currency. Allowed values: USD, RMB, SGD, JPY, BPD.");
        }
    }

    private static String decodeNullTerminated(byte[] payloadBytes) {
        int end = 0;
        while (end < payloadBytes.length && payloadBytes[end] != 0) {
            end++;
        }
        return new String(payloadBytes, 0, end, StandardCharsets.UTF_8).trim();
    }
}
