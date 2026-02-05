package common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Marshaller {

    // === Core Type Marshalling ===

    public static void packInt(ByteBuffer buf, int value) {
        buf.putInt(value);
    }

    public static int unpackInt(ByteBuffer buf) {
        return buf.getInt();
    }

    public static void packDouble(ByteBuffer buf, double value) {
        buf.putDouble(value);
    }

    public static double unpackDouble(ByteBuffer buf) {
        return buf.getDouble();
    }

    public static void packLong(ByteBuffer buf, long value) {
        buf.putLong(value);
    }

    public static long unpackLong(ByteBuffer buf) {
        return buf.getLong();
    }

    public static void packString(ByteBuffer buf, String text) {
        if (text == null) text = "";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        buf.putLong((long)bytes.length);
        buf.put(bytes);
    }

    public static String unpackString(ByteBuffer buf) {
        long len = buf.getLong();
        if (len <= 0) {
            return "";
        }
        if (len > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("String length too large: " + len);
        }
        byte[] bytes = new byte[(int) len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void packFloat(ByteBuffer buf, float value) {
        buf.putFloat(value);
    }

    public static float unpackFloat(ByteBuffer buf) {
        return buf.getFloat();
    }


    // === Legacy Support for string-only packing (for tests) ===
    // Keeps the old method to avoid breaking existing tests immediately, 
    // or we can update tests.
    public static byte[] packString(String text) {
        if (text == null) {
            text = "";
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + bytes.length);
        buf.putInt(bytes.length);
        buf.put(bytes);
        return buf.array();
    }
}
