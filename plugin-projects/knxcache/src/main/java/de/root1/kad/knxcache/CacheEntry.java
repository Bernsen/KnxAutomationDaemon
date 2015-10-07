/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.knxcache;

/**
 *
 * @author achristian
 */
public class CacheEntry {
    
    private final String address;
    private final long accessTime;
    private final String value;
    private final long timeout;

    public CacheEntry(String address, String value, long timeout) {
        this.address = address;
        this.accessTime = System.currentTimeMillis();
        this.value = value;
        this.timeout = timeout;
    }

    public String getAddress() {
        return address;
    }


    public long getAccessTime() {
        return accessTime;
    }

    public String getValue() {
        return value;
    }
    
    public boolean isValid() {
        return System.currentTimeMillis()-accessTime<timeout;
    }


    @Override
    public String toString() {
        return "CacheEntry{" + "address=" + address + ", accessTime=" + accessTime + ", value=" + value + '}';
    }
    
    
}
