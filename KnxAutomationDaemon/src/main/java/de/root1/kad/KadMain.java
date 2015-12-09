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

import de.root1.kad.knxservice.KnxServiceImpl;
import de.root1.kad.pf4j.JarPluginManager;
import de.root1.logging.JulFormatter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public class KadMain {

    private final JarPluginManager pluginManager = new JarPluginManager();

    private static final Logger log;

    static {
        JulFormatter.set();
        log = LoggerFactory.getLogger(KadMain.class);
        try {
            String basedir = System.getProperty("kad.basedir");
            
            String devPluginsDir = System.getProperty("kad.developmentPluginsDir");
            String pluginsDir = System.getProperty("kad.pluginsDir");
            
            if (basedir == null) {
                System.setProperty("kad.basedir",".");
                basedir = ".";
            }
            
            if (devPluginsDir == null) {
                System.setProperty("kad.developmentPluginsDir", basedir + File.separator + ".." + File.separator + "plugin-projects");
            }
            
            if (pluginsDir == null) {
                System.setProperty("kad.pluginsDir", basedir + File.separator + "plugins");
            }
            
            System.setProperty("kad.basedir", Utils.shortenFile(new File(basedir)).getAbsolutePath());
            
            log.info("basedir:      {}", Utils.shortenFile(new File(basedir)));
            log.info("Mode:         {}", System.getProperty("kad.mode", "deployment"));
            log.info("devPluginDir: {}", System.getProperty("kad.developmentPluginsDir"));
            log.info("pluginDir:    {}", Utils.shortenFile(new File(System.getProperty("kad.pluginsDir"))));
        } catch (IOException ex) {
            log.error("Error while setting up paths.", ex);
            System.exit(1);
        }
    }

    public KadMain() throws IOException {

        
        log.info("Registering main services ...");
        try {
            registerService(new KnxServiceImpl());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        log.info("Loading Plugins ...");
        pluginManager.loadPlugins();
        
        // insert reference to kadmain in each plugin
        // --> required f.i. to get services
        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for (PluginWrapper pluginWrapper : plugins) {
            Plugin plugin = pluginWrapper.getPlugin();
            if (plugin instanceof KadPlugin) {
                KadPlugin kadPlugin = (KadPlugin) plugin;
                kadPlugin.init(this);
            } else {
                log.error("plugin not supported? -> {}", plugin);
            }
        }
        
        log.info("Starting plugins ...");
        pluginManager.startPlugins();
        log.info("Running!");

        // do nothing while plugins are running... --> need to be replaced with a kind of interactive console or so...!
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new KadMain();
    }

    void registerService(KadService service) {
        synchronized (services) {
            Class serviceClass = service.getServiceClass();
            List<KadService> instances = services.get(serviceClass);
            if (instances == null) {
                instances = new ArrayList<>();
                services.put(serviceClass, instances);
            }
            instances.add(service);
        }
    }

    void unregisterService(KadService service) {

        synchronized (services) {
            Class serviceClass = service.getServiceClass();
            List<KadService> instances = services.get(serviceClass);
            if (instances != null) {
                instances.remove(service);
                if (instances.isEmpty()) {
                    services.remove(serviceClass);
                }
            }
        }

    }

    public <T> List<T> getService(Class<T> serviceClass) {
        synchronized (services) {
            return (List<T>) new ArrayList<>(services.get(serviceClass));
        }
        
    }
    
    ClassLoader getClassLoader(){
        return getClass().getClassLoader();
    }
    
    private final Map<Class, List<KadService>> services = new HashMap<Class, List<KadService>>();

}
