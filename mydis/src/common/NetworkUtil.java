package common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class NetworkUtil {

    // 0.0 = No loss, 0.3 = 30% loss
    public static double LOSS_RATE = 0.3; 
    private static Random random = new Random();

    public static void send(DatagramSocket socket, InetAddress address, int port, byte[] data) throws IOException {
        // Simulate packet loss
        if (random.nextDouble() < LOSS_RATE) {
            System.out.println("[NetworkUtil] SIMULATED PACKET LOSS to " + address + ":" + port);
            return;
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        System.out.println("[NetUtil] Sent packet to " + address + ":" + port + " (len=" + data.length + ")");
    }

    public static DatagramPacket receive(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[Constants.BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        System.out.println("[NetUtil] Received packet from " + packet.getAddress() + ":" + packet.getPort() + " (len=" + packet.getLength() + ")");
        return packet;
    }
}
