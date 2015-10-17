/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.cvbackend;

import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserSessionID {
    private Logger log = LoggerFactory.getLogger(getClass());

    private UUID id;
    private String ip;
    private long lastAccess;
    private Long listening = new Long(System.currentTimeMillis());
    private final int sessionTimeout;

    public UserSessionID(String ip, int sessionTimeout) {
        this.id = UUID.randomUUID();
        this.ip = ip;
        this.lastAccess = System.currentTimeMillis();
        this.sessionTimeout = sessionTimeout;
    }

    public void renew() {
        this.lastAccess = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public boolean isValid() {
        return System.currentTimeMillis() - lastAccess < sessionTimeout;
    }

    @Override
    public String toString() {
        return "SessionID{ip=" + ip + ", lastAccess=" + new Date(lastAccess) + ",  id=" + id + "}";
    }

    public long setListeningAddresses() {
        // notify about upcoming change
        synchronized (listening) {
            listening.notifyAll();
            listening = System.currentTimeMillis();
            log.info("value is now {}", listening);
            return listening;
        }
    }

    public void waitForListeningAddressesChange(long value) {
        while (listening == value && isValid()) {
            synchronized (listening) {
                try {
                    listening.wait(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (!isValid()) {
            log.info("session invalidated in the meanwhile {}", this);
        } else if (listening != value) {
            log.info("listening addresses changed in the meanwhile: {}->{} {}", value, listening, this);
        } else {
            log.warn("Something went wrong?! {}", this);
        }
    }
}
