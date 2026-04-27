package com._bit.Unison.common;

public class AccountRestrictedException extends RuntimeException {

    public static final String CODE_ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String CODE_ACCOUNT_BANNED = "ACCOUNT_BANNED";

    private final String code;

    public AccountRestrictedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
