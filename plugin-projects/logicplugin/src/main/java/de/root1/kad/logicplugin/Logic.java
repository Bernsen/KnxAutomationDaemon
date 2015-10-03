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

import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
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
    public Knx knx;
    private final List<String> groupAddresses = new ArrayList<>();
    private String pa;

    public Logic() {
        try {
            knx = new Knx();
        } catch (KnxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void listenTo(String ga) {
        groupAddresses.add(ga);
        log.info(getClass().getCanonicalName()+" now listens to "+ga);
    }

    public abstract void init();

    public abstract void knxEvent(GroupAddressEvent event) throws KnxException;

    public List<String> getGroupAddresses() {
        return groupAddresses;
    }
    
    public void setPA(String pa) {
        try {
            this.pa = pa;
            knx.setIndividualAddress("1.1.100");
            log.info(getClass().getCanonicalName()+" now has PA "+pa);
        } catch (KnxException ex) {
            throw new IllegalArgumentException("pa is invalid", ex);
        }
    }
    
    public String getGA(String name) {
        String ga = GaProvider.getGA(name);
        if (ga==null) {
            throw new LogicException("Group address '"+name+"' not known! Please fix!");
        }
        return ga;
    }

    @Override
    public String toString() {
        return "Logic["+getClass().getCanonicalName()+(pa!=null?"@"+pa:"")+"]";
    }

    
    
    

}
