package common;

public class Constants {
    public static final int SERVER_PORT = 8080; 
    public static final int BUFFER_SIZE = 1200; 
    public static final int NETWORK_BUFFER_SIZE = BUFFER_SIZE + 200; 

    public static final int OP_OPEN_ACCOUNT = 1;
    public static final int OP_CLOSE_ACCOUNT = 2;
    public static final int OP_CHECK_BALANCE = 3;
    public static final int OP_DEPOSIT = 4;
    public static final int OP_WITHDRAW = 5;
    public static final int OP_TRANSFER = 6;
    public static final int OP_EXCHANGE = 7;
    public static final int OP_MONITOR = 8;

    public static final int STATUS_OK = 1;      // success = 1
    public static final int STATUS_FAIL = 2;    // fail = 2
    public static final int STATUS_ERROR = 3;   // error = 3
    public static final int STATUS_CALLBACK = 4; // callback = 4

    // === Client retry policy ===
    // Total time to keep retrying before giving up (ms)
    public static final long RETRY_TIMEOUT_MS = 30000;
}