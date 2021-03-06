/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KAD Logic Plugin (KLP).
 *
 *   KLP is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KLP is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KLP.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.kad.logicplugin;

import de.root1.kad.logicplugin.cron.AnnotationScheduler;
import de.root1.kad.knxservice.KnxService;
import de.root1.kad.knxservice.KnxServiceConfigurationException;
import de.root1.kad.knxservice.KnxServiceException;
import de.root1.kad.knxservice.KnxSimplifiedTranslation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public abstract class Logic {

    public final Logger log = LoggerFactory.getLogger(getClass());
    private KnxService knx;
    private final List<String> groupAddresses = new ArrayList<>();
    private String pa;
    protected Properties config = new Properties();
    private final File confFile;
    private final String id = getClass().getCanonicalName();
    
    void loadConfig() {
        if (confFile.exists()) {
            try (FileInputStream fis = new FileInputStream(confFile)) {
                config.load(fis);
            } catch (IOException ex) {
                log.error("", ex);
            }
        }
    }
    
    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(confFile)){
            config.store(fos, "Logic "+id+", saved "+new Date().toString());
        } catch (IOException ex) {
            log.error("",ex);
        }
    }

    public enum TYPE {

        READ, WRITE, RESPONSE, UNDEFINED
    };

    public Logic() {
        
        confFile = new File(de.root1.kad.Utils.getConfDir(), "logic_" + id + ".properties");
    }

    public void listenTo(String gaName) {
        try {
            groupAddresses.add(gaName);
            log.info("{} now listens to [{}@{}]", getClass().getCanonicalName(), gaName, knx.translateNameToGa(gaName));
        } catch (KnxServiceConfigurationException ex) {
            log.error("Cannot setup listener for groupaddress name '" + gaName + "'.", ex);
        }
    }

    public abstract void init();

    public void onDataWrite(String ga, String value) throws KnxServiceException {

    }

    public void onData(String ga, String value, TYPE type) throws KnxServiceException {
        // needs to be overwritten by logic script
    }

    public List<String> getGroupAddresses() {
        return groupAddresses;
    }

    public void setPA(String pa) {
        this.pa = pa;
    }

    @Override
    public String toString() {
        return "Logic[" + getClass().getCanonicalName() + (pa != null ? "@" + pa : "") + "]";
    }

    void setKnxService(KnxService knx) {
        log.trace("Setting knxservice");
        this.knx = knx;
    }

    void startCron(AnnotationScheduler scheduler) {
        scheduler.schedule(this);
    }

    // ----------------
    // Helper methods
    // ----------------
    protected boolean getValueAsBoolean(String value) {
        switch (KnxSimplifiedTranslation.decode("1.001", value)) {
            case "on":
            case "1":
                return true;

            case "off":
            case "0":
                return false;
        }
        throw new IllegalArgumentException("Value '" + value + "' can not be converted to boolean");
    }

    protected String getBooleanAsValue(boolean b) {
        return b ? "1" : "0";
    }

    public void write(String gaName, String stringData) throws KnxServiceException {
        if (pa != null) {
            knx.write(pa, gaName, stringData);
        } else {
            knx.write(gaName, stringData);
        }
    }

    public void writeResponse(String gaName, String stringData) throws KnxServiceException {
        if (pa != null) {
            knx.writeResponse(pa, gaName, stringData);
        } else {
            knx.writeResponse(gaName, stringData);
        }
    }

    public String read(String gaName) throws KnxServiceException {
        if (pa != null) {
            return knx.read(pa, gaName);
        } else {
            return knx.read(gaName);
        }
    }

}
