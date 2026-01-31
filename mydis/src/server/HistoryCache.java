package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

public class HistoryCache {
    private static final ConcurrentHashMap<String, byte[]> history = new ConcurrentHashMap<>();

    // Create a unique key for the request
    private static String makeKey(String clientIp, int clientPort, int reqId) {
        return clientIp + ":" + clientPort + ":" + reqId;
    }

    public static boolean hasResponse(String clientIp, int clientPort, int reqId) {
        return history.containsKey(makeKey(clientIp, clientPort, reqId));
    }

    public static byte[] getResponse(String clientIp, int clientPort, int reqId) {
        return history.get(makeKey(clientIp, clientPort, reqId));
    }

    public static void putResponse(String clientIp, int clientPort, int reqId, byte[] responseData) {
        history.put(makeKey(clientIp, clientPort, reqId), responseData);
    }
}
