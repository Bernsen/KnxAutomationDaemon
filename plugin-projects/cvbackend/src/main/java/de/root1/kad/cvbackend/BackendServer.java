/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KAD CometVisu Backend (KCVB).
 *
 *   KCVB is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KCVB is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KCVB.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.kad.cvbackend;

import de.root1.kad.knxservice.KnxService;
import de.root1.kad.knxservice.KnxServiceDataListener;
import de.root1.kad.knxservice.KnxServiceException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class BackendServer extends NanoHttpdSSE {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String documentRoot;

    private static final String REQUEST_LOGIN = "l";
    private static final String REQUEST_READ = "r";
    private static final String REQUEST_WRITE = "w";
    private static final String REQUEST_FILTER = "f";
    private static final String REQUEST_RRDFETCH = "rrdfetch";

    private final Map<String, UserSessionID> sessions = new HashMap<>();
    private Timer t = new Timer("SessionID Remover");
    private TimerTask tt = new TimerTask() {

        @Override
        public void run() {
            synchronized (sessions) {
                Iterator<String> iter = sessions.keySet().iterator();
                while (iter.hasNext()) {
                    String clientIp = iter.next();
                    UserSessionID session = sessions.get(clientIp);
                    if (!session.isValid()) {
                        log.info("Removing session due to timeout: {}", session);
                        iter.remove();
                    }
                }
            }
        }
    };
    private final KnxService knx;
    private final int SESSION_TIMEOUT;
    private boolean requireUserSession = false;

    BackendServer(int port, String documentRoot, KnxService knx, int sessionTimeout, boolean requireUserSession) {
        super(port);
        this.documentRoot = documentRoot;
        this.knx = knx;
        t.schedule(tt, 5000, 30 * 60 * 1000);
        SESSION_TIMEOUT = sessionTimeout;
        this.requireUserSession = requireUserSession;
    }

    private String extractClientIp(IHTTPSession session) {
        return session.getHeaders().get("http-client-ip");
    }

    private String extractUserSessionIdString(IHTTPSession session) {
        return session.getParms().get("s");
    }

    private Map<String, List<String>> getParams(IHTTPSession session) {
        return decodeParameters(session.getQueryParameterString());
    }

    @Override
    public IResponse serve(IHTTPSession session) {

        log.info("uri: {}", session.getUri());
//        log.info("queryParameterString: {}", session.getQueryParameterString());
        log.info("params: {}", getParams(session));
//        log.info("headers: {}", session.getHeaders());

        String uri = session.getUri();

        if (!uri.startsWith(documentRoot)) {
            Response response = new Response("<html><body>URI '" + uri + "' not handled by this server</body></html>");
            response.setStatus(Status.BAD_REQUEST);
            return response;
        }

        String resource = uri.substring(documentRoot.length());

        log.info("resource: {}", resource);

        switch (resource) {
            case REQUEST_LOGIN:
                return handleLogin(session);
            case REQUEST_FILTER:
                return handleFilter(session);
            case REQUEST_READ:
                return handleRead(session);
            case REQUEST_WRITE:
                return handleWrite(session);
            case REQUEST_RRDFETCH:
                return handleRrdfetch(session);
            default:
                Response response = new Response("<html><body>resource '" + resource + "' not handled by this server</body></html>");
                response.setStatus(Status.BAD_REQUEST);
                return response;

        }

    }

    /**
     * Validates user session information found in header. If found&valid,
     * session is renewed.
     *
     * @param session
     * @return not null, if session information in request is valid + session is
     * valid too, false null
     */
    private UserSessionID validateUserSessionInRequest(IHTTPSession session) {

        String clientIp = extractClientIp(session);
        String sessionIdString = extractUserSessionIdString(session);

        synchronized (sessions) {
            UserSessionID sessionId = sessions.get(clientIp);

            if (sessionId != null && sessionId.getId().toString().equals(sessionIdString) && sessionId.isValid()) {
                sessionId.renew();
                return sessionId;
            }
        }
        return null;
    }

    private UserSessionID createUserSessionID(IHTTPSession session) {
        String clientIp = session.getHeaders().get("http-client-ip");
        UserSessionID userSessionId = new UserSessionID(clientIp, SESSION_TIMEOUT);
        sessions.put(clientIp, userSessionId);
        return userSessionId;
    }

    private Response handleLogin(IHTTPSession session) {

        Map<String, String> parms = session.getParms();
        String user = parms.get("u");
        String pass = parms.get("p");
        String device = parms.get("d");
        log.info("login: user={}, pass={}, device={}", user, pass, device);

        JSONObject obj = new JSONObject();
        obj.put("v", "0.0.1");

        String userSessionIdString = requireUserSession ? createUserSessionID(session).getId().toString() : "0";

        obj.put("s", userSessionIdString);
        log.info("response: {}", obj.toJSONString());

        return new Response(obj.toJSONString());
    }

    private Response handleWrite(IHTTPSession session) {

        long start = System.currentTimeMillis();
        // s=SESSION&a=ADDRESS1&a=...&v=VALUE
        Map<String, List<String>> params = getParams(session);
        log.info("write params: {}", params);

        // FIXME CometVisu does not send the session at all?! So the following is disabled for now
//        if (requireUserSession) {
//        UserSessionID userSessionID = validateUserSessionInRequest(session);
//        if (userSessionID==null) return new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "");
//        }
        List<String> addresses = params.get("a");
        String value = session.getParms().get("v");

        for (String address : addresses) {
            try {
                knx.write(address, value);
            } catch (KnxServiceException ex) {
                ex.printStackTrace();
            }
        }
        log.info("done with write for {}: {}ms", params, System.currentTimeMillis() - start);
        return new Response(Status.OK, MIME_PLAINTEXT, "");

    }

    private IResponse handleRead(final IHTTPSession session) {
        // a=3/1/10, s=223f7232-ee73-41c1-8ea7-f7ef1d2adea7, t=0
        Map<String, List<String>> params = getParams(session);
        log.info("read params: {}", params);

        UserSessionID userSessionID = null;
        userSessionID = validateUserSessionInRequest(session);
        if (userSessionID==null) {
            return new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "");
        }

        final UserSessionID finalUserSessionId = userSessionID;

        final List<String> addresses = params.get("a");
        
        // heartbeat can not be read
        addresses.remove("KAD.CV.heartbeat");

        JSONObject jsonResponse = new JSONObject();
        JSONObject jsonData = new JSONObject();

        final SseResponse sse = new SseResponse(session);

        log.info("Reading addresses: {}", addresses);

        // client knows nothing. Full response required
        for (String address : addresses) {

            try {
                String value = knx.read(address);
                if (!value.isEmpty()) {
                    jsonData.put(address, value);
                } else {
                    log.error("Address '" + address + "' not readable");
                }
            } catch (KnxServiceException ex) {
                log.warn("Skipping '"+address+"' due to read problem.", ex);
            }

        }
        jsonResponse.put("d", jsonData);
        jsonResponse.put("i", "1");

        log.info("response: {}", jsonResponse.toJSONString());
        sse.sendMessage(null, null, jsonResponse.toJSONString());

        KnxServiceDataListener listener = new KnxServiceDataListener() {

            @Override
            public void onData(String ga, String value) {
                JSONObject jsonResponse = new JSONObject();
                JSONObject jsonData = new JSONObject();
                jsonData.put(ga, value);
                jsonResponse.put("d", jsonData);
                jsonResponse.put("i", "1");
                log.info("response: {}", jsonResponse.toJSONString());
                boolean trouble = sse.sendMessage(null, null, jsonResponse.toJSONString());
                if (!trouble) {
                    finalUserSessionId.renew();
                }
            }
        };

        try {

            for (String addr : addresses) {
                knx.registerListener(addr, listener);
            }

            log.info("Waiting for session closed for {}", userSessionID.getId());
            try {
                long lastCheck = System.currentTimeMillis();
                boolean heartbeatState = true;
                while (!sse.waitForTrouble(1000)) {
                    if (System.currentTimeMillis() - lastCheck > 1000) {
                        JSONObject r = new JSONObject();
                        JSONObject d = new JSONObject();
                        d.put("KAD.CV.heartbeat", heartbeatState?"1":"0");
                        r.put("d", d);
                        r.put("i", "1");
                        boolean trouble = sse.sendMessage(null, null, r.toJSONString());
                        log.trace("Sent keepalive for " + finalUserSessionId);
                        if (!trouble) {
                            finalUserSessionId.renew();
                        }
                        heartbeatState=!heartbeatState; // toggle
                        lastCheck = System.currentTimeMillis();
                    }
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            for (String addr : addresses) {
                knx.unregisterListener(addr, listener);
            }
        } catch (KnxServiceException ex) {
            ex.printStackTrace();
        }
        log.info("Session closed for {}", userSessionID.getId());
        return sse;
    }

    private Response handleFilter(IHTTPSession session) {
        return new Response("<html><body>FILTER: it works</body></html>");
    }

    private IResponse handleRrdfetch(IHTTPSession session) {
        Map<String, List<String>> params = getParams(session);
        log.info("rrdfetch params: {}", params);

//        UserSessionID userSessionID = null;
//        userSessionID = validateUserSessionInRequest(session);
//        if (userSessionID==null) {
//            return new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "");
//        }


        String rrd = session.getParms().get("rrd");
        String ds = session.getParms().get("ds");
        String start = session.getParms().get("start");
        String end = session.getParms().get("end");
        String res = session.getParms().get("res");
        
        
        return new Response("["
            + "[1445869830000,[\"2.0700000000E01\"]],"
            + "[1445873874000,[\"2.0600000000E01\"]],"
            + "[1445881643000,[\"2.0480000000E01\"]],"
            + "[1445901039000,[\"2.0600000000E01\"]],"
            + "[1445901042000,[\"2.0500000000E01\"]],"
            + "[1445910320000,[\"2.0400000000E01\"]],"
            + "[1445919568000,[\"2.0300000000E01\"]],"
            + "[1445931781000,[\"2.0420000000E01\"]],"
            + "[1445941319000,[\"2.0520000000E01\"]],"
            + "[1445959136000,[\"2.0520000000E01\"]],"
            + "[1445959577000,[\"2.0520000000E01\"]],"
            + "[1445960204000,[\"2.0500000000E01\"]],"
            + "[1445965508000,[\"2.0420000000E01\"]],"
            + "[1445973246000,[\"2.0320000000E01\"]],"
            + "[1445976175000,[\"2.0420000000E01\"]],"
            + "[1445976479000,[\"2.0520000000E01\"]],"
            + "[1445978510000,[\"2.0620000000E01\"]],"
            + "[1445978935000,[\"2.0740000000E01\"]],"
            + "[1445984397000,[\"2.0640000000E01\"]],"
            + "[1445986888000,[\"2.0540000000E01\"]],"
            + "[1445990845000,[\"2.0640000000E01\"]],"
            + "[1445990847000,[\"2.0540000000E01\"]],"
            + "[1445992558000,[\"2.0450000000E01\"]],"
            + "[1445998861000,[\"2.0540000000E01\"]],"
            + "[1446020329000,[\"2.0450000000E01\"]],"
            + "[1446025375000,[\"2.0350000000E01\"]],"
            + "[1446039860000,[\"2.0450000000E01\"]],"
            + "[1446048227000,[\"2.0350000000E01\"]],"
            + "[1446053687000,[\"2.0250000000E01\"]],"
            + "[1446061039000,[\"2.0360000000E01\"]],"
            + "[1446063806000,[\"2.0460000000E01\"]],"
            + "[1446068456000,[\"2.0360000000E01\"]],"
            + "[1446071016000,[\"2.0260000000E01\"]],"
            + "[1446083019000,[\"2.0160000000E01\"]],"
            + "[1446087977000,[\"2.0270000000E01\"]],"
            + "[1446087980000,[\"2.0160000000E01\"]],"
            + "[1446096819000,[\"2.0060000000E01\"]],"
            + "[1446104753000,[\"2.0160000000E01\"]],"
            + "[1446109980000,[\"2.0280000000E01\"]]"
            + "]");
    }

}
