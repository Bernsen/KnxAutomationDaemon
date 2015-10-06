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

import fi.iki.elonen.NanoHTTPD;
import java.util.HashMap;
import java.util.Iterator;
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
            synchronized(sessions) {
                Iterator<String> iter = sessions.keySet().iterator();
                while(iter.hasNext()) {
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

    class SessionID {

        long SESSION_TIMEOUT = 120 * 1000; // 120sec

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
            return "SessionID{ip=" + ip +", lastAccess=" + lastAccess+ ",  id=" + id +  + '}';
        }
        
        
    }

    BackendServer(int port, String documentRoot) {
        super(port);
        this.documentRoot = documentRoot;
        t.schedule(tt, 5000, 30*60*1000);

    }

    @Override
    public Response serve(IHTTPSession session) {

        log.info("uri: {}", session.getUri());
        log.info("params: {}", session.getParms());
        log.info("headers: {}", session.getHeaders());

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

    private Response handleWrite(IHTTPSession session) {
        return new Response("<html><body>WRITE: it works</body></html>");
    }

    private Response handleRead(IHTTPSession session) {
        return new Response("<html><body>READ: it works</body></html>");
    }

    private Response handleFilter(IHTTPSession session) {
        return new Response("<html><body>FILTER: it works</body></html>");
    }

}
