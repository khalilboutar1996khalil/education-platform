package com.example.education_platform.user.entity;

public enum UserStatus {
    ACTIVE,
    PAUSED,
    DISABLED,
    /**
     * Created by activating a book coupon and waiting for the admin to accept it: the account
     * exists, but cannot sign in until then.
     */
    PENDING
}
