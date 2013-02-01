package com.google.android.gcm.demo.server;

import java.util.Collection;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Iterator;

public class MsgToRegIdSetMap extends TreeMap<PolledMessage, Set<String> > {

    public MsgToRegIdSetMap() {
	super();
    }

    public boolean put(PolledMessage message, String regId) {
	if (!message.isAlive() )
	    return false;
	Set<String> set = get(message);
	if (set == null) {
	    set = new HashSet<String>();
	    put(message, set);
	}
	return set.add(regId);
    }

    public boolean remove(PolledMessage message, String regId) {
	Set<String> set = get(message);
	if (set == null)
	    return false;
	return set.remove(regId);
    }

    public boolean putAll(PolledMessage message, Collection<String> regIds){
	if (!message.isAlive() )
	    return false;
	Set<String> set = get(message);
	if (set == null) {
	    set = new HashSet<String>();
	    put(message, set);
	}
	return set.addAll(regIds);
    }

    public Map.Entry<PolledMessage, Set<String> > peekEntry() {
	Map.Entry<PolledMessage, Set<String> > entry;
	for (entry = firstEntry();
	     entry != null && !entry.getKey().isAlive();
	     entry = pollFirstEntry() );
	return entry;
    }

    public PolledMessage peek() {
	return peekEntry().getKey();
    }

    public PolledMessage poll(Set<String> devices) {
	Map.Entry<PolledMessage, Set<String> > entry = peekEntry();
	if (entry == null) return null;
	pollFirstEntry();
	if (isEmpty() )
	    return null;
	devices.addAll(entry.getValue() );
	return entry.getKey();
    }

    public Queue<PolledMessage> remove(String regId) {
	Queue<PolledMessage> result = new PriorityQueue<PolledMessage>();
	peek();
	Iterator<Map.Entry<PolledMessage, Set<String> > > iter = entrySet().iterator();
	while (iter.hasNext() ) {
	    Map.Entry<PolledMessage, Set<String> > entry = iter.next();
	    Set<String> set = entry.getValue();
	    if (set.contains(regId) ) {
		set.remove(regId);
		if (set.isEmpty() )
		    iter.remove();
		result.add(entry.getKey() );
	    }
	}
	return result;
    }

    
}