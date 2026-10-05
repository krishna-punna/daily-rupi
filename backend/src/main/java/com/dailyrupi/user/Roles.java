package com.dailyrupi.user;

public final class Roles {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    /** Granted instead of the real roles until a temporary password has been replaced. */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private Roles() {
    }
}
