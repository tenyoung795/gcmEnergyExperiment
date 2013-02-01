package com.google.android.gcm.demo.server;

import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.PriorityQueue;

public class RegIdToMsgQueueMap extends LinkedHashMap<String, Queue<PolledMessage> > {

    public RegIdToMsgQueueMap() {
	super();
    }

    public boolean put(String regId, PolledMessage message) {
	if (!message.isAlive() )
	    return false;
	Queue<PolledMessage> queue = get(regId);
	if (queue == null) {
	    queue = new PriorityQueue<PolledMessage>();
	    put(regId, queue);
	}
	return queue.add(message);
    }

    public boolean remove(String regId, PolledMessage message) {
	Queue<PolledMessage> queue = get(regId);
	if (queue == null)
	    return false;
	return queue.remove(message);
    }

    public PolledMessage peek(String regId) {
	Queue<PolledMessage> queue = get(regId);
	if (queue == null)
	    return null;
	PolledMessage msg;
	for (msg = queue.peek(); msg != null && !msg.isAlive(); msg = queue.poll() );
	if (msg == null) {
	    remove(regId);
	    return null;
	}
	return msg;
    }

    public PolledMessage poll(String regId) {
	PolledMessage msg = peek(regId);
	if (msg == null)
	    return null;
	Queue<PolledMessage> queue = get(regId);
	queue.poll(); // remove this alive message
	if (queue.isEmpty() )
	    remove(regId);
	return msg;
    }

}