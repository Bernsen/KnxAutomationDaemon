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

import de.root1.kad.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class KnxCache {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private int purgetime;

    private final Map<String, CacheEntry> cache = new WeakHashMap<>();

    private TimerTask taskCacheClean = new TimerTask() {

        @Override
        public void run() {
            synchronized (cache) {
                Iterator<String> iterator = cache.keySet().iterator();
                while (iterator.hasNext()) {
                    String address = iterator.next();
                    CacheEntry cacheEntry = cache.get(address);
                    if (System.currentTimeMillis() - cacheEntry.getLastUpdate() > purgetime) {
                        log.info("Removing from cache: {}", cacheEntry);
                        iterator.remove();
                    }
                }
            }
        }
    };

    private TimerTask taskCachePersist = new TimerTask() {

        @Override
        public void run() {
            synchronized (cache) {
                try {
                    log.info("Persisting cache. Number of items: {}", cache.size());
                    if (cacheFile.exists()) {
                        cacheFile.delete();
                    }
                    FileOutputStream fos = new FileOutputStream(cacheFile);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    Collection<CacheEntry> values = cache.values();
                    oos.writeLong(values.size());
                    for (CacheEntry ce : values) {
                        oos.writeObject(ce);
                    }
                    oos.close();
                    log.info("Persisting cache... *done*");
                } catch (IOException ex) {
                    log.error("Error persisting cache. Deleting cache.", ex);
                    if (cacheFile.exists()) {
                        cacheFile.delete();
                    }
                }
            }
        }
    };

    private Timer timerCacheClean = new Timer("KnxCache Cleaner");
    private Timer timerPersistCache;
    private boolean persist;
    private int interval;
    private File cacheFile = new File(Utils.getCacheDir(), "KnxCache.dat");

    public KnxCache(Properties configProperties) {

        purgetime = Integer.parseInt(configProperties.getProperty("knx.cache.purgetime", "480" /*8 hrs*/));
        persist = Boolean.parseBoolean(configProperties.getProperty("knx.cache.persist", "false"));

        /*
         0 = every update
         1..n == every n minute
         */
        interval = Integer.parseInt(configProperties.getProperty("knx.cache.persist.interval", "15" /* 15min*/));

        log.info("cache purge time: {}min", purgetime);
        log.info("cache persist: {}", persist);
        log.info("cache persist interval: {}min", interval);
        interval *= 1000 * 60; // calc minutes to milliseconds
        purgetime *= 1000 * 60; 

        if (persist) {
            if (interval > 0) {
                timerPersistCache = new Timer("KnxCache Persist");
                timerPersistCache.schedule(taskCachePersist, interval, interval);
                log.info("Started persistance timer");
            } else {
                log.info("Will persist cache on every change");
            }
        }

        // start with 5sec delay
        timerCacheClean.schedule(taskCacheClean, 5000, purgetime);

        if (persist) {
            try {
                FileInputStream fis = new FileInputStream(cacheFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                long count = ois.readLong();
                log.info("Filling cache with persisted data. Reading {} items from cache.", count);
                for (int i = 0; i < count; i++) {
                    CacheEntry ce = (CacheEntry) ois.readObject();
                    cache.put(ce.getAddress(), ce);
                }
                log.info("Done filling cache");
            } catch (FileNotFoundException ex) {
                log.info("No cache file present. Skip.");
            } catch (ClassNotFoundException | IOException ex) {
                log.error("Error reading cache. Will delete cache file", ex);
            }
        }
    }

    /**
     * Get cached value
     *
     * @param ga groupaddress to query for
     * @return cached value, or null if not found in cache
     */
    public String get(String ga) {
        CacheEntry ce;
        synchronized (cache) {
            ce = cache.get(ga);
            if (ce != null && System.currentTimeMillis() - ce.getLastUpdate() < purgetime) {
                log.info("got value from cache: {}", ce);
                return ce.getValue();
            } else {
                cache.remove(ga);
                return null;
            }
        }
    }

    /**
     * Update value in cache
     *
     * @param ga
     * @param value
     */
    void update(String ga, String value) {
        synchronized (cache) {
            CacheEntry ce = cache.get(ga);
            if (ce == null) {
                ce = new CacheEntry(ga, value);
                cache.put(ga, ce);
            }
            log.info("Updating cache({}): {} -> '{}'", cache.size(), ga, value);
            ce.setValue(value);
            if (persist && interval == 0) {
                taskCachePersist.run();
            }
        }
    }

}
