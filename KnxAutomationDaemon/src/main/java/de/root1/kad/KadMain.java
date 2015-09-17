/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KnxAutomationDaemon (KAD).
 *
 *   KAD is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KAD is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KAD.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.kad;

import de.root1.kad.pf4j.JarPluginManager;
import de.root1.logging.JulFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class KadMain {
    
    private Logger log = LoggerFactory.getLogger(getClass());

    public KadMain() {
        log.info("Mode: {}", System.getProperty("kad.mode"));
        log.info("devPluginDir: {}", System.getProperty("kad.developmentPluginsDir"));
        log.info("pluginDir: {}", System.getProperty("kad.pluginsDir"));
        
        log.info("Starting PluginManager ...");
        JarPluginManager pluginManager = new JarPluginManager();
        
        log.info("Loading Plugins ...");
        pluginManager.loadPlugins();
        log.info("Starting plugins ...");
        pluginManager.startPlugins();
        log.info("Running!");
        
        // do nothing while plugins are running... --> need to be replaced with a kind of interactive console or so...!
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        JulFormatter.set();
        new KadMain();
    }
    
}
