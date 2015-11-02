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

/**
 *
 * @author achristian
 */
public interface KnxService {

    
    /**
     * Translates a name of groupaddress to the configured groupaddress
     * @param gaName name of groupaddress, like "foobar"
     * @throws KnxServiceConfigurationException
     * @return groupaddress, like "1/2/3"
     */
    public String translateNameToGa(String gaName) throws KnxServiceConfigurationException;
    
    /**
     * Translates a groupaddress to the configured groupaddress name
     * @param ga groupaddress, like "1/2/3"
     * @throws KnxServiceConfigurationException
     * @return groupaddress, like "foobar"
     */
    public String translateGaToName(String ga) throws KnxServiceConfigurationException;
    
    /**
     * Returns DPT of given groupaddress name
     * @param gaName
     * @throws KnxServiceConfigurationException
     * @return DPT, like "1.001"
     */
    public String getDPT(String gaName) throws KnxServiceConfigurationException;
    
    /**
     * Write data to knx implementation has to lookup the required GA + DPT for
     * given groupaddress name.
     * Data is then feed into DPT conversion and written to knx bus
     *
     * @param gaName groupaddress name
     * @param stringData data in human readable format, matching the human readable representation of the underlying DPT
     * @throws de.root1.kad.knxservice.KnxServiceException
     */
    public void write(String gaName, String stringData) throws KnxServiceException;

    /**
     * Same as {@link KnxService#write(java.lang.String, java.lang.String) }
     * but with an individual address as sender address instead of a default one
     *
     * @param individualAddress individual address
     * @param gaName groupaddress name
     * @param stringData data in human readable format, matching the human readable representation of the underlying DPT
     * @throws de.root1.kad.knxservice.KnxServiceException
     */
    public void write(String individualAddress, String gaName, String stringData) throws KnxServiceException;

    /**
     * Read data from KNX implementation has to lookup the required GA + DPT for given groupaddress name.
     * Read request is then triggered.
     * 
     * @param gaName groupaddress name
     * @return data in human readable format, matching the human readable representation of the underlying DPT
     * @throws de.root1.kad.knxservice.KnxServiceException
     */
    public String read(String gaName) throws KnxServiceException;
    
    /**
     * Same as {@link KnxService#read(java.lang.String) } but with an individual address as sender of request instead of default one
     * @param individualAddress individual address
     * @param gaName groupaddress name
     * @return data in human readable format, matching the human readable representation of the underlying DPT
     * @throws de.root1.kad.knxservice.KnxServiceException
     */
    public String read(String individualAddress, String gaName) throws KnxServiceException;
    
    /**
     * Returns cached value, or null if not in cache
     * @param gaName groupaddress name
     * @return data in human readable format, matching the human readable representation of the underlying DPT, of null if not in cache
     * @throws de.root1.kad.knxservice.KnxServiceException
     */
    public String getCachedValue(String gaName) throws KnxServiceException;
    
    public void registerListener(String gaName, KnxServiceDataListener listener) throws KnxServiceConfigurationException;
    public void unregisterListener(String gaName, KnxServiceDataListener listener) throws KnxServiceConfigurationException;

}
