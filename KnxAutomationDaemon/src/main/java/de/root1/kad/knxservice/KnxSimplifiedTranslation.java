/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.knxservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class KnxSimplifiedTranslation {
    
    private static final Logger log = LoggerFactory.getLogger(KnxSimplifiedTranslation.class);
    
    public static String encode(String dpt, String value) {
        String[] dptSplit = dpt.split("\\.");
        int mainDpt = Integer.parseInt(dptSplit[0]);
        String subDpt = dptSplit[1];
        
        return value;
    }
    
    public static String decode(String dpt, String value) {
        
        String[] dptSplit = dpt.split("\\.");
        int mainDpt = Integer.parseInt(dptSplit[0]);
        String subDpt = dptSplit[1];
        
        log.debug("Before post-translation: {} dpt: {}", value, dpt);
            switch(mainDpt) {
                case 1:
                    switch(value.toLowerCase()){
                        case "on": //1.001
                        case "true": //1.002
                        case "enable": //1.003
                        case "ramp": // 1.004
                        case "alarm": // 1.005
                        case "high": // 1.006
                        case "increase": //1.007
                        case "down": //1.008
                        case "close": //1.009
                        case "start": //1.010
                            value = "1";
                            break;
                        case "off":
                        case "false":
                        case "disable":
                        case "no ramp":
                        case "no alarm":
                        case "low":
                        case "decrease":
                        case "up":
                        case "open":
                        case "stop":
                            value = "0";
                            break;
                            
                    }
            }
            log.debug("After post-translation: {} dpt: {}", value, dpt);
        
        return value;
    }
    
}
