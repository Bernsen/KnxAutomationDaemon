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
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.util.Iterator;
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
    
    private Map<String, CacheEntry> cache = new WeakHashMap<>();
    
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
    public synchronized String readGa(String ga, long timeout) {
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
                    ce = new CacheEntry(ga, value, Integer.parseInt(pluginConfig.getProperty("cachetimeout", "60000")));
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

}
