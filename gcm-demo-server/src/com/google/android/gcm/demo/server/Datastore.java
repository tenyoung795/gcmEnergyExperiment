/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gcm.demo.server;

import static com.google.android.gcm.demo.server.Constants.*;

import com.google.android.gcm.server.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/**
 * Simple implementation of a data store using standard Java collections.
 * <p>
 * This class is thread-safe but not persistent (it will lost the data when the
 * app is restarted) - it is meant just as an example.
 */
public final class Datastore {

    private static final Map<String, Boolean> regIds = new HashMap<String, Boolean>();
    // for polling. empty elements are forbidden
    private static final RegIdToMsgQueueMap messageQueues = new RegIdToMsgQueueMap();
    private static final RegIdToColKeyToMsgMap collapseKeys = new RegIdToColKeyToMsgMap();
    // for pushing polled messages
    private static final MsgToRegIdSetMap regIdSets = new MsgToRegIdSetMap();

  private static final Logger logger =
      Logger.getLogger(Datastore.class.getName());

  private Datastore() {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a device.
   */
  public static void register(String regId) {
    logger.info("Registering " + regId);
    synchronized (regIds) {
	regIds.put(regId, DEFAULT_CONNECTION_TYPE);
    }
  }

  /**
   * Unregisters a device.
   */
  public static void unregister(String regId) {
    logger.info("Unregistering " + regId);
    synchronized (Datastore.class) {
      regIds.remove(regId);
      messageQueues.remove(regId);
      collapseKeys.remove(regId);
      regIdSets.remove(regId);
    }
  }

  /**
   * Updates the registration id of a device.
   */
  public static void updateRegistration(String oldId, String newId) {
    logger.info("Updating " + oldId + " to " + newId);
    synchronized (Datastore.class) {
	Boolean connectionType = regIds.remove(oldId);
	if (connectionType != null)
	    regIds.put(newId, connectionType);

	Queue<PolledMessage> messages = messageQueues.remove(oldId);
	if (messages != null)
	    messageQueues.put(newId, messages);

	LinkedHashMap<String, PolledMessage> collapseKeyMap = collapseKeys.remove(oldId);
	if (collapseKeyMap != null)
	    collapseKeys.put(newId, collapseKeyMap);

	for (PolledMessage pmsg : regIdSets.remove(oldId) )
	    regIdSets.put(pmsg, newId);
    }
  }

  /**
   * Gets all registered devices.
   */
  public static Set<String> getDevices() {
    synchronized (regIds) {
	return new HashSet<String>(regIds.keySet() );
    }
  }

  /**
   * Add all registed push devices to one collection and poll devices to another. Precondition: pushDevices and pollDevices are empty
   */
    public static void getPushAndPollDevices(Set<String> pushDevices, Set<String> pollDevices) {
    synchronized (regIds) {
	for (Map.Entry<String, Boolean> entry : regIds.entrySet() ) {
	    Set<String> which;
	    if (entry.getValue() == PUSH)
		which = pushDevices;
	    else
		which = pollDevices;
	    which.add(entry.getKey() );
	}
    }
  }

    /**
     * Gets all registered push devices.
     */
  public static Set<String> getPushDevices() {
    synchronized (regIds) {
	Set<String> set = new HashSet<String>();
	for (Map.Entry<String, Boolean> entry : regIds.entrySet() )
	    if (entry.getValue() == PUSH)
		set.add(entry.getKey() );
	return set;
    }
  }

    /**
     * Gets all registered poll devices.
     */
  public static Set<String> getPollDevices() {
    synchronized (regIds) {
	Set<String> set = new HashSet<String>();
	for (Map.Entry<String, Boolean> entry : regIds.entrySet() )
	    if (entry.getValue() == POLL)
		set.add(entry.getKey() );
	return set;
    }
  }

  /**
   * Sets a device's connection type to push.
   */
    public static void toPush(String oldId, String newId) {
    logger.info("Setting " + oldId + " to push " + newId);
    synchronized (regIds) {
	updateRegistration(oldId, newId);
	regIds.put(newId, PUSH);
    }
  }

  /**
   * Sets a device's connection type to poll.
   */
  public static void toPoll(String regId) {
    logger.info("Setting " + regId + " to poll");
    synchronized (regIds) {
	regIds.put(regId, POLL);
    }
  }

  /**
   * Adds a message to be polled by these devices.
   * Does not add if push-only or already dead.
   */
  public static boolean addPolledMessage(Set<String> regIds, Message msg) {
      synchronized(Datastore.class) {
	  Boolean b = msg.isDelayWhileIdle();
	  if (b == null || b != true)
	      return false;
	  PolledMessage pmsg = new PolledMessage(msg);
	  if (!pmsg.isAlive() )
	      return false;
	  String collapseKey = pmsg.message.getCollapseKey();
	  boolean anyAdd = false;
	  for (String regId : regIds) {
	      messageQueues.remove(regId, collapseKeys.put(regId, pmsg) );
	      anyAdd = anyAdd
		  || messageQueues.put(regId, pmsg);
	  }
	  return anyAdd || regIdSets.putAll(pmsg, regIds);
      }
  }

  /**
   * Removes the first alive message to be polled by a device.
   * Will remove all previous dead messages.
   */
  public static Message removePolledMessage(String regId) {
      synchronized(Datastore.class) {
	  PolledMessage pmsg = messageQueues.poll(regId);
	  if (pmsg == null)
	      return null;
	  // remove the same message from the other data structures
	  collapseKeys.remove(regId, pmsg);
	  regIdSets.remove(pmsg, regId);
	  return pmsg.message;
      }
  }

  /**
   * Removes either the first set of polled messages to be pushed to a device or the first polled message to be pushed to a set of devices.
   * Will remove all dead messages.
   * Returns ONE_MESSAGE_MULTI_DEVICE if a set of messages to a device, MULTI_MESSAGE_ONE_DEVICE if a message to a set of devices, or NO_MESSAGE.
   * Precondition: both sets are empty, pushDevices was made correctly
   */
    public static int removePushableMessages(Set<Message> messages, Set<String> devices, Set<String> pushDevices) {
	synchronized(Datastore.class) {
	    Set<String> devicesToDequeue = new HashSet<String>();
	    // Clean up both regId-message and message-regId maps
	    for (String device : pushDevices)
		if (messageQueues.peek(device) != null)
		    devicesToDequeue.add(device);
	    if (devicesToDequeue.isEmpty() )
		return NO_MESSAGE;

	    PolledMessage pmsg = regIdSets.peek();
	    int numPushable = 0; // number of now pushable messages
	    for (Set<String> set : regIdSets.values() )
		if (!Collections.disjoint(set, devicesToDequeue) )
		    numPushable++;
		    
	    // Determine whether more push devices with polled messages or more pushable messages
	    if (numPushable < devicesToDequeue.size() ) {
		pmsg = regIdSets.poll(devices);
		messages.add(pmsg.message);

		// sync other data structures
		for (String regId : devices) {
		    messageQueues.remove(regId, pmsg);
		    collapseKeys.remove(regId, pmsg);
		}
		return ONE_MESSAGE_MULTIPLE_DEVICES;
	    }
	    for (Map.Entry<String, Queue<PolledMessage> > entry : messageQueues.entrySet() ) {
		String device = entry.getKey();
		if (regIds.get(device) == PUSH) {
		    devices.add(device);
		    Queue<PolledMessage> queue = entry.getValue();
		    for (PolledMessage pm : queue)
			messages.add(pm.message);

		    // sync with other data structures
		    collapseKeys.remove(device);
		    for (PolledMessage pm : queue)
			regIdSets.remove(pm, device);
		    break;
		}
	    }
	    return MULTIPLE_MESSAGES_ONE_DEVICE;
	}
    }

    public static int numMessagesFor(String regId) {
	synchronized(messageQueues) {
	    Queue<PolledMessage> queue = messageQueues.get(regId);
	    return queue == null? 0 : queue.size();
	}
    }

    public static List getList(String regId) {
	List<Map<String, String> > array = new ArrayList<Map<String, String> >();
	while (true) {
	    Message msg = removePolledMessage(regId);
	    if (msg == null) break;
	    array.add(msg.getData() );
	}
	return array;
    }

    public static Boolean getConnectionType(String regId) {
	return regIds.get(regId);
    }

}
