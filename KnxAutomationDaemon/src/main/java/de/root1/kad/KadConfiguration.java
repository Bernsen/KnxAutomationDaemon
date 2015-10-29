/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad;

import java.util.Properties;

/**
 *
 * @author achristian
 */
public interface KadConfiguration {
    
    /**
     * Properties-File containing the configuration
     */
    public final Properties configProperties = new Properties();
    
    /**
     * (Re-)Reads the configuration
     */
    void readConfig();
    
}
