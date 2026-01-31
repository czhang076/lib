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
        if (args.length > 0) {
            serverIp = args[0];
        }

        System.out.println("Starting Client. Target Server: " + serverIp + ":" + Constants.SERVER_PORT);
        
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {
            
            InetAddress serverAddress = InetAddress.getByName(serverIp);

            while (true) {
                System.out.println("\n--- Bank Client Menu ---");
                System.out.println("1. Open Account");
                System.out.println("2. Deposit");
                System.out.println("3. Check Balance");
                System.out.println("4. Monitor Updates");
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
                Marshaller.packInt(reqBuf, reqID);

                switch (choice) {
                    case 1: // Open Account
                        System.out.print("Enter Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter Password: ");
                        String pwd = scanner.nextLine();
                        System.out.print("Enter Currency (SGD/USD): ");
                        String curr = scanner.nextLine();
                        System.out.print("Enter Initial Balance: ");
                        double bal = Double.parseDouble(scanner.nextLine());

                        Marshaller.packInt(reqBuf, Constants.OP_OPEN_ACCOUNT);
                        Marshaller.packString(reqBuf, name);
                        Marshaller.packString(reqBuf, pwd);
                        Marshaller.packString(reqBuf, curr);
                        Marshaller.packDouble(reqBuf, bal);
                        break;

                    case 2: // Deposit
                        System.out.print("Enter Account ID: ");
                        int accId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Password: ");
                        String depPwd = scanner.nextLine();
                        System.out.print("Enter Amount: ");
                        double amt = Double.parseDouble(scanner.nextLine());

                        Marshaller.packInt(reqBuf, Constants.OP_DEPOSIT);
                        Marshaller.packInt(reqBuf, accId);
                        Marshaller.packString(reqBuf, depPwd);
                        Marshaller.packDouble(reqBuf, amt);
                        break;
                    
                    case 3: // Balance
                        System.out.print("Enter Account ID: ");
                        int balId = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Password: ");
                        String balPwd = scanner.nextLine();

                        Marshaller.packInt(reqBuf, Constants.OP_CHECK_BALANCE);
                        Marshaller.packInt(reqBuf, balId);
                        Marshaller.packString(reqBuf, balPwd);
                        break;

                    case 4: // Monitor
                        Marshaller.packInt(reqBuf, Constants.OP_MONITOR);
                        break;

                    default:
                        System.out.println("Invalid option.");
                        continue;
                }

                byte[] reqData = Arrays.copyOf(reqBuf.array(), reqBuf.position());
                
                // === Send with Retry (At-least-once) ===
                int maxRetries = 5;
                int retries = 0;
                boolean received = false;
                DatagramPacket replyPacket = null;
                
                socket.setSoTimeout(1000); // 1.0 second timeout

                while (retries < maxRetries && !received) {
                    try {
                        NetworkUtil.send(socket, serverAddress, Constants.SERVER_PORT, reqData);
                        
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
                    } else if (choice == 2 || choice == 3) {
                         // Deposit/Balance returns double balance
                         double newBal = Marshaller.unpackDouble(resBuf);
                         System.out.println(">> Balance: " + newBal);
                    } else if (choice == 4) {
                        enterMonitorLoop(socket);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enterMonitorLoop(DatagramSocket socket) {
        System.out.println("--- Entered Monitor Mode (Ctrl+C to exit) ---");
        long localVersion = 0;
        try {
            socket.setSoTimeout(0); // Infinite timeout
            while (true) {
                DatagramPacket packet = NetworkUtil.receive(socket);
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                ByteBuffer buf = ByteBuffer.wrap(data);

                int reqID = Marshaller.unpackInt(buf);
                int opCode = Marshaller.unpackInt(buf);

                if (opCode == Constants.OP_CALLBACK) {
                    long serverVersion = Marshaller.unpackLong(buf);
                    double balance = Marshaller.unpackDouble(buf);

                    if (serverVersion > localVersion) {
                        System.out.println("Update Received: Balance " + balance);
                        localVersion = serverVersion;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
