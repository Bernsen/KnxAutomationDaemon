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
package de.root1.kad.pf4j;

import de.root1.kad.pf4j.util.JarFileFilter;
import java.io.File;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.DefaultPluginRepository;
import ro.fortsoft.pf4j.DevelopmentPluginClasspath;
import ro.fortsoft.pf4j.PluginClasspath;
import ro.fortsoft.pf4j.PluginDescriptorFinder;
import ro.fortsoft.pf4j.PluginRepository;
import ro.fortsoft.pf4j.RuntimeMode;

public class JarPluginManager extends DefaultPluginManager {

    private static final Logger log = LoggerFactory.getLogger(JarPluginManager.class);
    private static final String developmentPluginsDir = System.getProperty("kad.developmentPluginsDir", "../plugin-projects");
    private static final String deploymentPluginsDir = System.getProperty("kad.pluginsDir", "plugins");
    private static final RuntimeMode mode = RuntimeMode.valueOf(System.getProperty("kad.mode", "DEPLOYMENT"));


    @Override
    protected PluginClasspath createPluginClasspath() {
        if (RuntimeMode.DEVELOPMENT.equals(getRuntimeMode())) {
            return new DevelopmentPluginClasspath();
        }

        return new JarPluginClasspath();
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new DefaultPluginRepository(pluginsDirectory, new JarFileFilter());
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        return mode;
    }

    @Override
    protected File createPluginsDirectory() {
        // default is deployment mode
        String pluginsDir = deploymentPluginsDir;

        if (RuntimeMode.DEVELOPMENT.equals(getRuntimeMode())) {
            pluginsDir = developmentPluginsDir;
        }
        log.info("Using pluginsdir: {} @ {}", pluginsDir, getRuntimeMode().toString());
        return new File(pluginsDir);
    }
    
    private PluginClasspath getPluginClasspath() {
        
        try {
            Class<? extends JarPluginManager> thisClazz = getClass();
            Field field = thisClazz.getField("pluginClasspath");
            
            field.setAccessible(true);
            PluginClasspath cp = (PluginClasspath) field.get(this);
            
            return cp;
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Cannnot access classpath variable!", ex);
        } 
    }

    /**
     * Add the possibility to override the PluginDescriptorFinder. By default if
     * getRuntimeMode() returns RuntimeMode.DEVELOPMENT than a
     * PropertiesPluginDescriptorFinder is returned else this method returns
     * DefaultPluginDescriptorFinder.
     * @return 
     */
    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        if (RuntimeMode.DEVELOPMENT.equals(getRuntimeMode())) {
            return new KadPropertiesPluginDescriptorFinder();
        }

        return new KadManifestPluginDescriptorFinder(getPluginClasspath());
    }

}
