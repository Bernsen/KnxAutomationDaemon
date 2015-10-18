/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
     * @return groupaddress, like "1/2/3"
     */
    public String translateNameToGa(String gaName);
    
    /**
     * Translates a groupaddress to the configured groupaddress name
     * @param ga groupaddress, like "1/2/3"
     * @return groupaddress, like "foobar"
     */
    public String translateGaToName(String ga);
    
    /**
     * Returns DPT of given groupaddress name
     * @param gaName
     * @return DPT, like "1.001"
     */
    public String getDPT(String gaName);
    
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
    
    public void registerListener(String gaName, KnxServiceDataListener listener);
    public void unregisterListener(String gaName, KnxServiceDataListener listener);

}
