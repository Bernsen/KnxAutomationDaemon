package samplescripts;

import de.root1.kad.knxservice.KnxServiceException;
import de.root1.kad.logicplugin.Logic;

/**
 *
 * @author achristian
 */
public class AutoLightOff extends Logic {

    String presenceBathroom = "Presence detector bath room";
    String lightBathroom = "Light bath room";
    
    boolean lastState = false;
    
    @Override
    public void init() {
        setPA("1.1.100"); // when we send out data, we use this individual address
        listenTo(presenceBathroom); // we are interested in presence detection in bathroom
    }

    @Override
    public void onDataWrite(String ga, String value) throws KnxServiceException {
        boolean state = getValueAsBoolean(value); // get presence state from knx event
        if (lastState && !state) { // if presence is gone ...
            write(lightBathroom, getBooleanAsValue(false)); // ... turn off the light
            log.info("Licht Bad Auto-OFF");
        }
        lastState = state; // store last state
    }
    
}
