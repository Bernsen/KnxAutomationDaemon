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

import static de.root1.kad.KadConfiguration.configProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public abstract class KadService implements KadConfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public KadService() {
        readConfig();
    }
    
    protected abstract Class getServiceClass();

    @Override
    public void readConfig() {
        String id = getClass().getCanonicalName();
        File configFile = new File(Utils.getConfDir(), "service_" + id + ".properties");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configFile);
            configProperties.load(fis);
            fis.close();
            log.info("Successfully read config from: {}", configFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            log.info("No configfile: {}", configFile.getAbsolutePath());
        } catch (IOException ex) {
            log.error("Not able to read config file {}", configFile.getAbsolutePath());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                // nothing to do
            }
        }
    }

}
