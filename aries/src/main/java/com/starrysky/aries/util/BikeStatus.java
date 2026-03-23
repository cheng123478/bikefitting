package com.starrysky.aries.util;

public enum BikeStatus {
    ACTIVE(1, "现役"),
    RETIRED(2, "退役");

    public final int code;
    public final String title;

    BikeStatus(int code, String title) {
        this.code = code;
        this.title = title;
    }
}
