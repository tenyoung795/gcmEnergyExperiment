/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gcm.demo.app;

import static com.google.android.gcm.demo.app.CommonUtilities.SENDER_ID;
import static com.google.android.gcm.demo.app.CommonUtilities.displayMessage;
import static com.google.android.gcm.demo.app.CommonUtilities.POLLED_MESSAGES;
import static com.google.android.gcm.demo.app.CommonUtilities.PUSH;
import static com.google.android.gcm.demo.app.CommonUtilities.POLL;
import static com.google.android.gcm.demo.app.CommonUtilities.NEW_REGISTER;
import static com.google.android.gcm.demo.app.CommonUtilities.PUSH_REGISTER;
import static com.google.android.gcm.demo.app.CommonUtilities.TOTAL_UNREGISTER;
import static com.google.android.gcm.demo.app.CommonUtilities.MANUAL_POLL;
import static com.google.android.gcm.demo.app.CommonUtilities.PREFERENCES;
import static com.google.android.gcm.demo.app.CommonUtilities.getRegisterType;
import static com.google.android.gcm.demo.app.CommonUtilities.getPollInterval;
import static com.google.android.gcm.demo.app.CommonUtilities.poll;
import static com.google.android.gcm.demo.app.CommonUtilities.getConnectionType;
import static com.google.android.gcm.demo.app.CommonUtilities.setConnectionType;
import static com.google.android.gcm.demo.app.CommonUtilities.getOldId;
import static com.google.android.gcm.demo.app.CommonUtilities.setOldId;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Handler;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    @SuppressWarnings("hiding")
    private static final String TAG = "GCMIntentService";
    private final Handler handler;

    public GCMIntentService() {
        super(SENDER_ID);
	handler = new Handler();
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        displayMessage(context, getString(R.string.gcm_registered));
	/* BEGIN MINE */
	if (getRegisterType(context) == PUSH_REGISTER) {
	    String response = ServerUtilities.toPush(context, getOldId(context), registrationId);
	    if (response != null){
	    	setConnectionType(context, PUSH);
	    	if (response.length() != 0)
	    		generateNotifications(context, response);
	    }
	}
	else /* END MINE */
	    ServerUtilities.register(context, registrationId);
    }

    /* I rewrote most of onUnregistered() */
    @Override
    protected void onUnregistered(final Context context, final String registrationId) {
    	long intervalMillis = getPollInterval(context);
	Log.i(TAG, "Device unregistered");
	displayMessage(context, getString(R.string.gcm_unregistered));
	
	if (intervalMillis != TOTAL_UNREGISTER) {
	    if (ServerUtilities.toPoll(context, registrationId) ) {
		setConnectionType(context, POLL);
		setOldId(context, registrationId);
		startPolling(context, registrationId, intervalMillis);
	    }
	} else if (GCMRegistrar.isRegisteredOnServer(context)) {
            ServerUtilities.unregister(context, registrationId);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
            Log.i(TAG, "Ignoring unregister callback");
        }
    }
    
    /* My own function */
    private void startPolling(final Context context, final String registrationId, final long intervalMillis) {
    	if (intervalMillis == MANUAL_POLL) return;
    	handler.postDelayed(new Runnable() {
    		
			public void run() {
				if (getConnectionType(context) != POLL)
				    return;
				final Runnable runnable = this;
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... arg0) {
						poll(context, registrationId);
						long interval = getPollInterval(context);
						if (interval != MANUAL_POLL)
						    handler.postDelayed(runnable, interval);
						return null;
					}
					
				}.execute();
			}
			
		}, intervalMillis);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message");
	/* I turned gcm_message into a quantity string */
        String message = context.getResources().getQuantityString(R.plurals.gcm_message, 1);
        displayMessage(context, message);
    	generateNotification(context, message);
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(TAG, "Received deleted messages notification");
        String message = getString(R.string.gcm_deleted, total);
        displayMessage(context, message);
        // notifies user
        generateNotification(context, message);
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.i(TAG, "Received error: " + errorId);
        displayMessage(context, getString(R.string.gcm_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        displayMessage(context, getString(R.string.gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.ic_stat_gcm;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        String title = context.getString(R.string.app_name);
        Intent notificationIntent = new Intent(context, DemoActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
    
    /* My own function */
    public static void generateNotifications(Context context, String JSONstring) {
    	int numMessages = 0;
    if (JSONstring != null && JSONstring.length() != 0) {
	    try {
		Object o = JSONValue.parseWithException(JSONstring);
		numMessages = ((JSONArray)o).size();
		Log.d(TAG, "JSONArray: " + ((JSONArray)o).toString() );
	    } catch (ParseException e) {
	    	Log.d(TAG, "Parser exception " + e.getLocalizedMessage() + " at " + JSONstring.substring(e.getPosition() ) + "before " + JSONstring.substring(0, e.getPosition() ) );
	    	numMessages = 1;
	    }
	}
    if (numMessages == 0) return;
    String message = context.getResources().getQuantityString(R.plurals.gcm_message, numMessages, numMessages);
    displayMessage(context, message);
	generateNotification(context, message);
    }

}
