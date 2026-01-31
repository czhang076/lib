package client;

import common.Constants;
import common.Marshaller;
import common.NetworkUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
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
                System.out.println("3. Deposit");
                System.out.println("4. Withdraw");
                System.out.println("5. Check Balance");
                System.out.println("6. Transfer");
                System.out.println("7. Monitor Updates");
                System.out.println("8. Account Info");
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

                ByteBuffer reqBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
                int reqID = reqIdCounter++;
                Marshaller.packInt(reqBuf, 0); // Length placeholder
                Marshaller.packInt(reqBuf, reqID);
                int monitorIntervalSeconds = -1;

                switch (choice) {
                    case 1: // Open Account
                        System.out.print("Enter Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String pwd = scanner.nextLine();
                        String curr = readCurrency(scanner);
                        System.out.print("Enter Initial Balance: ");
                        double bal = Double.parseDouble(scanner.nextLine());

                        Marshaller.packInt(reqBuf, Constants.OP_OPEN_ACCOUNT);
                        Marshaller.packString(reqBuf, name);
                        Marshaller.packString(reqBuf, pwd);
                        Marshaller.packString(reqBuf, curr);
                        Marshaller.packDouble(reqBuf, bal);
                        break;

                    case 2: // Close Account
                        System.out.print("Enter Account ID: ");
                        int closeId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String closeName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String closePwd = scanner.nextLine();

                        Marshaller.packInt(reqBuf, Constants.OP_CLOSE_ACCOUNT);
                        Marshaller.packInt(reqBuf, closeId);
                        Marshaller.packString(reqBuf, closeName);
                        Marshaller.packString(reqBuf, closePwd);
                        break;

                    case 3: // Deposit
                        System.out.print("Enter Account ID: ");
                        int accId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String depName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String depPwd = scanner.nextLine();
                        String depCurr = readCurrency(scanner);
                        System.out.print("Enter Amount: ");
                        double amt = Double.parseDouble(scanner.nextLine());

                        Marshaller.packInt(reqBuf, Constants.OP_DEPOSIT);
                        Marshaller.packInt(reqBuf, accId);
                        Marshaller.packString(reqBuf, depName);
                        Marshaller.packString(reqBuf, depPwd);
                        Marshaller.packString(reqBuf, depCurr);
                        Marshaller.packDouble(reqBuf, amt);
                        break;

                    case 4: // Withdraw
                        System.out.print("Enter Account ID: ");
                        int wId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String wName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String wPwd = scanner.nextLine();
                        String wCurr = readCurrency(scanner);
                        System.out.print("Enter Amount: ");
                        double wAmt = Double.parseDouble(scanner.nextLine());

                        Marshaller.packInt(reqBuf, Constants.OP_WITHDRAW);
                        Marshaller.packInt(reqBuf, wId);
                        Marshaller.packString(reqBuf, wName);
                        Marshaller.packString(reqBuf, wPwd);
                        Marshaller.packString(reqBuf, wCurr);
                        Marshaller.packDouble(reqBuf, wAmt);
                        break;

                    case 5: // Balance
                        System.out.print("Enter Account ID: ");
                        int balId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String balName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String balPwd = scanner.nextLine();

                        Marshaller.packInt(reqBuf, Constants.OP_CHECK_BALANCE);
                        Marshaller.packInt(reqBuf, balId);
                        Marshaller.packString(reqBuf, balName);
                        Marshaller.packString(reqBuf, balPwd);
                        break;

                    case 6: // Transfer
                        System.out.print("Enter From Account ID: ");
                        int fromId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String fromName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String fromPwd = scanner.nextLine();
                        System.out.print("Enter To Account ID: ");
                        int toId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Amount: ");
                        double tAmt = Double.parseDouble(scanner.nextLine());

                        Marshaller.packInt(reqBuf, Constants.OP_TRANSFER);
                        Marshaller.packInt(reqBuf, fromId);
                        Marshaller.packString(reqBuf, fromName);
                        Marshaller.packString(reqBuf, fromPwd);
                        Marshaller.packInt(reqBuf, toId);
                        Marshaller.packDouble(reqBuf, tAmt);
                        break;

                    case 7: // Monitor
                        System.out.print("Enter Monitor Interval (seconds): ");
                        int interval = Integer.parseInt(scanner.nextLine());
                        monitorIntervalSeconds = interval;
                        Marshaller.packInt(reqBuf, Constants.OP_MONITOR);
                        Marshaller.packInt(reqBuf, interval);
                        break;

                    case 8: // Account Info
                        System.out.print("Enter Account ID: ");
                        int infoId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Name: ");
                        String infoName = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String infoPwd = scanner.nextLine();

                        Marshaller.packInt(reqBuf, Constants.OP_GET_ACCOUNT_INFO);
                        Marshaller.packInt(reqBuf, infoId);
                        Marshaller.packString(reqBuf, infoName);
                        Marshaller.packString(reqBuf, infoPwd);
                        break;

                    default:
                        System.out.println("Invalid option.");
                        continue;
                }

                reqBuf.putInt(0, reqBuf.position());
                byte[] reqData = Arrays.copyOf(reqBuf.array(), reqBuf.position());
                
                // === Send with Retry (At-least-once) ===
                int maxRetries = enableRetry ? 5 : 1;
                int retries = 0;
                boolean received = false;
                DatagramPacket replyPacket = null;
                
                socket.setSoTimeout(1000); // 1.0 second timeout

                while (retries < maxRetries && !received) {
                    try {
                        NetworkUtil.send(socket, serverAddress, serverPort, reqData);
                        
                        // Receive
                        System.out.println("Waiting for reply (Attempt " + (retries + 1) + ")...");
                        replyPacket = NetworkUtil.receive(socket);
                        received = true; // Success!

                    } catch (java.net.SocketTimeoutException e) {
                        retries++;
                        System.out.println("Timeout! Retrying (" + retries + "/" + maxRetries + ")...");
                    }
                }

                if (!received) {
                    System.out.println("Error: No response from server after " + maxRetries + " attempts.");
                    continue; // Skip processing response
                }
                
                byte[] resData = Arrays.copyOf(replyPacket.getData(), replyPacket.getLength());
                ByteBuffer resBuf = ByteBuffer.wrap(resData);

                int length = Marshaller.unpackInt(resBuf);
                if (length != resData.length) {
                    System.out.println("Invalid response length. Ignoring.");
                    continue;
                }

                int resReqID = Marshaller.unpackInt(resBuf);
                int status = Marshaller.unpackInt(resBuf);
                String msg = Marshaller.unpackString(resBuf);

                System.out.println("Reply [ReqID=" + resReqID + ", Status=" + status + "]: " + msg);

                if (status == Constants.STATUS_OK) {
                    // Extract payload if needed
                    if (choice == 1) {
                         // Open Account returns ID
                         int newId = Marshaller.unpackInt(resBuf);
                         System.out.println(">> Created Account ID: " + newId);
                    } else if (choice == 3 || choice == 4 || choice == 5 || choice == 6) {
                         // Deposit/Withdraw/Balance/Transfer returns double balance
                         double newBal = Marshaller.unpackDouble(resBuf);
                         System.out.println(">> Balance: " + newBal);
                    } else if (choice == 7) {
                        System.out.println("Entering monitor mode...");
                        enterMonitorLoop(socket, monitorIntervalSeconds);
                    } else if (choice == 8) {
                        int id = Marshaller.unpackInt(resBuf);
                        String name = Marshaller.unpackString(resBuf);
                        String curr = Marshaller.unpackString(resBuf);
                        double bal = Marshaller.unpackDouble(resBuf);
                        long ver = Marshaller.unpackLong(resBuf);
                        System.out.println(">> Account: ID=" + id + ", Name=" + name + ", Currency=" + curr + ", Balance=" + bal + ", Version=" + ver);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enterMonitorLoop(DatagramSocket socket, int intervalSeconds) {
        System.out.println("--- Entered Monitor Mode (Interval: " + intervalSeconds + "s) ---");
        long localVersion = 0;
        long endTime = System.currentTimeMillis() + (intervalSeconds * 1000L);
        try {
            socket.setSoTimeout(1000); // Poll every second to check interval
            while (System.currentTimeMillis() < endTime) {
                try {
                    DatagramPacket packet = NetworkUtil.receive(socket);
                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                    ByteBuffer buf = ByteBuffer.wrap(data);

                    if (data.length < 12) {
                        continue;
                    }

                    int length = Marshaller.unpackInt(buf);
                    if (length != data.length) {
                        continue;
                    }

                    int reqID = Marshaller.unpackInt(buf);
                    int opCode = Marshaller.unpackInt(buf);

                    if (opCode == Constants.OP_CALLBACK) {
                        long serverVersion = Marshaller.unpackLong(buf);
                        int accountId = Marshaller.unpackInt(buf);
                        String name = Marshaller.unpackString(buf);
                        String currency = Marshaller.unpackString(buf);
                        double balance = Marshaller.unpackDouble(buf);

                        if (serverVersion > localVersion) {
                            System.out.println("Update Received: ID=" + accountId + ", Name=" + name + ", Currency=" + currency + ", Balance=" + balance + ", Version=" + serverVersion);
                            localVersion = serverVersion;
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // loop to check interval
                }
            }
            System.out.println("--- Monitor interval ended ---");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.setSoTimeout(1000);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static String readCurrency(Scanner scanner) {
        while (true) {
            System.out.print("Enter Currency (SGD/USD): ");
            String curr = scanner.nextLine();
            String norm = curr.trim().toUpperCase();
            if ("SGD".equals(norm) || "USD".equals(norm)) {
                return norm;
            }
            System.out.println("Invalid currency. Allowed values: SGD or USD.");
        }
    }
}
