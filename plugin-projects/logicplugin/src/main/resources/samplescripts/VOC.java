package samplescripts;


import de.root1.kad.logicplugin.Logic;
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.KnxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VOC extends Logic {
    
    String vocGa = getGA("VOC");

    private TimerTask tt = new TimerTask() {

        @Override
        public void run() {
            String[] cmd = new String[]{"/opt/usb-sensors-linux/airsensor/airsensor", "-v", "-o"};
            try {
                Process exec = Runtime.getRuntime().exec(cmd);
                
                InputStream inputStream = exec.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String data = br.readLine();
                br.close();
                int exitvalue = exec.waitFor();
                int vocValue = Integer.parseInt(data);
                log.info("Read VOC value: {} exitvalue: {}",data, exitvalue);
                knx.writeDpt7(false, vocGa, vocValue);
                
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (KnxException ex) {
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                Logger.getLogger(VOC.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    private Timer t = new Timer("VOC reader");
    
    @Override

    public void init() {
        setPA("1.1.202");
        t.schedule(tt, 5000, 60000);
    }

    @Override
    public void knxEvent(GroupAddressEvent event) throws KnxException {
        // nothing to do
    }

}
