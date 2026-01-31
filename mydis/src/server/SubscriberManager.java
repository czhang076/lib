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
        long expiresAt;

        ClientInfo(InetAddress address, int port, long expiresAt) {
            this.address = address;
            this.port = port;
            this.expiresAt = expiresAt;
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

    public void addSubscriber(InetAddress address, int port, int intervalSeconds) {
        cleanupExpired();
        long expiresAt = System.currentTimeMillis() + (intervalSeconds * 1000L);
        ClientInfo client = new ClientInfo(address, port, expiresAt);
        if (!subscribers.contains(client)) {
            subscribers.add(client);
            System.out.println("[SubscriberManager] Added subscriber: " + address + ":" + port + " (expires in " + intervalSeconds + "s)");
        } else {
            for (ClientInfo existing : subscribers) {
                if (existing.equals(client)) {
                    existing.expiresAt = expiresAt;
                    break;
                }
            }
            System.out.println("[SubscriberManager] Subscriber refreshed: " + address + ":" + port + " (expires in " + intervalSeconds + "s)");
        }
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (ClientInfo client : subscribers) {
            if (now > client.expiresAt) {
                subscribers.remove(client);
            }
        }
    }

    public void notifySubscribers(DatagramSocket socket, Account account) {
        if (subscribers.isEmpty()) return;

        System.out.println("[SubscriberManager] Notifying " + subscribers.size() + " subscribers about Account update (Ver " + account.getVersion() + ")");

        ByteBuffer resBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        Marshaller.packInt(resBuf, 0); // Length placeholder
        Marshaller.packInt(resBuf, 0); // ReqID for callbacks
        Marshaller.packInt(resBuf, Constants.OP_CALLBACK);
        Marshaller.packLong(resBuf, account.getVersion());
        Marshaller.packInt(resBuf, account.getAccountNumber());
        Marshaller.packString(resBuf, account.getName());
        Marshaller.packString(resBuf, account.getCurrencyType().name());
        Marshaller.packDouble(resBuf, account.getBalance());

        resBuf.putInt(0, resBuf.position());
        byte[] data = new byte[resBuf.position()];
        System.arraycopy(resBuf.array(), 0, data, 0, data.length);

        long now = System.currentTimeMillis();
        for (ClientInfo client : subscribers) {
            if (now > client.expiresAt) {
                subscribers.remove(client);
                continue;
            }
            try {
                NetworkUtil.send(socket, client.address, client.port, data);
            } catch (Exception e) {
                System.out.println("[SubscriberManager] Failed to notify " + client.address + ":" + client.port);
                subscribers.remove(client); // Optional: Remove failed subscriber
            }
        }
    }
}
