/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.knxcache;

import java.util.List;

/**
 *
 * @author achristian
 */
public interface DataListener {
    
    public List<String> listenTo();
    public void newDataArrived(String ga, String value);
    
}
