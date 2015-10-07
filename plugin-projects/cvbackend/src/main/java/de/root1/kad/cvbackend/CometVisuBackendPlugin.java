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

import de.root1.kad.KadPlugin;
import de.root1.kad.knxcache.KnxCachePlugin;
import java.io.IOException;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public class CometVisuBackendPlugin extends KadPlugin {

    private final BackendServer backendServer;

    public CometVisuBackendPlugin(PluginWrapper wrapper) {
        super(wrapper);

        PluginWrapper knxcachePluginWrapper = wrapper.getPluginManager().getPlugin("de.root1.kad-knxcache");

        KnxCachePlugin knxCachePlugin = (KnxCachePlugin) knxcachePluginWrapper.getPlugin();

        int port = Integer.parseInt(pluginConfig.getProperty("port", "8080"));
        int sessionTimeout = Integer.parseInt(pluginConfig.getProperty("sessiontimeout", "120000"));
        String pa = pluginConfig.getProperty("pa", "0.0.0");
        String documentRoot = pluginConfig.getProperty("ducumentroot", "/kad/");
        backendServer = new BackendServer(port, documentRoot, knxCachePlugin, sessionTimeout);
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
