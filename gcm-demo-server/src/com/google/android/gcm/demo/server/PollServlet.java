package com.google.android.gcm.demo.server;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.json.simple.*;

public class PollServlet extends BaseServlet {

    @Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws IOException, ServletException {
	String registrationId = getParameter(req, "regId");
	// check if a POST from phone
	if (Datastore.getConnectionType(registrationId) != Constants.POLL)
	    return;
	//	resp.setContentType("text/");
	PrintWriter out = resp.getWriter();
	JSONValue.writeJSONString(Datastore.getList(registrationId), out);
	out.close();
    }
	

}