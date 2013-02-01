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

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that adds a new message to all registered devices.
 * <p>
 * This servlet is used just by the browser (i.e., not device).
 */
@SuppressWarnings("serial")
public class SendAllMessagesServlet extends BaseServlet {

  private static final int MULTICAST_SIZE = 1000;
    /* I added these "pointers" */
  private final String[] stringPointer = new String[1];
  private final Message[] messagePointer = new Message[1];

    /* Changed from Sender to my own subclass PushPollSender */
  private PushPollSender sender;

  private static final Executor threadPool = Executors.newFixedThreadPool(5);
  private static final String REGISTRATION_ID = "regId";

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    sender = newSender(config);
  }

  /**
   * Creates the {@link PushPollSender} based on the servlet settings.
   */
    /* Changed constructor and return type from Sender to PushPollSender */
  protected PushPollSender newSender(ServletConfig config) {
    String key = (String) config.getServletContext()
        .getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
    return new PushPollSender(key);
  }

  /**
   * Processes the request to add a new message.
   */
  /* I rewrote much of doPost */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
      Set<String> pushDevices = new HashSet<String>();
      Set<String> pollDevices = new HashSet<String>();
      Datastore.getPushAndPollDevices(pushDevices, pollDevices);
      String status = "";
      if (pushDevices.isEmpty() && pollDevices.isEmpty() )
	  status = "Message ignored as there is no device registered!";
      else {
	  String registrationId = getParameter(req, REGISTRATION_ID, "");

	  // NOTE: check below is for demonstration purposes; a real application
	  // could always send a multicast, even for just one recipient
	  Message message = new Message.Builder().delayWhileIdle(true).build();
	  if (pushDevices.size() == 1) {
	      // send a single message using plain post
	      registrationId = pushDevices.toArray(stringPointer)[0];
	      Result result = sender.send(message, registrationId, 5);
	      status = "Sent message to one device: " + result;

	      int numLeft = Datastore.numMessagesFor(registrationId);
	      if (numLeft > 0) {
		  // send the queue
		  sender.sendQueueFromPush(registrationId);
		  status += "\nSent " + numLeft + " queued messages to the same device: " + result;
	      }
	  } else {
	      // send a multicast message using JSON
	      // must split in chunks of 1000 devices (GCM limit)
	      int total = pushDevices.size();
	      Set<String> partialDevices = new HashSet<String>(total);
	      int counter = 0;
	      int tasks = 0;
	      for (String device : pushDevices) {
		  counter++;
		  partialDevices.add(device);
		  int partialSize = partialDevices.size();
		  if (partialSize == MULTICAST_SIZE || counter == total) {
		      asyncSend(message, partialDevices);
		      partialDevices.clear();
		      tasks++;
		  }
	      }
	      status = "Asynchronously sending " + tasks + " multicast messages to " +
		  total + " devices";
	      Set<Message> messages = new HashSet<Message>();
	      Set<String> devices = new HashSet<String>();
	      boolean shouldLoop = true;
	      int overallTotal = 0;
	      while (shouldLoop) {
		  switch (Datastore.removePushableMessages(messages, devices, pushDevices) ) {
		  case ONE_MESSAGE_MULTIPLE_DEVICES:
		      total = devices.size();
		      overallTotal += total;
		      partialDevices.clear();
		      counter = 0;
		      for (String device : devices) {
			  counter++;
			  partialDevices.add(device);
			  int partialSize = partialDevices.size();
			  if (partialSize == MULTICAST_SIZE || counter == total) {
			      asyncSend(messages.toArray(messagePointer)[0], partialDevices);
			      partialDevices.clear();
			  }
		      }
		      break;

		  case MULTIPLE_MESSAGES_ONE_DEVICE:	
		      overallTotal++;
		      sender.sendQueueFromPush(devices.toArray(stringPointer)[0]);
		      break;
			  
		  default:
		      shouldLoop = false;
		      break;
		  }
		  messages.clear();
		  devices.clear();
	      }
	      status += "\nDequeued various messages to " + overallTotal + " devices";
	  }
	  Datastore.addPolledMessage(pollDevices, message);
	  int numPollDevices = pollDevices.size();
	  if (numPollDevices > 0)
	      status += "\nQueued a message to be polled by " + numPollDevices + " devices, the first of which is " + pollDevices.toArray()[0].toString();
	  req.setAttribute(HomeServlet.ATTRIBUTE_STATUS, status.toString());
	  getServletContext().getRequestDispatcher("/home").forward(req, resp);

      }
  }

  private void asyncSend(final Message message, Set<String> partialDevices) {
    // make a copy
    final List<String> devices = new ArrayList<String>(partialDevices);
    threadPool.execute(new Runnable() {

      public void run() {
        MulticastResult multicastResult;
        try {
          multicastResult = sender.send(message, devices, 5);
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Error posting messages", e);
          return;
        }
        List<Result> results = multicastResult.getResults();
        // analyze the results
        for (int i = 0; i < devices.size(); i++) {
          String regId = devices.get(i);
          Result result = results.get(i);
          String messageId = result.getMessageId();
          if (messageId != null) {
            logger.fine("Succesfully sent message to device: " + regId +
                "; messageId = " + messageId);
            String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
              // same device has more than on registration id: update it
              logger.info("canonicalRegId " + canonicalRegId);
              Datastore.updateRegistration(regId, canonicalRegId);
            }
          } else {
            String error = result.getErrorCodeName();
            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
              // application has been removed from device - unregister it
              logger.info("Unregistered device: " + regId);
              Datastore.unregister(regId);
            } else {
              logger.severe("Error sending message to " + regId + ": " + error);
            }
          }
        }
      }});
  }

}
