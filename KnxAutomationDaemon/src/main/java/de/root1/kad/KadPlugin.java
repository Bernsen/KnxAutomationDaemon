/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public abstract class KadPlugin extends ro.fortsoft.pf4j.Plugin implements KadConfiguration {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private KadMain kadmain;

    public KadPlugin(PluginWrapper wrapper) {
        super(wrapper);
        readConfig();
    }

    public void readConfig() {
        String id = wrapper.getPluginId();
        File configFile = new File(Utils.getConfDir(), "plugin_"+id + ".properties");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configFile);
            configProperties.load(fis);
            fis.close();
            log.info("Successfully read config from: {}", configFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            log.info("No configfile: {}", configFile.getAbsolutePath());
        } catch (IOException ex) {
            log.error("Not able to read config file {}", configFile.getAbsolutePath());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                // nothing to do
            }
        }
    }

    void init(KadMain kadMain) {
        this.kadmain = kadMain;
    }
    
    protected void registerService(KadService service) {
        if (kadmain==null) {
            throw new IllegalStateException("Don't call registerService from plugin-constructor.");
        }
        kadmain.registerService(service);
    }
    
    public <T> List<T> getService(Class<T> serviceClass) {
        if (kadmain==null) {
            throw new IllegalStateException("Don't call getService from plugin-constructor.");
        }
        return kadmain.getService(serviceClass);
    }
    
    public ClassLoader getKadClassLoader() {
        return getWrapper().getPluginManager().getClass().getClassLoader();
    }
    
    

}
