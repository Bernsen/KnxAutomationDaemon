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

import de.root1.kad.knxcache.DataListener;
import de.root1.kad.knxcache.GaValuePair;
import de.root1.kad.knxcache.KnxCachePlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
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
    private final KnxCachePlugin knx;
    private final int SESSION_TIMEOUT;

    /**
     * HTTP/1.1 200 OK Content-type: text/event-stream;charset=UTF-8
     * Cache-Control: no-cache Connection: keep-alive Transfer-Encoding: chunked
     * Date: Thu, 08 Oct 2015 06:38:17 GMT Server: lighttpd/1.4.35
     *
     * 17 id: data: Hallo Welt
     *
     *
     * 18 id: 1 data: Hallo Welt
     *
     *
     * 18 id: 2 data: Hallo Welt
     *
     *
     * 18 id: 3 data: Hallo Welt
     */
    private IResponse handleSseTest(IHTTPSession session) {

        SseResponse sse = new SseResponse(session);

        int i = 0;
        while (i < 10) {
            i++;
            log.info("Sending message... {}", i);
            sse.sendMessage(Integer.toString(i), null, "Hallo Welt");
            log.info("Sending message...*done*");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        return sse;

    }

    class UserSessionID {

        private UUID id;
        private String ip;
        private long lastAccess;
        private Long listening = new Long(System.currentTimeMillis());

        public UserSessionID(String ip) {
            this.id = UUID.randomUUID();
            this.ip = ip;
            this.lastAccess = System.currentTimeMillis();
        }

        public void renew() {
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

        public long setListeningAddresses() {
            // notify about upcoming change
            synchronized (listening) {
                listening.notifyAll();
                listening = System.currentTimeMillis();
                log.info("value is now {}", listening);
                return listening;
            }
        }

        public void waitForListeningAddressesChange(long value) {
            while (listening == value && isValid()) {
                synchronized (listening) {
                    try {
                        listening.wait(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (!isValid()) {
                log.info("session invalidated in the meanwhile {}", this);
            } else  if (listening!=value) {
                log.info("listening addresses changed in the meanwhile: {}->{} {}", value, listening, this);
            } else {
                log.warn("Something went wrong?! {}", this);
            }
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
            default:
                Response response = new Response("<html><body>resource '" + resource + "' not handled by this server</body></html>");
                response.setStatus(Status.BAD_REQUEST);
                return response;

        }

    }

    public boolean validateSession(IHTTPSession session, String sessionIdString) {
        String clientIp = session.getHeaders().get("http-client-ip");
        synchronized (sessions) {
            UserSessionID sessionId = sessions.get(clientIp);

            if (sessionId != null && sessionId.getId().toString().equals(sessionIdString) && sessionId.isValid()) {
                sessionId.renew();
                return true;
            }
        }
        return false;
    }

    private UserSessionID createUserSessionID(IHTTPSession session) {
        String clientIp = session.getHeaders().get("http-client-ip");
        UserSessionID userSessionId = new UserSessionID(clientIp);
        sessions.put(clientIp, userSessionId);
        return userSessionId;
    }

    /**
     * get usersessionid by client ip
     *
     * @param session
     * @return
     */
    private UserSessionID getUserSessionID(IHTTPSession session) {
        String clientIp = session.getHeaders().get("http-client-ip");
        return sessions.get(clientIp);
    }

    /**
     * get matching usersessionid by usersessionid-string
     *
     * @param sessionIdString
     * @return
     */
    private UserSessionID getUserSessionID(String sessionIdString) {
        synchronized (sessions) {
            Collection<UserSessionID> values = sessions.values();
            for (UserSessionID userSessionId : values) {
                if (userSessionId.getId().toString().equals(sessionIdString)) {
                    return userSessionId;
                }
            }
        }
        return null;
    }

    private Response handleLogin(IHTTPSession session) {

        Map<String, String> parms = session.getParms();
        String user = parms.get("u");
        String pass = parms.get("p");
        String device = parms.get("d");
        log.info("login: user={}, pass={}, device={}", user, pass, device);

        JSONObject obj = new JSONObject();
        obj.put("v", "0.0.1");
        obj.put("s", createUserSessionID(session).getId().toString());
        log.info("response: {}", obj.toJSONString());

        return new Response(obj.toJSONString());
    }

    private Response handleWrite(IHTTPSession session) {
        return new Response("<html><body>WRITE: it works</body></html>");
    }

    private IResponse handleRead(IHTTPSession session) {
        // a=3/1/10, s=223f7232-ee73-41c1-8ea7-f7ef1d2adea7, t=0
        Map<String, List<String>> params = getParams(session);
        log.info("read params: {}", params);

        String sessionString = params.get("s").get(0);
        log.info("sessionString: {}", sessionString);

        UserSessionID userSessionID = getUserSessionID(sessionString);

        if (userSessionID == null || !userSessionID.isValid()) {
            log.warn("Access with unknown/invalid usersession detected: {}", sessionString);
            return new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "");
        }

//        List<String> filterIds = params.get("f");
        final List<String> addresses = params.get("a");
//        List<String> addresshashs = params.get("h");
//        String timeout = session.getParms().get("t");
//        String index = session.getParms().get("i");

        JSONObject jsonResponse = new JSONObject();
        JSONObject jsonData = new JSONObject();
//        int i = index == null ? 0 : Integer.parseInt(index);

        final SseResponse sse = new SseResponse(session);

        log.info("Reading addresses: {}", addresses);

        // client knows nothing. Full response required
        for (String address : addresses) {

            String value = knx.getGa(address);

            if (!value.isEmpty()) {
                jsonData.put(address, value);
            } else {
                log.error("Address '{}' not readable");
            }

        }
        jsonResponse.put("d", jsonData);
        jsonResponse.put("i", "1");

        log.info("response: {}", jsonResponse.toJSONString());
        sse.sendMessage(null, null, jsonResponse.toJSONString());

        DataListener listener = new DataListener() {

            @Override
            public List<String> listenTo() {
                return addresses;
            }

            @Override
            public void newDataArrived(String ga, String value) {
                JSONObject jsonResponse = new JSONObject();
                JSONObject jsonData = new JSONObject();
                jsonData.put(ga, value);
                jsonResponse.put("d", jsonData);
                jsonResponse.put("i", "1");
                log.info("response: {}", jsonResponse.toJSONString());
                sse.sendMessage(null, null, jsonResponse.toJSONString());
            }
        };
        
       
        long listeningAddresses = userSessionID.setListeningAddresses();
        knx.addDataListener(listener);
        
        log.info("Waiting for listening address change ...");
        userSessionID.waitForListeningAddressesChange(listeningAddresses);
        
        log.info("Waiting for listening address change ...*done*");
        knx.removeDataListener(listener);
        
        return sse;
    }

    private Response handleFilter(IHTTPSession session) {
        return new Response("<html><body>FILTER: it works</body></html>");
    }

}
