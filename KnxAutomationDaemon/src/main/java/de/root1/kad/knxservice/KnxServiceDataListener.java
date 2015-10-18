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
public interface KnxServiceDataListener {
    
    
    /**
     * knx data received
     * @param gaName name of ga
     * @param value value as string
     */
    public void onData(String gaName, String value);
    
}
