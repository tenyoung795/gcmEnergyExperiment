package com.google.android.gcm.demo.server;

import static com.google.android.gcm.demo.server.Constants.PARAM_POLLED_MESSAGES;
import static com.google.android.gcm.server.Constants.JSON_PAYLOAD;

import com.google.android.gcm.server.Sender;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import java.io.IOException;
import java.io.PrintWriter;
import org.json.simple.JSONValue;

/**
 * A {@link Sender} that supports push <i>and</i> poll clients.
 */
public class PushPollSender extends Sender {

    public PushPollSender(String key) {
	super(key);
    }
    
    public Result sendQueueFromPush(final String regId) throws IOException {
	return send(new Message.Builder().delayWhileIdle(true).addData(PARAM_POLLED_MESSAGES, JSONValue.toJSONString(Datastore.getList(regId) ) ).build(),
		    regId, 5);
    }

    

}