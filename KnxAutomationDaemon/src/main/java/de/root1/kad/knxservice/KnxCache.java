/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.knxservice;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
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
    private final int purgetime;

    class CacheEntry {

        private final String address;
        private String value;
        private long lastUpdate;

        public CacheEntry(String address, String value) {
            this.address = address;
            this.lastUpdate = System.currentTimeMillis();
            this.value = value;
        }

        public String getAddress() {
            return address;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "CacheEntry{" + "address=" + address + ", value=" + value +", lastUpdate="+new Date(lastUpdate)+"}";
        }

        private void setValue(String value) {
            this.value = value;
            lastUpdate = System.currentTimeMillis();
        }

    }

    private final Map<String, CacheEntry> cache = new WeakHashMap<>();

    private TimerTask tt = new TimerTask() {

        @Override
        public void run() {
            synchronized (cache) {
                Iterator<String> iterator = cache.keySet().iterator();
                while (iterator.hasNext()) {
                    String address = iterator.next();
                    CacheEntry cacheEntry = cache.get(address);
                    if (System.currentTimeMillis()-cacheEntry.getLastUpdate()>purgetime) {
                        iterator.remove();
                    }
                }
            }
        }
    };

    private Timer t = new Timer("KnxCache Cleaner");

    public KnxCache(int purgetime) {
        log.info("cache purge time: {}ms", purgetime);
        t.schedule(tt, 5000, purgetime);
        this.purgetime = purgetime;
    }

    /**
     * Get cached value
     * @param ga groupaddress to query for
     * @return cached value, or null if not found in cache
     */
    public String get(String ga) {
        CacheEntry ce;
        synchronized (cache) {
            ce = cache.get(ga);
            if (ce!=null && System.currentTimeMillis()-ce.getLastUpdate()<purgetime) {
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
     * @param ga
     * @param value 
     */
    void update(String ga, String value) {
        synchronized(cache){
            CacheEntry ce = cache.get(ga);
            if (ce==null) {
                ce = new CacheEntry(ga, value);
                cache.put(ga, ce);
            }
            log.info("Updating cache: {} -> '{}'", ga, value);
            ce.setValue(value);
        }
    }

}
