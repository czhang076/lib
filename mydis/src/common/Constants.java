package common;

public class Constants {
    public static final int SERVER_PORT = 8888;
    public static final int BUFFER_SIZE = 1024; // 1KB

    // === OpCode ===
    public static final int OP_OPEN_ACCOUNT = 1;
    public static final int OP_CLOSE_ACCOUNT = 2;
    public static final int OP_DEPOSIT = 3;
    public static final int OP_MONITOR = 4;
    public static final int OP_CALLBACK = 5;
    

    public static final int OP_CHECK_BALANCE = 7;
    public static final int OP_TRANSFER = 8;

    public static final int STATUS_OK = 200;
    public static final int STATUS_ERROR = 500;
}