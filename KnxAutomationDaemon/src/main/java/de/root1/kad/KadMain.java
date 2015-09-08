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
import de.root1.logging.JulLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;

/**
 *
 * @author achristian
 */
public class KadMain {
    
    private Logger log = LoggerFactory.getLogger(getClass());

    public KadMain() {
        
        log.info("Starting PluginManager ...");
        JarPluginManager pluginManager = new JarPluginManager();
        log.info("Loading Plugins ...");
        pluginManager.loadPlugins();
        log.info("Starting plugins ...");
        pluginManager.startPlugins();
        log.info("Running!");
    }
    
    public static void main(String[] args) {
        JulLogFormatter.set();
        new KadMain();
    }
    
}
