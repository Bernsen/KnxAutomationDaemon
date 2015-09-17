/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.pf4j;

import ro.fortsoft.pf4j.PluginClasspath;

/**
 * Overwrite classes directories to "target/classes" and lib directories to "target/lib".
 *
 * @author Decebal Suiu
 */
public class KadDevelopmentPluginClasspath extends PluginClasspath {

	private static final String DEVELOPMENT_CLASSES_DIRECTORY = "target/classes";
	private static final String DEVELOPMENT_LIB_DIRECTORY = "target/lib";

	public KadDevelopmentPluginClasspath() {
		super();
	}

	@Override
	protected void addResources() {
		classesDirectories.add(DEVELOPMENT_CLASSES_DIRECTORY);
		libDirectories.add(DEVELOPMENT_LIB_DIRECTORY);
	}


}
