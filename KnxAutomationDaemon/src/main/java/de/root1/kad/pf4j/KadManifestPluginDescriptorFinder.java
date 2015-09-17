/*
 * Copyright 2012 Decebal Suiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with
 * the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.root1.kad.pf4j;

import com.github.zafarkhaja.semver.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginClasspath;
import ro.fortsoft.pf4j.PluginDescriptor;
import ro.fortsoft.pf4j.PluginDescriptorFinder;
import ro.fortsoft.pf4j.PluginException;

import ro.fortsoft.pf4j.util.StringUtils;

/**
 * Read the plugin descriptor from the manifest file.
 *
 * @author Decebal Suiu
 */
public class KadManifestPluginDescriptorFinder implements PluginDescriptorFinder {

    private static final Logger log = LoggerFactory.getLogger(KadManifestPluginDescriptorFinder.class);

    private PluginClasspath pluginClasspath;

    public KadManifestPluginDescriptorFinder(PluginClasspath pluginClasspath) {
        this.pluginClasspath = pluginClasspath;
    }

    @Override
    public PluginDescriptor find(File pluginRepository) throws PluginException {
        // TODO it's ok with first classes directory? Another idea is to specify in PluginClasspath the folder.
        String classes = pluginClasspath.getClassesDirectories().get(0);
        File manifestFile = new File(pluginRepository, classes + "/META-INF/MANIFEST.MF");
        log.debug("Lookup plugin descriptor in '{}'", manifestFile);
        if (!manifestFile.exists()) {
            throw new PluginException("Cannot find '" + manifestFile + "' file");
        }

        FileInputStream input = null;
        try {
            input = new FileInputStream(manifestFile);
        } catch (FileNotFoundException e) {
            // not happening
        }

        Manifest manifest = null;
        try {
            manifest = new Manifest(input);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        }

//        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        // TODO validate !!!
        Attributes attrs = manifest.getMainAttributes();
        String id = attrs.getValue("Kad-Plugin-Id");
        if (StringUtils.isEmpty(id)) {
            throw new PluginException("Kad-Plugin-Id cannot be empty");
        }

        String description = attrs.getValue("Kad-Plugin-Description");
        if (StringUtils.isEmpty(description)) {
            description = "";
        } 

        String clazz = attrs.getValue("Kad-Plugin-Class");
        if (StringUtils.isEmpty(clazz)) {
            throw new PluginException("Kad-Plugin-Class cannot be empty");
        }

        String version = attrs.getValue("Kad-Plugin-Version");
        if (StringUtils.isEmpty(version)) {
            throw new PluginException("Kad-Plugin-Version cannot be empty");
        }

        String provider = attrs.getValue("Kad-Plugin-Provider");
        String dependencies = attrs.getValue("Kad-Plugin-Dependencies");
        String requires = attrs.getValue("Kad-Plugin-Requires");
        
        KadPluginDescriptor pluginDescriptor = new KadPluginDescriptor(id, description, clazz, Version.valueOf(version), requires, provider, dependencies);

        return pluginDescriptor;
    }

}
