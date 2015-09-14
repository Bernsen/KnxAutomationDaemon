package samplescripts;

import de.root1.kad.logicplugin.Logic;
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.KnxException;

/**
 *
 * @author achristian
 */
public class AutoLightOff extends Logic {

    String presenceBathroom = getGA("Presence detector bath room");
    String lightBathroom = getGA("Light bath room");
    
    boolean lastState = false;
    
    @Override
    public void init() {
        setPA("1.1.100"); // when we send out data, we use this individual address
        listenTo(presenceBathroom); // we are interested in presence detection in bathroom
    }

    @Override
    public void knxEvent(GroupAddressEvent event) throws KnxException {
        boolean state = event.asBool(); // get presence state from knx event
        if (lastState && !state) { // if presence is gone ...
            knx.writeBoolean(false, lightBathroom, false);// ... turn off the light
        }
        lastState = state; // store last state
    }
    
}
