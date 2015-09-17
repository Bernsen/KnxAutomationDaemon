/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.pf4j;

import com.github.zafarkhaja.semver.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginDescriptor;
import ro.fortsoft.pf4j.PluginDescriptorFinder;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PropertiesPluginDescriptorFinder;
import ro.fortsoft.pf4j.util.StringUtils;

/**
 *
 * @author achristian
 */
public class KadPropertiesPluginDescriptorFinder implements PluginDescriptorFinder {

    private static final Logger log = LoggerFactory.getLogger(KadPropertiesPluginDescriptorFinder.class);

    private static final String DEFAULT_PROPERTIES_FILE_NAME = "plugin.properties";

    private String propertiesFileName;

    public KadPropertiesPluginDescriptorFinder() {
        this(DEFAULT_PROPERTIES_FILE_NAME);
    }

    public KadPropertiesPluginDescriptorFinder(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
    }

    @Override
    public PluginDescriptor find(File pluginRepository) throws PluginException {

        File propertiesFile = new File(pluginRepository, propertiesFileName);
        log.debug("Lookup plugin descriptor in '{}'", propertiesFile);
        if (!propertiesFile.exists()) {
            throw new PluginException("Cannot find '" + propertiesFile + "' file");
        }

        InputStream input = null;
        try {
            input = new FileInputStream(propertiesFile);
        } catch (FileNotFoundException e) {
            // not happening
        }

        Properties properties = new Properties();
        try {
            properties.load(input);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        }

        String id = properties.getProperty("kad.plugin.id");
        if (StringUtils.isEmpty(id)) {
            throw new PluginException("kad.plugin.id cannot be empty");
        }

        String clazz = properties.getProperty("kad.plugin.class");
        if (StringUtils.isEmpty(clazz)) {
            throw new PluginException("kad.plugin.class cannot be empty");
        }

        String version = properties.getProperty("kad.plugin.version");
        if (StringUtils.isEmpty(version)) {
            throw new PluginException("kad.plugin.version cannot be empty");
        }
        
        String description = properties.getProperty("kad.plugin.description");
        if (StringUtils.isEmpty(description)) {
            description = "";
        } 

        String provider = properties.getProperty("kad.plugin.provider");
        String dependencies = properties.getProperty("kad.plugin.dependencies");
        String requires = properties.getProperty("kad.plugin.requires");

        KadPluginDescriptor pluginDescriptor = new KadPluginDescriptor(id, description, clazz, Version.valueOf(version), requires, provider, dependencies);

        return pluginDescriptor;

    }

}
