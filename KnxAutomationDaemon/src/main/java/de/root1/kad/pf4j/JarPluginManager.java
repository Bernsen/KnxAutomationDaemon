/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.pf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.*;
import ro.fortsoft.pf4j.util.*;

public class JarPluginManager extends DefaultPluginManager {

    private final Logger log = LoggerFactory.getLogger(JarPluginManager.class);

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
    
}
