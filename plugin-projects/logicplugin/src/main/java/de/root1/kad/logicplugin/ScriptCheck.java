/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.kad.logicplugin;

import de.root1.logging.JulFormatter;
import java.io.File;
import java.util.List;

/**
 *
 * @author achristian
 */
public class ScriptCheck {
    
    public static void main(String[] args) throws LoadSourceException {
        JulFormatter.set();
        File srcDir = new File(System.getProperty("kad.basedir")+File.separator+"logic", "src");
        File libDir = new File(System.getProperty("kad.basedir")+File.separator+"logic", "lib");
        String scriptToCheck = null;
        
//        System.out.println(Arrays.toString(args));
        
        if (args!=null && args.length>0) {
            scriptToCheck = args[0];
        }
        
        if (scriptToCheck!=null) {
            Utils.log.info("Searching for script ["+scriptToCheck+"] in "+srcDir.getAbsolutePath()+" ...");
        } else {
            Utils.log.info("Searching scripts in "+srcDir.getAbsolutePath()+" ...");
        }
        List<SourceContainer> sourceContainers = Utils.getSourceContainers(srcDir, libDir);
        
        for (SourceContainer sourceContainer : sourceContainers) {
            if (scriptToCheck!=null) {
                if (sourceContainer.getCanonicalClassName().equals(scriptToCheck)) {
                    Utils.log.info("Checking script ["+sourceContainer.getCanonicalClassName()+"] ...");
                    sourceContainer.loadLogic();
                }
            } else {
                Utils.log.info("Checking script ["+sourceContainer.getCanonicalClassName()+"] ...");
                sourceContainer.loadLogic();
            }
        }
        Utils.log.info("All checks done!");
    }
    
}
