/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KAD Knx Cache (KKC).
 *
 *   KKC is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KKC is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KKC.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.kad.knxcache;

import de.root1.kad.KadPlugin;
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public class KnxCachePlugin extends KadPlugin {
    
    /**
     * GA -> CacheEntry
     */
    private Map<String, CacheEntry> cache = Collections.synchronizedMap(new WeakHashMap<String, CacheEntry>());
    
    private final int CACHE_TIMEOUT = Integer.parseInt(pluginConfig.getProperty("cachetimeout", "60000"));
    
    private TimerTask tt = new TimerTask() {

        @Override
        public void run() {
            synchronized (cache) {
                Iterator<String> iterator = cache.keySet().iterator();
                while (iterator.hasNext()) {
                    String address = iterator.next();
                    CacheEntry cacheEntry = cache.get(address);
                    if (!cacheEntry.isValid()) {
                        iterator.remove();
                    }
                }
            }
        }
    };
    
    private Timer t = new Timer("Cache Cleaner");

    private Knx knx;
    private List<DataListener> dataListeners = Collections.synchronizedList(new ArrayList<DataListener>());

    public KnxCachePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() throws PluginException {
        String pa = pluginConfig.getProperty("pa");
        try {
            if (pa != null) {
                knx = new Knx(pa);
            } else {
                knx = new Knx();
            }
            
            int purgetime = Integer.parseInt(pluginConfig.getProperty("purgetime", "20000"));
            log.info("cache purge time: {}ms",purgetime);
            t.schedule(tt, 5000, purgetime);
            
            knx.setGlobalGroupAddressListener(new GroupAddressListener() {

                @Override
                public void readRequest(GroupAddressEvent event) {
                    // not interested in
                }

                @Override
                public void readResponse(GroupAddressEvent event) {
                    // it's a read-response, so the value is up2date
                    putToCache(event);
                }

                @Override
                public void write(GroupAddressEvent event) {
                    putToCache(event);
                }
                
                void putToCache(GroupAddressEvent event) {
                    String ga = event.getDestination();
                    String value = de.root1.kad.Utils.byteArrayToHex(event.getData(), false);
                    log.info("Put to cache: {} -> {}",ga, value);
                    cache.put(ga, new CacheEntry(ga, value, CACHE_TIMEOUT));
                    
                    // notify datalisteners
                    synchronized(dataListeners) {
                        for (DataListener dataListener : dataListeners) {
                            for (String address : dataListener.listenTo()) {
                                if (ga.equals(address)) {
                                    log.info("new data arrived for listener: {}", dataListener.listenTo());
                                    dataListener.newDataArrived(ga, value);
                                }
                            }
                        }
                    }
                }
            });
            
            log.info("KNX Cache started using pa={}!", knx.getIndividualAddress());
            
            
        } catch (KnxException ex) {
            throw new PluginException("Not able to start plugin due to knx problem", ex);
        }
    }
    
    /**
     * Read raw data from KNX
     * @param ga groupaddress to query
     * @return hexadecimal string representation of raw data, without whitespaces
     */
    public synchronized String getGa(String ga) {
        try {
            log.info("reading {}", ga);
            
            CacheEntry ce;
            
            synchronized(cache) {
                ce = cache.get(ga);
                if (ce!=null && (!ce.isValid() || !ce.getAddress().equals(ga))) {
                    cache.remove(ga);
                    log.info("removing from cache: {}",ce);
                } else if (ce!=null && ce.isValid()){
                    log.info("using cached value: {}", ce);
                } else {
                    String value = knx.readRawAsString(ga).replaceAll(" ","");
                    ce = new CacheEntry(ga, value, CACHE_TIMEOUT);
                    cache.put(ga, ce);
                    log.info("adding to cache: {}", ce);
                }
            }
            
            log.info("got value: {}", ce.getValue());
            return ce.getValue();
        } catch (KnxException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    @Override
    public void stop() throws PluginException {
        if (knx!=null) {
            knx.close();
            log.info("KNX Cache stopped!");
        }
    }

    public void addDataListener(DataListener listener) {
        log.info("Register datalistener: {}", listener.listenTo());
        dataListeners.add(listener);
    }

    public void removeDataListener(DataListener listener) {
        log.info("UnRegister datalistener: {}", listener.listenTo());
        dataListeners.remove(listener);
    }

}
