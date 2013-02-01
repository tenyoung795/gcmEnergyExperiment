package com.google.android.gcm.demo.server;

import com.google.android.gcm.server.Message;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

public class RegIdToColKeyToMsgMap extends HashMap<String, LinkedHashMap<String, PolledMessage> > {

    public RegIdToColKeyToMsgMap() {
	super();
    }

    public PolledMessage put(String regId, PolledMessage message) {
	if (!message.isAlive() || message.message.getCollapseKey() == null)
	    return null;
	String key = message.message.getCollapseKey();
	if (key == null)
	    return null;
	LinkedHashMap<String, PolledMessage> map = get(regId);
	if (map == null) {
	    map = new LinkedHashMap<String, PolledMessage>();
	    put(regId, map);
	}
	return map.put(key, message);
    }

    public PolledMessage remove(String regId, String key) {
	LinkedHashMap<String, PolledMessage> map = get(regId);
	if (map == null)
	    return null;
	PolledMessage result = map.remove(key);
	if (map.isEmpty() )
	    remove(regId);
	return result.isAlive()? result : null;
    }

    public PolledMessage remove(String regId, PolledMessage msg) {
	String key = msg.message.getCollapseKey();
	if (key == null)
	    return null;
	return remove(regId, key);
    }

    public PolledMessage poll(String regId) {
	LinkedHashMap<String, PolledMessage> map = get(regId);
	if (map == null)
	    return null;
	PolledMessage msg;
	Iterator<PolledMessage> iter = map.values().iterator();
	for (msg = iter.next(); msg != null && !msg.isAlive(); iter.remove() )
	    msg = iter.hasNext()? iter.next() : null;
	if (msg == null) {
	    remove(regId);
	    return null;
	}
	iter.remove();
	if (map.isEmpty() )
	    remove(regId);
	return msg;
    }

}