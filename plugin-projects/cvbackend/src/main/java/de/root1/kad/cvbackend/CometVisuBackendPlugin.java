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

import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.io.IOException;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public class CometVisuBackendPlugin extends Plugin {

    private BackendServer backendServer;
    
    public CometVisuBackendPlugin(PluginWrapper wrapper) {
        super(wrapper);
        try {
            backendServer = new BackendServer(8080, "/kad/", new Knx("1.1.203"));
        } catch (KnxException ex) {
            log.error("Could not create KNX instance.", ex);
        }
    }
    
    @Override
    public void start() throws PluginException {
        try {
            super.start();
            if (backendServer != null) {
                backendServer.start();
                log.info("Started CometVisu backend server");
            } else {
                log.error("Cannot start CometVisu backend server due to initialization failure.");
            }
        } catch (IOException ex) {
            throw new PluginException("Error starting server", ex);
        }
    }
    
    @Override
    public void stop() throws PluginException {
        if (backendServer != null) {
            backendServer.stop();
            log.info("Stopped CometVisu backend server");
        } 
        super.stop();
    }
    
}
