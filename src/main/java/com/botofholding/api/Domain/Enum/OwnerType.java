package com.botofholding.api.Domain.Enum;

import java.util.stream.Stream;

public enum OwnerType {
    USER(0, "User"),
    GUILD(1, "Server"),
    SYSTEM(2, "System");

    private final int code;
    private final String displayName;


    OwnerType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int getCode() {
        return code;
    }
    public String getDisplayName() { return displayName; }


    /**
     * A static lookup method to find an OwnerType by its integer code.
     *
     * @param code The integer code to look up (e.g., 0 for USER).
     * @return The matching OwnerType constant.
     * @throws IllegalArgumentException if no OwnerType with the given code exists.
     */
    public static OwnerType fromCode(int code) {
        return Stream.of(OwnerType.values())
                .filter(targetEnum -> targetEnum.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown OwnerType code: " + code));
    }

    @Override
    public String toString() {
        return this.displayName;
    }

}