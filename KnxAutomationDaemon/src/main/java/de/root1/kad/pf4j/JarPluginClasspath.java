/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.pf4j;

import ro.fortsoft.pf4j.PluginClasspath;

public class JarPluginClasspath extends PluginClasspath {


    public JarPluginClasspath() {
        super();
    }

    @Override
    protected void addResources() {
        classesDirectories.add(".");
    }

}
