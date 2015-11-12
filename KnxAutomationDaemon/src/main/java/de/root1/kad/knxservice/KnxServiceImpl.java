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
package de.root1.kad.knxservice;

import de.root1.ets4reader.GroupAddress;
import de.root1.ets4reader.KnxProjReader;
import de.root1.kad.KadService;
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import de.root1.slicknx.KnxFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class KnxServiceImpl extends KadService implements KnxService {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String defaultIA = "1.1.254";
    private Knx knx;

    /**
     * GA Name -> ListenerList
     */
    private final Map<String, List<KnxServiceDataListener>> listeners = new HashMap<>();

    ExecutorService pool = Executors.newCachedThreadPool(new NamedThreadFactory("KnxServiceOndataForwarding"));

    private GroupAddressListener gal;
    
    private final KnxCache cache;

    public KnxServiceImpl() {
        super();
        cache = new KnxCache(configProperties);
        this.gal = new GroupAddressListener() {
            
            private void processEvent(GroupAddressEvent event) {
                KnxServiceDataListener.TYPE type = KnxServiceDataListener.TYPE.WRITE;
                
                String ga=null;
                String dpt=null;
                String gaName=null;
                try {
                    ga = event.getDestination();
                    dpt = gaToDptProperties.getProperty(ga);
                    String[] split = dpt.split("\\.");
                    int mainType = Integer.parseInt(split[0]);
                    String value = event.asString(mainType, dpt); // slicknx/calimero string styleq
                    switch(event.getType()) {
                        case GROUP_READ:
                            type = KnxServiceDataListener.TYPE.READ;
                            value = null;
                            break;
                        case GROUP_RESPONSE:
                            type = KnxServiceDataListener.TYPE.RESPONSE;
                            value = KnxSimplifiedTranslation.decode(dpt, value);
                            break;
                        case GROUP_WRITE:
                            type = KnxServiceDataListener.TYPE.WRITE;
                            break;
                    }
                    
                    fireKnxEvent(ga, gaName, dpt, value, type);

                } catch (KnxFormatException ex) {
                    log.warn("Error converting data to String with DPT"+dpt+". event="+event+" ga="+ga+" gaName='"+gaName+"'", ex);
                } 
            }

            @Override
            public void readRequest(GroupAddressEvent event) {
                processEvent(event);
            }

            @Override
            public void readResponse(GroupAddressEvent event) {
                processEvent(event);
            }

            @Override
            public void write(final GroupAddressEvent event) {
                processEvent(event);
            }

        };

        log.info("Reading knx project data ...");
        readKnxProjectData();
        try {
            knx = new Knx();
            knx.setGlobalGroupAddressListener(gal);
            knx.setIndividualAddress(configProperties.getProperty("knx.individualaddress", defaultIA));
        } catch (KnxException ex) {
            log.error("Error setting up knx access", ex);
        }

    }

    private void fireKnxEvent(final String ga, final String finalGaName, final String dpt, String value, final KnxServiceDataListener.TYPE type) {
        final String finalValue = KnxSimplifiedTranslation.decode(dpt, value); // convert to KAD string style (no units etc...)
        switch(type) {
            case RESPONSE:
            case WRITE:
                cache.update(ga, finalValue);
                break;
        }
        synchronized (listeners) {

            // get listeners for this specific ga name
            List<KnxServiceDataListener> list = listeners.get(ga);
            if (list == null) {
                log.debug("There's no special listener for [{}@{}]", finalGaName, ga);
                list = new ArrayList<>();
            } else {
                log.debug("{} listeners for [{}@{}]", list.size(), finalGaName, ga);
            }

            // get also wildcard listeners, listening for all addresses
            final List<KnxServiceDataListener> globalList = listeners.get("*");
            if (globalList != null) {
                log.debug("{} wildcard listeners", globalList.size());
            }

            if (dpt == null || dpt.startsWith("-1")) {
                log.error("There's no DPT for [" + finalGaName + "@" + ga + "] known?! Can not read --> will not forward. Skipping.");
                return;
            }

            for (final KnxServiceDataListener listener : list) {
                // execute listener async in thread-pool
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        log.info("Forwarding '{}' with '{}' to [{}@{}]->{}", new Object[]{finalValue, dpt, finalGaName, ga, listener});
                        listener.onData(finalGaName, finalValue, type);
                    }

                };
                pool.execute(r);
            }

            if (globalList != null) {
                for (final KnxServiceDataListener listener : globalList) {
                    // execute listener async in thread-pool
                    Runnable r = new Runnable() {

                        @Override
                        public void run() {
                            log.debug("Forwarding wildcard '{}' with '{}' to [{}@{}]->{}", new Object[]{finalValue, dpt, finalGaName, ga, listener});
                            listener.onData(finalGaName, finalValue, type);
                        }

                    };
                    pool.execute(r);
                }
            }

        }
    }

    private File cachedGaPropertiesFile = new File(System.getProperty("kad.basedir") + File.separator + "cache" + File.separator + "knxproject.ga.properties");
    private File cachedDptPropertiesFile = new File(System.getProperty("kad.basedir") + File.separator + "cache" + File.separator + "knxproject.dpt.properties");
    private File knxprojData = new File(System.getProperty("kad.basedir") + File.separator + "conf" + File.separator + "knxproject.knxproj");
    /**
     * NAME -> GA
     */
    private Properties nameToGaProperties = new Properties();
    /**
     * GA -> DPT
     */
    private Properties gaToDptProperties = new Properties();

    private void readKnxProjectData() {

        String checksumCache = "notyetread";
        String checksumCacheKnxprojUserxml = "notyetcalculated";

        String checksumKnxproj = "notyetcalculated";
        String checksumKnxprojUserxml = "notyetcalculated";

        boolean useCacheOnly = false;
        boolean ok = false;

        try {

            if (cachedGaPropertiesFile.exists()) {
                nameToGaProperties.load(new FileInputStream(cachedGaPropertiesFile));
                checksumCache = nameToGaProperties.getProperty("knxproject.checksum", "");
                checksumCacheKnxprojUserxml = nameToGaProperties.getProperty("knxproject.userxml.checksum", "");
            }

            if (cachedDptPropertiesFile.exists()) {
                gaToDptProperties.load(new FileInputStream(cachedDptPropertiesFile));
                if (checksumCache.equals("notyetread")) {
                    checksumCache = nameToGaProperties.getProperty("knxproject.checksum", "");
                }
            }

            if (knxprojData.exists()) {
                checksumKnxproj = de.root1.kad.Utils.createSHA1(knxprojData);
            } else {
                log.warn("No knxproject data available. Using cached value only!");
                useCacheOnly = true;
            }

            if (knxprojData.exists()) {
                checksumKnxprojUserxml = de.root1.kad.Utils.createSHA1(knxprojData);
            }

            if (useCacheOnly || (checksumCache.equals(checksumKnxproj) && checksumCacheKnxprojUserxml.equals(checksumKnxprojUserxml))) {

                log.info("No change in knxproject data detected. Continue with cached values.");
                ok = true;

            } else {

                log.info("knxproject data change detected. Reading data ...");
                cachedGaPropertiesFile.delete();
                nameToGaProperties.clear();
                gaToDptProperties.clear();

                nameToGaProperties.put("knxproject.checksum", checksumKnxproj);
                nameToGaProperties.put("knxproject.userxml.checksum", checksumKnxprojUserxml);

                log.info("Reading knx project data ...");
                KnxProjReader kpr = new KnxProjReader(knxprojData);
                List<GroupAddress> groupaddressList = kpr.getProjects().get(0).getGroupaddressList();
                Map<String, String> gaMap = new HashMap<>();
                for (GroupAddress ga : groupaddressList) {
                    gaMap.put(ga.getName(), ga.getAddress());
                    nameToGaProperties.put(ga.getName(), ga.getAddress());
                    gaToDptProperties.put(ga.getAddress(), ga.getDataPointType());
                }

                // writing to cache
                log.info("Writing cache data ...");

                try (FileOutputStream fos = new FileOutputStream(cachedGaPropertiesFile);) {
                    log.info("Writing {} cache data to {} ...", nameToGaProperties.size(), cachedGaPropertiesFile);
                    nameToGaProperties.store(fos, "This is GA-Name to GA cache");
                    fos.close();
                } catch (IOException ex) {
                    log.warn("Cannot write ga cache to " + cachedGaPropertiesFile.getCanonicalPath(), ex);
                }

                try (FileOutputStream fos = new FileOutputStream(cachedDptPropertiesFile);) {
                    log.info("Writing {} cache data to {} ...", gaToDptProperties.size(), cachedDptPropertiesFile);
                    gaToDptProperties.store(fos, "This is GA-Name to GA cache");
                    fos.close();
                } catch (IOException ex) {
                    log.warn("Cannot write dpt cache to " + cachedDptPropertiesFile.getCanonicalPath(), ex);
                }

                log.info("Writing cache data ...*done*");

                log.info("Reading knx project data ... *DONE*");
                ok = true;
            }

        } catch (Exception ex) {
            log.warn("Error while reading file data", ex);
        } finally {
            if (!ok) {
                log.warn("GA and DPT cache not available");
                nameToGaProperties.clear();
                gaToDptProperties.clear();
            }
        }
    }
    
    
    @Override
    public void writeResponse(String gaName, String value) throws KnxServiceException {

        String ga = translateNameToGa(gaName);
        String dpt = getDPT(gaName);

        try {
            knx.write(true, ga, dpt, value);
            fireKnxEvent(ga, gaName, dpt, value, KnxServiceDataListener.TYPE.RESPONSE);
        } catch (KnxException ex) {
            throw new KnxServiceException("Problem writing '" + value + "' with DPT " + dpt + " to " + ga, ex);
        }
    }
    
    @Override
    public void writeResponse(String individualAddress, String gaName, String value) throws KnxServiceException {
        try {
            knx.setIndividualAddress(individualAddress);
            writeResponse(gaName, value);
            knx.setIndividualAddress(defaultIA);
        } catch (KnxServiceException | KnxException ex) {
            throw new KnxServiceException("Problem writing", ex);
        }
    }

    @Override
    public void write(String gaName, String value) throws KnxServiceException {

        String ga = translateNameToGa(gaName);
        String dpt = getDPT(gaName);

        try {
            knx.write(false, ga, dpt, value);
            fireKnxEvent(ga, gaName, dpt, value, KnxServiceDataListener.TYPE.WRITE);
        } catch (KnxException ex) {
            throw new KnxServiceException("Problem writing '" + value + "' with DPT " + dpt + " to " + ga, ex);
        }
    }

    @Override
    public void write(String individualAddress, String gaName, String value) throws KnxServiceException {
        try {
            knx.setIndividualAddress(individualAddress);
            write(gaName, value);
            knx.setIndividualAddress(defaultIA);
        } catch (KnxServiceException | KnxException ex) {
            throw new KnxServiceException("Problem writing", ex);
        }
    }

    @Override
    public String read(String gaName) throws KnxServiceException {

        String ga = translateNameToGa(gaName);
        String dpt = getDPT(gaName);

        String value = cache.get(ga);
        if (value==null) {
            try {
                value = knx.read(ga, dpt);
                log.info("Cache does not contain value for [{}@{}], reading from bus.", gaName, ga);
                value = KnxSimplifiedTranslation.decode(dpt, value);
                cache.update(ga, value);
                return value;
            } catch (KnxException ex) {
                throw new KnxServiceException("Problem reading with DPT " + dpt + " from " + ga, ex);
            }
        } else {
            return value;
        }
    }

    @Override
    public String read(String individualAddress, String gaName) throws KnxServiceException {
        try {
            knx.setIndividualAddress(individualAddress);
            String read = read(gaName);
            knx.setIndividualAddress(defaultIA);
            return read;
        } catch (KnxServiceException | KnxException ex) {
            throw new KnxServiceException("Problem writing", ex);
        }
    }

    @Override
    public void registerListener(String gaName, KnxServiceDataListener listener) throws KnxServiceConfigurationException {
        if (gaName == null || gaName.isEmpty()) {
            throw new IllegalArgumentException("gaName must not be null or empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("lister must not be null");
        }
        String ga = translateNameToGa(gaName);
        log.debug("[{}@{}]", gaName, ga);
        synchronized (listeners) {
            List<KnxServiceDataListener> list = listeners.get(ga);
            if (list == null) {
                list = new ArrayList<>();
                listeners.put(ga, list);
            }
            list.add(listener);
        }
    }

    @Override
    public void unregisterListener(String gaName, KnxServiceDataListener listener) throws KnxServiceConfigurationException {
        if (gaName == null || gaName.isEmpty()) {
            throw new IllegalArgumentException("gaName must not be null or empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("lister must not be null");
        }
        String ga = translateNameToGa(gaName);
        log.debug("[{}@{}]", gaName, ga);
        synchronized (listeners) {
            List<KnxServiceDataListener> list = listeners.get(ga);
            if (list != null) {
                list.remove(listener);
                if (list.isEmpty()) {
                    listeners.remove(ga);
                }
            }
        }
    }

    @Override
    protected Class getServiceClass() {
        return KnxService.class;
    }

    @Override
    public String translateNameToGa(String gaName) throws KnxServiceConfigurationException {
        if (gaName.equals("*")) {
            return "*";
        } else {
            String name = (String) nameToGaProperties.get(gaName);
            if (name == null) {
                throw new KnxServiceConfigurationException("Group address name [" + gaName + "] can not be resolved to a groupaddress. Name unkown.");
            }
            return name;
        }
    }

    @Override
    public String getDPT(String gaName) throws KnxServiceConfigurationException {
        String ga = translateNameToGa(gaName);
        String dpt = gaToDptProperties.getProperty(ga);
        if (dpt == null) {
            throw new KnxServiceConfigurationException("Group address [" + gaName + "@" + ga + "] has no associated DPT. Please update configuration.");
        }
        return dpt;
    }

    @Override
    public String translateGaToName(String ga) throws KnxServiceConfigurationException {
        Enumeration<Object> keys = nameToGaProperties.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            String gaValue = (String) nameToGaProperties.get(name);
            if (gaValue.equals(ga)) {
                return name;
            }
        }
        throw new KnxServiceConfigurationException("Group address [" + ga + "] can not be resolved to a groupaddress name. GA unkown.");
    }

    @Override
    public String getCachedValue(String gaName) throws KnxServiceException {
        String value=null;
        try {
            String ga = translateNameToGa(gaName);
            String dpt = getDPT(gaName);

            value = cache.get(ga);
        } catch (KnxServiceException ex) {
            throw new KnxServiceException("Problem translating '"+gaName+"' to GA", ex);
        }
        return value;        
    }

}
