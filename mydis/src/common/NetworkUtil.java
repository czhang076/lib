package common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkUtil {

    public static void send(DatagramSocket socket, InetAddress address, int port, byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        System.out.println("[NetUtil] Sent packet to " + address + ":" + port + " (len=" + data.length + ")");
    }

    public static DatagramPacket receive(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[Constants.NETWORK_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        System.out.println("[NetUtil] Received packet from " + packet.getAddress() + ":" + packet.getPort() + " (len=" + packet.getLength() + ")");
        return packet;
    }
}
