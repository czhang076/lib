package client;

import common.Constants;
import common.Marshaller;
import common.NetworkUtil;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class BankClientManager {
    private static BankClientManager instance;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int reqIdCounter = new Random().nextInt(1000);
    private UserSession currentUser;
    private boolean enableRetry = false;
    private DatagramSocket monitorSocket;
    private Thread monitorThread;
    private ServerMessageListener messageListener;

    private BankClientManager(String host, int port) throws Exception {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
    }

    public static synchronized BankClientManager getInstance(String host, int port) throws Exception {
        if (instance == null) {
            instance = new BankClientManager(host, port);
        }
        return instance;
    }

    public static synchronized BankClientManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BankClientManager not initialized. Call getInstance(host, port) first.");
        }
        return instance;
    }

    public synchronized void setInvocationSemantics(boolean enableRetry) {
        this.enableRetry = enableRetry;
    }

    public synchronized void setServer(String host, int port) throws Exception {
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
    }

    public synchronized UserSession getCurrentUser() {
        return currentUser;
    }

    public synchronized void logout() {
        currentUser = null;
    }

    public synchronized void setServerMessageListener(ServerMessageListener listener) {
        this.messageListener = listener;
    }

    public synchronized void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (monitorSocket != null && !monitorSocket.isClosed()) {
            monitorSocket.close();
        }
    }

    public Result openAccount(String name, String password, String currency, float initialBalance) throws Exception {
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packString(payloadBuf, name);
        Marshaller.packString(payloadBuf, password);
        Marshaller.packFloat(payloadBuf, initialBalance);
        Marshaller.packString(payloadBuf, currency);

        int opCode = Constants.OP_OPEN_ACCOUNT;
        Response response = sendRequest(opCode, payloadBuf);
        if (response.status == Constants.STATUS_OK) {
            int accountId = extractAccountId(response.message);
            synchronized (this) {
                currentUser = new UserSession(accountId, name, password);
            }
        }
        return new Result(response.status, response.message, response.payload);
    }

    public Result login(int accountId, String name, String password, String currency) throws Exception {
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, accountId);
        Marshaller.packString(payloadBuf, name);
        Marshaller.packString(payloadBuf, password);
        Marshaller.packString(payloadBuf, currency);

        int opCode = Constants.OP_CHECK_BALANCE;
        Response response = sendRequest(opCode, payloadBuf);
        if (response.status == Constants.STATUS_OK) {
            synchronized (this) {
                currentUser = new UserSession(accountId, name, password);
            }
        }
        return new Result(response.status, response.message, response.payload);
    }

    public Result deposit(int accountId, String currency, float amount) throws Exception {
        UserSession session = requireSession();

        int opCode = Constants.OP_DEPOSIT;
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, accountId);
        Marshaller.packString(payloadBuf, session.getName());
        Marshaller.packString(payloadBuf, session.getPassword());
        Marshaller.packString(payloadBuf, currency);
        Marshaller.packFloat(payloadBuf, amount);

        Response response = sendRequest(opCode, payloadBuf);
        return new Result(response.status, response.message, response.payload);
    }

    public Result transfer(int receiverId, String currency, float amount) throws Exception {
        UserSession session = requireSession();

        int opCode = Constants.OP_TRANSFER;
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, session.getAccountId());
        Marshaller.packString(payloadBuf, session.getName());
        Marshaller.packString(payloadBuf, session.getPassword());
        Marshaller.packString(payloadBuf, currency);
        Marshaller.packFloat(payloadBuf, amount);
        Marshaller.packInt(payloadBuf, receiverId);

        Response response = sendRequest(opCode, payloadBuf);
        return new Result(response.status, response.message, response.payload);
    }

    public Result exchange(String fromCurrency, String toCurrency, float amountToExchange) throws Exception {
        UserSession session = requireSession();

        int opCode = Constants.OP_EXCHANGE;
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, session.getAccountId());
        Marshaller.packString(payloadBuf, session.getName());
        Marshaller.packString(payloadBuf, session.getPassword());
        Marshaller.packString(payloadBuf, fromCurrency);
        Marshaller.packString(payloadBuf, toCurrency);
        Marshaller.packFloat(payloadBuf, amountToExchange);

        Response response = sendRequest(opCode, payloadBuf);
        return new Result(response.status, response.message, response.payload);
    }

    public Result withdraw(int accountId, String currency, float amount) throws Exception {
        UserSession session = requireSession();

        int opCode = Constants.OP_WITHDRAW;

        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, accountId);
        Marshaller.packString(payloadBuf, session.getName());
        Marshaller.packString(payloadBuf, session.getPassword());
        Marshaller.packString(payloadBuf, currency);
        Marshaller.packFloat(payloadBuf, amount);

        Response response = sendRequest(opCode, payloadBuf);
        return new Result(response.status, response.message, response.payload);
    }

    public Result checkBalance(String currency) throws Exception {
        UserSession session = requireSession();

        int opCode = Constants.OP_CHECK_BALANCE;
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, session.getAccountId());
        Marshaller.packString(payloadBuf, session.getName());
        Marshaller.packString(payloadBuf, session.getPassword());
        Marshaller.packString(payloadBuf, currency);

        Response response = sendRequest(opCode, payloadBuf);
        return new Result(response.status, response.message, response.payload);
    }

    public Result closeAccount() throws Exception {
        UserSession session = requireSession();

        int opCode = Constants.OP_CLOSE_ACCOUNT;
        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packInt(payloadBuf, session.getAccountId());
        Marshaller.packString(payloadBuf, session.getName());
        Marshaller.packString(payloadBuf, session.getPassword());

        Response response = sendRequest(opCode, payloadBuf);
        if (response.status == Constants.STATUS_OK) {
            synchronized (this) {
                currentUser = null;
            }
        }
        return new Result(response.status, response.message, response.payload);
    }

    public Result startMonitor(long durationMillis) throws Exception {
        if (durationMillis <= 0) {
            return new Result(Constants.STATUS_ERROR, "Invalid monitor duration", new byte[0]);
        }

        synchronized (this) {
            if (monitorThread != null && monitorThread.isAlive()) {
                return new Result(Constants.STATUS_FAIL, "Monitor already running", new byte[0]);
            }
            if (monitorSocket != null && !monitorSocket.isClosed()) {
                monitorSocket.close();
            }
            monitorSocket = new DatagramSocket();
        }

        ByteBuffer payloadBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        Marshaller.packLong(payloadBuf, durationMillis);

        Response response = sendRequestWithSocket(Constants.OP_MONITOR, payloadBuf, monitorSocket);
        if (response.status == Constants.STATUS_OK) {
            startMonitorListenerThread(durationMillis);
        }
        return new Result(response.status, response.message, response.payload);
    }

    private UserSession requireSession() {
        UserSession session = getCurrentUser();
        if (session == null) {
            throw new IllegalStateException("User not logged in.");
        }
        return session;
    }

    private Response sendRequest(int opCode, ByteBuffer payloadBuf) throws Exception {
        int reqID = reqIdCounter++;

        ByteBuffer reqBuf = ByteBuffer.allocate(8 + Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        reqBuf.putInt(reqID);
        reqBuf.putInt(opCode);
        reqBuf.put(payloadBuf.array());
        byte[] reqData = reqBuf.array();

        DatagramPacket replyPacket = sendWithRetryNonCallback(socket, serverAddress, serverPort, reqData, enableRetry);
        if (replyPacket == null) {
            return new Response(Constants.STATUS_ERROR, "No response", new byte[0]);
        }

        byte[] resData = Arrays.copyOf(replyPacket.getData(), replyPacket.getLength());
        if (resData.length < 8) {
            return new Response(Constants.STATUS_ERROR, "Invalid response length", resData);
        }

        ByteBuffer resBuf = ByteBuffer.wrap(resData).order(ByteOrder.LITTLE_ENDIAN);
        resBuf.getInt();
        int status = resBuf.getInt();
        byte[] payloadBytes = new byte[Math.min(Constants.BUFFER_SIZE, resData.length - 8)];
        resBuf.get(payloadBytes);
        String msg = decodeNullTerminated(payloadBytes);
        return new Response(status, msg, payloadBytes);
    }

    private Response sendRequestWithSocket(int opCode, ByteBuffer payloadBuf, DatagramSocket requestSocket) throws Exception {
        int reqID = reqIdCounter++;

        ByteBuffer reqBuf = ByteBuffer.allocate(8 + Constants.BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        reqBuf.putInt(reqID);
        reqBuf.putInt(opCode);
        reqBuf.put(payloadBuf.array());
        byte[] reqData = reqBuf.array();

        DatagramPacket replyPacket = sendWithRetryNonCallback(requestSocket, serverAddress, serverPort, reqData, enableRetry);
        if (replyPacket == null) {
            return new Response(Constants.STATUS_ERROR, "No response", new byte[0]);
        }

        byte[] resData = Arrays.copyOf(replyPacket.getData(), replyPacket.getLength());
        if (resData.length < 8) {
            return new Response(Constants.STATUS_ERROR, "Invalid response length", resData);
        }

        ByteBuffer resBuf = ByteBuffer.wrap(resData).order(ByteOrder.LITTLE_ENDIAN);
        resBuf.getInt();
        int status = resBuf.getInt();
        byte[] payloadBytes = new byte[Math.min(Constants.BUFFER_SIZE, resData.length - 8)];
        resBuf.get(payloadBytes);
        String msg = decodeNullTerminated(payloadBytes);
        return new Response(status, msg, payloadBytes);
    }

    private void startMonitorListenerThread(long durationMillis) {
        monitorThread = new Thread(() -> {
            long endTime = System.currentTimeMillis() + durationMillis;
            try {
                monitorSocket.setSoTimeout(1000);
                while (System.currentTimeMillis() < endTime && !monitorSocket.isClosed()) {
                    try {
                        DatagramPacket packet = NetworkUtil.receive(monitorSocket);
                        byte[] resData = Arrays.copyOf(packet.getData(), packet.getLength());
                        String msg = decodeCallbackMessage(resData);
                        if (msg == null || msg.isEmpty()) {
                            continue;
                        }
                        notifyListener(msg);
                    } catch (java.net.SocketTimeoutException e) {
                        // continue
                    }
                }
            } catch (Exception e) {
                notifyListener("Monitor error: " + e.getMessage());
            } finally {
                try {
                    monitorSocket.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void notifyListener(String msg) {
        ServerMessageListener listener = this.messageListener;
        if (listener != null && msg != null && !msg.isEmpty()) {
            listener.onMessageReceived(msg);
        }
    }

    private DatagramPacket sendWithRetryNonCallback(DatagramSocket socket, InetAddress serverAddress, int serverPort, byte[] reqData, boolean enableRetry) throws Exception {
        socket.setSoTimeout(1000);
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                NetworkUtil.send(socket, serverAddress, serverPort, reqData);
                while (true) {
                    DatagramPacket packet = NetworkUtil.receive(socket);
                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                    if (data.length >= 8) {
                        ByteBuffer resBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                        resBuf.getInt();
                        int status = resBuf.getInt();
                        if (status == Constants.STATUS_CALLBACK) {
                            byte[] payloadBytes = new byte[Math.min(Constants.BUFFER_SIZE, data.length - 8)];
                            resBuf.get(payloadBytes);
                            String msg = decodeNullTerminated(payloadBytes);
                            notifyListener(msg);
                            continue;
                        }
                        if (isValidStatus(status)) {
                            return packet;
                        }
                    }
                    String msg = decodeNullTerminated(data);
                    if (msg != null && !msg.isEmpty()) {
                        notifyListener(msg);
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                if (!enableRetry) {
                    return null;
                }
                if (System.currentTimeMillis() - startTime >= Constants.RETRY_TIMEOUT_MS) {
                    return null;
                }
            }
        }
    }

    private static String decodeNullTerminated(byte[] payloadBytes) {
        int end = 0;
        while (end < payloadBytes.length && payloadBytes[end] != 0) {
            end++;
        }
        return new String(payloadBytes, 0, end, StandardCharsets.UTF_8).trim();
    }

    private static boolean isValidStatus(int status) {
        return status == Constants.STATUS_OK
                || status == Constants.STATUS_FAIL
                || status == Constants.STATUS_ERROR
                || status == Constants.STATUS_CALLBACK;
    }

    private static String decodeCallbackMessage(byte[] resData) {
        if (resData == null || resData.length == 0) {
            return "";
        }
        if (resData.length >= 8) {
            ByteBuffer resBuf = ByteBuffer.wrap(resData).order(ByteOrder.LITTLE_ENDIAN);
            resBuf.getInt();
            int status = resBuf.getInt();
            if (status == Constants.STATUS_CALLBACK) {
                byte[] payloadBytes = new byte[Math.min(Constants.BUFFER_SIZE, resData.length - 8)];
                resBuf.get(payloadBytes);
                return decodeNullTerminated(payloadBytes);
            }
            if (isValidStatus(status)) {
                return "";
            }
        }
        return decodeNullTerminated(resData);
    }

    private static int resolveOpCode(String fieldName, int fallback) {
        try {
            Field field = Constants.class.getField(fieldName);
            return field.getInt(null);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int extractAccountId(String msg) {
        if (msg == null) {
            return -1;
        }
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("id:\\s*(\\d+)").matcher(msg);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            String digits = msg.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return -1;
            }
            if (digits.length() > 9) {
                return -1;
            }
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static class Result {
        private final int status;
        private final String message;
        private final byte[] payload;

        public Result(int status, String message, byte[] payload) {
            this.status = status;
            this.message = message;
            this.payload = payload;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public byte[] getPayload() {
            return payload;
        }

        public boolean isSuccess() {
            return status == Constants.STATUS_OK;
        }
    }

    public interface ServerMessageListener {
        void onMessageReceived(String msg);
    }

    private static class Response {
        private final int status;
        private final String message;
        private final byte[] payload;

        private Response(int status, String message, byte[] payload) {
            this.status = status;
            this.message = message;
            this.payload = payload;
        }
    }
}
