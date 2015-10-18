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

    /**
     * 
     * @param dpt DPT like "1.001"
     * @param value string value in slicknx/calimero style (with units etc...(
     * @return 
     */
    public static String decode(String dpt, String value) {

        String[] dptSplit = dpt.split("\\.");
        int mainDpt = Integer.parseInt(dptSplit[0]);
        String subDpt = dptSplit[1];

        log.debug("Before post-translation: {} dpt: {}", value, dpt);
        switch (mainDpt) {
            case 1:
                switch (value.toLowerCase()) {
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
                    case "active": //1.011
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
                    case "inactive":
                        value = "0";
                        break;

                }
                break;
            case 9:
                value = removeUnit(mainDpt, value);
        }
        log.debug("After post-translation: {} dpt: {}", value, dpt);

        return value;
    }

    private static String removeUnit(int mainType, String valueWithUnit) {
        String returnValue = valueWithUnit;
        switch (mainType) {
            case 5:
            case 9:
                String[] split = valueWithUnit.split(" ");
                if (split.length == 2) {
                    /*
                     * cut-off all units
                     *  expectation: value is separated from unit with a whitespace and there is just one whitespace
                     */
//                    switch (split[1]) {
//                        case "%":
//                        case "m/s":
//                        case "°C":
//                        case "lx":
                            returnValue = split[0];
//                    }
                }
                break;
            default:

        }
        return returnValue;

    }

}
