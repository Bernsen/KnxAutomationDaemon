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
public class GaValuePair {
    
    private String ga;
    private String value;

    public GaValuePair(String ga, String value) {
        this.ga = ga;
        this.value = value;
    }

    public String getGa() {
        return ga;
    }

    public void setGa(String ga) {
        this.ga = ga;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
}
