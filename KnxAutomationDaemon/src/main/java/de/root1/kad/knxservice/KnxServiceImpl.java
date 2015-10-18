/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    public KnxServiceImpl() {
        this.gal = new GroupAddressListener() {

            @Override
            public void readRequest(GroupAddressEvent event) {
            }

            @Override
            public void readResponse(GroupAddressEvent event) {
            }

            @Override
            public void write(final GroupAddressEvent event) {
                final String ga = event.getDestination();
                final String gaName = translateGaToName(ga);

                // execute async in thread-pool
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        synchronized (listeners) {

                            // get listeners for this specific ga name
                            List<KnxServiceDataListener> list = listeners.get(gaName);
                            if (list == null) {
                                log.info("There's no special listener for [{}@{}]", gaName, ga);
                                list = new ArrayList<>();
                            } else {
                                log.info("{} listeners for [{}@{}]", list.size(), gaName, ga);
                            }

                            // get also wildcard listeners, listening for all addresses
                            List<KnxServiceDataListener> globalList = listeners.get("*");
                            if (globalList != null) {
                                log.info("{} wildcard listeners", globalList.size());
                                list.addAll(globalList);
                            }

                            for (KnxServiceDataListener listener : list) {

                                String dpt = gaToDptProperties.getProperty(ga);

                                if (dpt==null || dpt.startsWith("-1")) {
                                    log.error("There's no DPT for ["+gaName+"@"+ga+"] known?! Can not read --> will not forward. Skipping.");
                                    return;
                                }
                                String[] split = dpt.split("\\.");
                                int mainType = Integer.parseInt(split[0]);
                                
                                try {
                                    String value = event.asString(mainType, dpt); // slicknx/calimero string style
                                    value = KnxSimplifiedTranslation.decode(dpt, value); // convert to KAD string style (no units etc...)
                                    
                                    listener.onData(gaName, value);
                                    log.info("Forward '{}' with '{}' to [{}@{}]", new Object[]{value, dpt, translateGaToName(ga), ga});
                                } catch (KnxFormatException ex) {
                                    log.error("Error sending value with DPT "+dpt+" to "+ga, ex);
                                }

                            }

                        }
                    }
                };
                pool.execute(r);

            }
        };
        try {
            knx = new Knx();
            knx.setGlobalGroupAddressListener(gal);
            knx.setIndividualAddress(defaultIA);
        } catch (KnxException ex) {
            ex.printStackTrace();
        }
        log.info("Reading knx project data ...");
        readKnxProjectData();
    }

    private File cachedGaPropertiesFile = new File(System.getProperty("kad.basedir") + File.separator + "cache" + File.separator + "knxproject.ga.properties");
    private File cachedDptPropertiesFile = new File(System.getProperty("kad.basedir") + File.separator + "cache" + File.separator + "knxproject.dpt.properties");
    private File knxprojData = new File(System.getProperty("kad.basedir") + File.separator + "conf" + File.separator + "knxproject.knxproj");
    /** NAME -> GA */
    private Properties nameToGaProperties = new Properties();
    /** GA -> DPT */
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
                    gaToDptProperties.put(ga.getAddress(), ga.getTypeString());
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
    public void write(String gaName, String value) throws KnxServiceException {

        String ga = translateNameToGa(gaName);
        String dpt = getDPT(gaName);

        try {
            knx.write(ga, dpt, value);
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
        try {
            return knx.read(gaName, dpt);
        } catch (KnxException ex) {
            throw new KnxServiceException("Problem reading with DPT " + dpt + " from " + ga, ex);
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
    public void registerListener(String gaName, KnxServiceDataListener listener) {
        String ga = translateNameToGa(gaName);
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
    public void unregisterListener(String gaName, KnxServiceDataListener listener) {
        String ga = translateNameToGa(gaName);
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
    public String translateNameToGa(String gaName) {
        if (gaName.equals("*")) {
            return "*";
        } else {
            String name = nameToGaProperties.get(gaName).toString();
            return name!=null?name:gaName;
        }
    }

    @Override
    public String getDPT(String gaName) {
        String ga = translateNameToGa(gaName);
        return gaToDptProperties.getProperty(ga);
    }

    @Override
    public String translateGaToName(String ga) {
        Enumeration<Object> keys = nameToGaProperties.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            String gaValue = (String) nameToGaProperties.get(name);
            if (gaValue.equals(ga)) {
                return name;
            }
        }
        return "[no name translation for ga="+ga+"]";
    }

}
