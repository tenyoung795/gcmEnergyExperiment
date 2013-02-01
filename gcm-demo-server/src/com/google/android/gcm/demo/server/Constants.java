package com.google.android.gcm.demo.server;

public final class Constants {

    private Constants() {}

    public static final boolean PUSH = false;
    public static final boolean POLL = !PUSH;
    public static final boolean DEFAULT_CONNECTION_TYPE = PUSH;

    public static final String PARAM_POLLED_MESSAGES = "polled_messages";

    public static final int ONE_MESSAGE_MULTIPLE_DEVICES = 1;
    public static final int MULTIPLE_MESSAGES_ONE_DEVICE = -1;
    public static final int NO_MESSAGE = 0;

}