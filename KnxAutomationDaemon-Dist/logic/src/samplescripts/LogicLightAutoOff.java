package samplescripts;

import de.root1.kad.knxservice.KnxServiceException;
import de.root1.kad.logicplugin.Logic;

public class LogicLightAutoOff extends Logic {

    String gaLightBathroom = "Light Bathroom";
    String gaPresenceBathroom = "Presence Bathroom";

    boolean lastState = false;

    @Override
    public void init() {
        setPA("1.1.100");
        listenTo(gaPresenceBathroom);

        try {
            lastState = getValueAsBoolean(read(gaPresenceBathroom));
        } catch (KnxServiceException ex) {
            log.warn("not able to retrieve last state", ex);
        }

        log.info("Starting with last state = {}", lastState);
    }

    @Override
    public void onDataWrite(String ga, String value) throws KnxServiceException {
        boolean state = getValueAsBoolean(value); // get presence state from knx event
        log.info("Received presence state: {}", state);
        if (lastState && !state) { // if presence is gone ...
            write(gaLightBathroom, getBooleanAsValue(false)); // ... turn off the light
            log.info("Light Auto-OFF");
        }
        lastState = state; // store last state
    }

}
