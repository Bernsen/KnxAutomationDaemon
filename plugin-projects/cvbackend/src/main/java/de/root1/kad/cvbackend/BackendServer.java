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

import de.root1.kad.knxcache.KnxCachePlugin;
import fi.iki.elonen.NanoHTTPD;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class BackendServer extends NanoHTTPD {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String documentRoot;
    private static final String REQUEST_LOGIN = "l";
    private static final String REQUEST_READ = "r";
    private static final String REQUEST_WRITE = "w";
    private static final String REQUEST_FILTER = "f";

    private final Map<String, SessionID> sessions = new HashMap<>();
    private Timer t = new Timer("SessionID Remover");
    private TimerTask tt = new TimerTask() {

        @Override
        public void run() {
            synchronized (sessions) {
                Iterator<String> iter = sessions.keySet().iterator();
                while (iter.hasNext()) {
                    String clientIp = iter.next();
                    SessionID session = sessions.get(clientIp);
                    if (!session.isValid()) {
                        log.info("Removing session due to timeout: {}", session);
                        iter.remove();
                    }
                }
            }
        }
    };
    private final KnxCachePlugin knx;
    private final int SESSION_TIMEOUT;

    class SessionID {

        UUID id;
        String ip;
        long lastAccess;

        public SessionID(String ip) {
            this.id = UUID.randomUUID();
            this.ip = ip;
            this.lastAccess = System.currentTimeMillis();
        }

        public void refresh() {
            
            this.lastAccess = System.currentTimeMillis();
        }

        public UUID getId() {
            return id;
        }

        public String getIp() {
            return ip;
        }

        public boolean isValid() {
            return System.currentTimeMillis() - lastAccess < SESSION_TIMEOUT;
        }

        @Override
        public String toString() {
            return "SessionID{ip=" + ip + ", lastAccess=" + new Date(lastAccess) + ",  id=" + id + +'}';
        }

    }

    BackendServer(int port, String documentRoot, KnxCachePlugin knx, int sessionTimeout) {
        super(port);
        this.documentRoot = documentRoot;
        this.knx = knx;
        t.schedule(tt, 5000, 30 * 60 * 1000);
        SESSION_TIMEOUT = sessionTimeout;
    }

    private Map<String, List<String>> getParams(IHTTPSession session) {
        return decodeParameters(session.getQueryParameterString());
    }

    @Override
    public Response serve(IHTTPSession session) {

        log.info("uri: {}", session.getUri());
        log.info("queryParameterString: {}", session.getQueryParameterString());
        log.info("params: {}", getParams(session));
//        log.info("headers: {}", session.getHeaders());

        String uri = session.getUri();

        if (!uri.startsWith(documentRoot)) {
            Response response = new Response("<html><body>URI '" + uri + "' not handled by this server</body></html>");
            response.setStatus(Response.Status.BAD_REQUEST);
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
            default:
                Response response = new Response("<html><body>resource '" + resource + "' not handled by this server</body></html>");
                response.setStatus(Response.Status.BAD_REQUEST);
                return response;

        }

    }

    public boolean validateSession(IHTTPSession session, String sessionIdString) {
        String clientIp = session.getHeaders().get("http-client-ip");
        synchronized (sessions) {
            SessionID sessionId = sessions.get(clientIp);

            if (sessionId != null && sessionId.getId().toString().equals(sessionIdString) && sessionId.isValid()) {
                sessionId.refresh();
                return true;
            }
        }
        return false;
    }

    public String getSessionID(IHTTPSession session) {

        String clientIp = session.getHeaders().get("http-client-ip");
        SessionID sessionId;
        synchronized (sessions) {
            sessionId = sessions.get(clientIp);

            if (sessionId != null) {
                sessionId.refresh();
            } else {
                sessionId = new SessionID(clientIp);
                sessions.put(clientIp, sessionId);
            }
        }

        return sessionId.getId().toString();
    }

    private Response handleLogin(IHTTPSession session) {

        Map<String, String> parms = session.getParms();
        String user = parms.get("u");
        String pass = parms.get("p");
        String device = parms.get("d");
        log.info("login: user={}, pass={}, device={}", user, pass, device);

//        if (user==null) {
//            return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "user param missing");
//        }
//        if (pass==null) {
//            return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "pass param missing");
//        }
//        if (device==null) {
//            return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "device param missing");
//        }
        JSONObject obj = new JSONObject();
        obj.put("v", "0.0.1");
        obj.put("s", getSessionID(session));
        log.info("response: {}", obj.toJSONString());

        return new Response(obj.toJSONString());
    }

    private Response handleWrite(IHTTPSession session) {
        return new Response("<html><body>WRITE: it works</body></html>");
    }

    private Response handleRead(IHTTPSession session) {
        // a=3/1/10, s=223f7232-ee73-41c1-8ea7-f7ef1d2adea7, t=0
        Map<String, List<String>> params = getParams(session);
        log.info("read params: {}", params);
        String sessionId = params.get("s").get(0);
        log.info("sessionid: {}", sessionId);
        boolean sessionValid = validateSession(session, sessionId);

        if (!sessionValid) {
            log.warn("Access with invalid session detected: {}", sessionId);
            return new Response(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "");
        }

        List<String> filterIds = params.get("f");
        List<String> addresses = params.get("a");
        List<String> addresshashs = params.get("h");
        String timeout = session.getParms().get("t");
        String index = session.getParms().get("i");

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        int i = index == null ? 0 : Integer.parseInt(index);
        int t = timeout == null ? 0 : Integer.parseInt(timeout);

        long start = System.currentTimeMillis();

        log.info("Reading addresses: {}", addresses);

        for (String address : addresses) {
            
            long remainingTimeout = t>0?t - (System.currentTimeMillis()-start):0;
            
            String value = knx.readGa(address, remainingTimeout);

            if (!value.isEmpty()) {
                data.put(address, value);
                i++;
            } else {
                log.error("Address '{}' not readable");
            }
            if (t > 0 && System.currentTimeMillis() - start > t) {
                log.info("Timeout for query exceeded. Stopping at index {}", i);
                break;
            }

        }
        obj.put("d", data);
        obj.put("i", i);

        log.info("response: {}", obj.toJSONString());

        return new Response(obj.toJSONString());
    }

    private Response handleFilter(IHTTPSession session) {
        return new Response("<html><body>FILTER: it works</body></html>");
    }

}
