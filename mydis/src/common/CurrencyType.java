package common;

public enum CurrencyType {
    USD,
    RMB,
    SGD,
    JPY,
    BPD;

    public static CurrencyType fromString(String value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        return CurrencyType.valueOf(value.trim().toUpperCase());
    }
}
