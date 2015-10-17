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

import de.root1.kad.knxservice.KnxService;
import de.root1.kad.knxservice.KnxServiceException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public abstract class Logic {

    public final Logger log = LoggerFactory.getLogger(getClass());
    public KnxService knx;
    private final List<String> groupAddresses = new ArrayList<>();
    private String pa;

    public Logic() {
        
    }

    public void listenTo(String ga) {
        groupAddresses.add(ga);
        log.info(getClass().getCanonicalName()+" now listens to "+ga);
    }

    public abstract void init();

    public abstract void onData(String ga, String value) throws KnxServiceException;

    public List<String> getGroupAddresses() {
        return groupAddresses;
    }
    
    public void setPA(String pa) {
        this.pa = pa;
    }
    
    @Override
    public String toString() {
        return "Logic["+getClass().getCanonicalName()+(pa!=null?"@"+pa:"")+"]";
    }

    void setKnxService(KnxService knx) {
        this.knx = knx;
    }
    
    // ----------------
    // Helper methods
    // ----------------
    
    protected boolean getValueAsBoolean(String value) {
        switch(value) {
            case "on":
                return true;
                
            case "off":
                return false;
        }
        throw new IllegalArgumentException("Value '"+value+"' can not be converted to boolean");
    }

    protected String getBooleanAsValue(boolean b) {
        return b?"1":"0";
    }
    
    

}
