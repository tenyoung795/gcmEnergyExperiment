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

import static com.google.android.gcm.demo.app.CommonUtilities.*;

import com.google.android.gcm.GCMConstants;
import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.EditText;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity {

    TextView mDisplay;
    AsyncTask<Void, Void, Void> mRegisterTask;
    /* Three tasks added by me */
    AsyncTask<Long, Void, Void> toPollTask;
    AsyncTask<Void, Void, Void> toPushTask;
    AsyncTask<Void, Void, Void> pollTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkNotNull(SERVER_URL, "SERVER_URL");
        checkNotNull(SENDER_ID, "SENDER_ID");
        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(this);
        // Make sure the manifest was properly set - comment out this line
        // while developing the app, then uncomment it when it's ready.
        GCMRegistrar.checkManifest(this);
        setContentView(R.layout.main);
	final Context context = this;
        mDisplay = (TextView) findViewById(R.id.display);
        registerReceiver(mHandleMessageReceiver,
                new IntentFilter(DISPLAY_MESSAGE_ACTION));
        final String regId = GCMRegistrar.getRegistrationId(this);
	setConnectionType(this, PUSH);
        if (regId.equals("")) {
        		// Automatically registers application on startup.
        		register(this, SENDER_ID);
        } else {
            // Device is already registered on GCM, check server.
            if (GCMRegistrar.isRegisteredOnServer(this)) {
                // Skips registration.
                mDisplay.append(getString(R.string.already_registered) + "\n");
            } else {
                mRegisterTask = new AsyncTask<Void, Void, Void>() {
		    // based on latest version
                    @Override
                    protected Void doInBackground(Void... params) {
			ServerUtilities.register(context, regId);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mRegisterTask = null;
                    }

                };
                mRegisterTask.execute(null, null, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	final Context context = this;
        switch(item.getItemId()) {
            /*
             * Typically, an application registers automatically, so options
             * below are disabled. Uncomment them if you want to manually
             * register or unregister the device (you will also need to
             * uncomment the equivalent options on options_menu.xml).
             */
            /*
            case R.id.options_register:
                register(this, SENDER_ID);
                return true;
            case R.id.options_unregister:
                unregister(this);
                return true;
	    */
	    /* BEGIN MINE */
	    case R.id.options_to_push:
		toPushTask = new AsyncTask<Void, Void, Void>() {
		    @Override
		    public Void doInBackground(Void... params) {
			toPush(context, SENDER_ID);
			return null;
		    }

		    @Override
		    public void onPostExecute(Void result) {
		    }
		};
		toPushTask.execute();
		return true;
	    case R.id.options_to_poll:
	    /*
		toPollTask = new AsyncTask<Long, Void, Void>() {
		    @Override
		    public Void doInBackground(Long... params) {
			toPoll(context, params[0]);
			return null;
		    }

		    @Override
		    public void onPostExecute(Void result) {
		    }
		};
		toPollTask.execute(10000l);
		*/
	    AlertDialog.Builder alert = new AlertDialog.Builder(this);
	    alert.setTitle("Set a poll interval");
	    alert.setMessage("Enter poll interval in ms (0 for manual):");
	    final EditText input = new EditText(this);
	    input.setInputType(InputType.TYPE_CLASS_NUMBER);
	    alert.setView(input);
	    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				toPollTask = new AsyncTask<Long, Void, Void>() {
				    @Override
				    public Void doInBackground(Long... params) {
					toPoll(context, params[0]);
					return null;
				    }

				    @Override
				    public void onPostExecute(Void result) {
				    }
				};
				toPollTask.execute(Long.parseLong(input.getText().toString() ));
			}
		});
	    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
			}
		});
	    alert.show();
		return true;
	    case R.id.options_poll:
		pollTask = new AsyncTask<Void, Void, Void>() {
		    @Override
		    public Void doInBackground(Void... params) {
			poll(context, getOldId(context) );
			return null;
		    }

		    @Override
		    public void onPostExecute(Void result) {
		    }
		};
		pollTask.execute();
		return true;
		/* END MINE */
            case R.id.options_clear:
                mDisplay.setText(null);
                return true;
            case R.id.options_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
	/* BEGIN MINE */
	if (toPollTask != null)
	    toPollTask.cancel(true);
	if (toPushTask != null)
	    toPushTask.cancel(true);
	if (pollTask != null)
	    pollTask.cancel(true);
	/* END MINE */
        unregisterReceiver(mHandleMessageReceiver);
        super.onDestroy();
    }

    private void checkNotNull(Object reference, String name) {
        if (reference == null) {
            throw new NullPointerException(
                    getString(R.string.error_config, name));
        }
    }

    private final BroadcastReceiver mHandleMessageReceiver =
            new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            mDisplay.append(newMessage + "\n");
        }
    };

}