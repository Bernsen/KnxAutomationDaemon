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
    
    BackendServer(int port, String documentRoot) {
        super(port);
        this.documentRoot = documentRoot;
        
    }

    @Override
    public Response serve(IHTTPSession session) {
        
        log.info("uri: {}",session.getUri());
        log.info("params: {}",session.getParms());
        log.info("headers: {}",session.getHeaders());
        
        String uri = session.getUri();
        
        if (!uri.startsWith(documentRoot)) {
            Response response = new Response("<html><body>URI '"+uri+"' not handled by this server</body></html>"); 
            response.setStatus(Response.Status.BAD_REQUEST);
            return response;
        }
        
        String request = uri.substring(documentRoot.length());
        
        log.info("request: {}", request);
        
        switch(request) {
            case REQUEST_LOGIN:
                return handleLogin();
            case REQUEST_FILTER:
                return handleFilter();
            case REQUEST_READ:
                return handleRead();
            case REQUEST_WRITE:
                return handleWrite();
            default:
                Response response = new Response("<html><body>request '"+request+"' not handled by this server</body></html>"); 
                response.setStatus(Response.Status.BAD_REQUEST);
                return response;
            
        }
        
    }

    private Response handleLogin() {
        return new Response("<html><body>LOGIN: it works</body></html>");
    }

    private Response handleWrite() {
        return new Response("<html><body>WRITE: it works</body></html>");
    }

    private Response handleRead() {
        return new Response("<html><body>READ: it works</body></html>");
    }

    private Response handleFilter() {
        return new Response("<html><body>FILTER: it works</body></html>");
    }
    
    
    
    
}
