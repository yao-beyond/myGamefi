package com.example.app.code;

import java.util.Optional;

/** CPS-08 · Bitmask Server-Role：value 為 2 的次方，可位元組合複合角色。 */
public enum ServerRole {

    MAINTAIN(-1) { @Override public String getDescription() { return "Maintenance Server"; } },
    CUSTOMER(1)  { @Override public String getDescription() { return "Customer-facing Server"; } },
    TRANSACTION(4) { @Override public String getDescription() { return "Transaction/Settlement Server"; } },
    CACHE(32)    { @Override public String getDescription() { return "Memory Cache Server"; } };

    public static final ServerRole[] VALUES = values();

    private final int value;

    ServerRole(int value) {
        this.value = value;
    }

    public int unique() {
        return value;
    }

    public abstract String getDescription();

    public static Optional<ServerRole> getInstanceOf(int value) {
        for (ServerRole e : VALUES) {
            if (e.value == value) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public static int mask(ServerRole... roles) {
        int m = 0;
        for (ServerRole r : roles) {
            if (r.value > 0) {
                m |= r.value;
            }
        }
        return m;
    }
}
