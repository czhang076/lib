package server;

import common.Constants;
import common.Marshaller;
import common.NetworkUtil;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

public class SubscriberManager {
    private static class ClientInfo {
        InetAddress address;
        int port;

        ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientInfo that = (ClientInfo) o;
            return port == that.port && address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return 31 * address.hashCode() + port;
        }
    }

    private final CopyOnWriteArrayList<ClientInfo> subscribers = new CopyOnWriteArrayList<>();

    public void addSubscriber(InetAddress address, int port) {
        ClientInfo client = new ClientInfo(address, port);
        if (!subscribers.contains(client)) {
            subscribers.add(client);
            System.out.println("[SubscriberManager] Added subscriber: " + address + ":" + port);
        } else {
             System.out.println("[SubscriberManager] Subscriber already exists: " + address + ":" + port);
        }
    }

    public void notifySubscribers(DatagramSocket socket, Account account) {
        if (subscribers.isEmpty()) return;

        System.out.println("[SubscriberManager] Notifying " + subscribers.size() + " subscribers about Account update (Ver " + account.getVersion() + ")");

        ByteBuffer resBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        Marshaller.packInt(resBuf, 0); // ReqID (ignored by Monitor)
        Marshaller.packInt(resBuf, Constants.OP_CALLBACK); // OpCode
        Marshaller.packLong(resBuf, account.getVersion());
        Marshaller.packDouble(resBuf, account.getBalance());

        byte[] data = new byte[resBuf.position()];
        System.arraycopy(resBuf.array(), 0, data, 0, data.length);

        for (ClientInfo client : subscribers) {
            try {
                NetworkUtil.send(socket, client.address, client.port, data);
            } catch (Exception e) {
                System.out.println("[SubscriberManager] Failed to notify " + client.address + ":" + client.port);
                subscribers.remove(client); // Optional: Remove failed subscriber
            }
        }
    }
}
