package com.google.android.gcm.demo.server;

import com.google.android.gcm.server.Message;

import java.util.Calendar;
import java.util.Date;

public final class PolledMessage implements Comparable<PolledMessage> {

    public static final int FOUR_WKS_IN_S = 2419200;
    public final long timeOfDeath;
    public final Message message;

    public PolledMessage(Message message) {
	if (!message.isDelayWhileIdle() )
	    throw new IllegalArgumentException();
	Integer lifetime = message.getTimeToLive();
	if (lifetime == null)
	    lifetime = FOUR_WKS_IN_S;
	timeOfDeath = System.currentTimeMillis() + lifetime * 1000l;
	this.message = message;
    }

    public boolean isAlive() {
	return System.currentTimeMillis() <= timeOfDeath;
    }

    @Override
    public String toString() {
	return message.toString() + " sent at " + Calendar.getInstance().getTime().toString();
    }

    // inconsistent with equals!
    public int compareTo(PolledMessage rhs) {
	return (int)(timeOfDeath - rhs.timeOfDeath);
    }

}