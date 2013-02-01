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

import com.google.android.gcm.GCMConstants;
import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.util.Log;


/**
 * Helper class providing methods and constants common to other classes in the
 * app.
 */
public final class CommonUtilities {

    /**
     * Base URL of the Demo Server (such as http://my_host:8080/gcm-demo)
     */
    static final String SERVER_URL = "http://149.125.165.201:8080/gcm-demo";

    /**
     * Google API project id registered to use GCM.
     */
    static final String SENDER_ID = "270536904292";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCMDemo";

    /**
     * Intent used to display a message in the screen.
     */
    static final String DISPLAY_MESSAGE_ACTION =
            "com.google.android.gcm.demo.app.DISPLAY_MESSAGE";

    /**
     * Intent's extra that contains the message to be displayed.
     */
    static final String EXTRA_MESSAGE = "message";

    /* BEGIN MINE */
    static final String POLLED_MESSAGES = "polled_messages";

    static final String PREFERENCES = "com.google.android.gcm.demo";
    static final String REGISTER_TYPE = "register_type";
    static final String UNREGISTER_TYPE = "unregister_type";
    static final String CONNECTION_TYPE = "connection_type";
    static final String OLD_ID = "old_id";
    
    static final boolean PUSH = false;
    static final boolean POLL = !PUSH;

    static final boolean NEW_REGISTER = false;
    static final boolean PUSH_REGISTER = !NEW_REGISTER;

    static final long TOTAL_UNREGISTER = -1;
    static final long MANUAL_POLL = 0;
    /* END MINE */

    /**
     * Notifies UI to display a message.
     * <p>
     * This method is defined in the common helper because it's used both by
     * the UI and the background service.
     *
     * @param context application's context.
     * @param message message to be displayed.
     */
    static void displayMessage(Context context, String message) {
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }

    /* The rest of these functions are my own */

    static SharedPreferences getSharedPreferences(Context context) {
	return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static void register(Context context, String senderId) {
	getSharedPreferences(context).edit().putBoolean(REGISTER_TYPE, NEW_REGISTER).commit();
	GCMRegistrar.register(context, senderId);
    }

    static void toPush(Context context, String senderId) {
	getSharedPreferences(context).edit().putBoolean(REGISTER_TYPE, PUSH_REGISTER).commit();
	if (getConnectionType(context) == PUSH)
	    return;
	GCMRegistrar.register(context, senderId);
    }

    static void toPoll(Context context, long millis) {
	getSharedPreferences(context).edit().putLong(UNREGISTER_TYPE, millis).commit();
	if (getConnectionType(context) == POLL) {
	    // directly send the callback intent to get to onUnregistered
	    Intent intent = new Intent(GCMConstants.INTENT_FROM_GCM_REGISTRATION_CALLBACK);
	    intent.putExtra(GCMConstants.EXTRA_UNREGISTERED, "");
	    context.startService(intent);
	} else
	    GCMRegistrar.unregister(context);
    }

    static void poll(Context context, String regId) {
	String response = ServerUtilities.poll(context, regId);
	if (response == null || response.length() == 0 ) return;
	Log.d("CommonUtilities", "\n\n--Response--\n\n" + response + "\n\n");
	GCMIntentService.generateNotifications(context, response);
    }

    static void unregister(Context context) {
	getSharedPreferences(context).edit().putLong(UNREGISTER_TYPE, TOTAL_UNREGISTER).commit();
	GCMRegistrar.unregister(context);
    }

    static boolean getRegisterType(Context context) {
	return getSharedPreferences(context).getBoolean(REGISTER_TYPE, NEW_REGISTER);
    }

    static long getPollInterval(Context context) {
	return getSharedPreferences(context).getLong(UNREGISTER_TYPE, TOTAL_UNREGISTER);
    }

    static boolean getConnectionType(Context context) {
	return getSharedPreferences(context).getBoolean(CONNECTION_TYPE, PUSH);
    }

    static boolean setConnectionType(Context context, boolean conType) {
	return getSharedPreferences(context).edit().putBoolean(CONNECTION_TYPE, conType).commit();
    }

    static String getOldId(Context context) {
	return getSharedPreferences(context).getString(OLD_ID, "");
    }

    static boolean setOldId(Context context, String oldId) {
	return getSharedPreferences(context).edit().putString(OLD_ID, oldId).commit();
    }

}
