/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.cvbackend;

import org.json.simple.JSONObject;

/**
 *
 * @author achristian
 */
public class Test {
    
    public static void main(String[] args) {
        String ga = "";
        String value="";
        
        JSONObject jsonResponse = new JSONObject();
        JSONObject jsonData = new JSONObject();
        jsonData.put(ga, value);
        jsonResponse.put("d", jsonData);
        jsonResponse.put("i", "1");
        System.out.println(jsonResponse.toJSONString());
    }
    
}
