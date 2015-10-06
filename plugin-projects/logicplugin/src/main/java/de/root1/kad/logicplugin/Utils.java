/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KAD Logic Plugin (KLP).
 *
 *   KLP is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KLP is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KLP.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.kad.logicplugin;

import de.root1.ets4reader.GroupAddress;
import de.root1.ets4reader.KnxProjReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class Utils {

    public static final Logger logger = LoggerFactory.getLogger(Utils.class);
    
    /*
     * Filter for files with .java extension + folders. nothing else.
     */
    public static final FileFilter scriptFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {

            return (file.isFile() && file.getName().endsWith(".java")) || file.isDirectory();

        }
    };
    
    public static void readKnxProjectData() {

        File cachedProjectData = new File(System.getProperty("kad.basedir")+File.separator+"cache"+File.separator+"knxproject.properties");
        File knxprojData = new File(System.getProperty("kad.basedir")+File.separator+"conf"+File.separator+"knxproject.knxproj");

        String checksumCache = "notyetread";
        String checksumKnxproj = "notyetcalculated";
        Properties data = new Properties();

        boolean useCacheOnly = false;
        boolean ok = false;

        try {

            if (cachedProjectData.exists()) {
                data.load(new FileInputStream(cachedProjectData));
                checksumCache = data.getProperty("knxproject.checksum", "");
            }

            if (knxprojData.exists()) {
                checksumKnxproj = Utils.createSHA1(knxprojData);
            } else {
                logger.warn("No knxproject data available. Using cached value only!");
                useCacheOnly = true;
            }

            if (useCacheOnly || checksumCache.equals(checksumKnxproj)) {

                logger.info("No change in knxproject data detected. Continue with cached values.");
                Map<String, String> gaMap = new HashMap<>();
                for (final String name : data.stringPropertyNames()) {
                    gaMap.put(name, data.getProperty(name));
                }
                GaProvider.setGaMap(gaMap);
                ok = true;

            } else {

                logger.info("knxproject data change detected. Reading data ...");
                cachedProjectData.delete();
                data.clear();
                try (FileOutputStream fos = new FileOutputStream(cachedProjectData)) {
                    logger.info("Reading knx project data ...");
                    KnxProjReader kpr = new KnxProjReader(knxprojData);

                    List<GroupAddress> groupaddressList = kpr.getProjects().get(0).getGroupaddressList();
                    Map<String, String> gaMap = new HashMap<>();
                    for (GroupAddress ga : groupaddressList) {
                        gaMap.put(ga.getName(), ga.getAddress());
                        data.put(ga.getName(), ga.getAddress());
                    }
                    
                    logger.info("Writing {} cache data to {} ...", data.size(), cachedProjectData);
                    // writing to cache
                    data.put("knxproject.checksum", checksumKnxproj);
                    data.store(fos, "Created on "+new Date().toString());
                    fos.close();
                    logger.info("Writing cache data ...*done*");
                    
                    GaProvider.setGaMap(gaMap);
                    logger.info("Reading knx project data ... *DONE*");
                    ok = true;
                } catch (IOException ex) {
                    logger.warn("Cannot read knx project from " + knxprojData.getAbsolutePath(), ex);
                } catch (JDOMException ex) {
                    logger.error("Cannot parse knx project file " + knxprojData.getAbsolutePath(), ex);
                }

            }

        } catch (Exception ex) {
            logger.warn("Error while reading file data", ex);
        } finally {
            if (!ok) {
                logger.warn("Scripts depending on GA names might not work properly!");
            }
        }
    }

    public static List<SourceContainer> getSourceContainers(File scriptsdir) {
        List<SourceContainer> result = new ArrayList<>();

        Stack<File> stack = new Stack<>();
        stack.addAll(Arrays.asList(scriptsdir.listFiles(scriptFileFilter)));

        while (!stack.empty()) {

            File f = stack.pop();

            if (f.isDirectory()) {

                stack.addAll(Arrays.asList(f.listFiles(scriptFileFilter)));

            } else {

                try {
                    // it's a file
                    SourceContainer sc = new SourceContainer(scriptsdir, f);
                    result.add(sc);

                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }

        }
        return result;
    }

    public static String byteArrayToHex(byte[] bytearray, boolean whitespace) {
        StringBuilder sb = new StringBuilder(bytearray.length * 2);

        for (int i = 0; i < bytearray.length; i++) {
            sb.append(String.format("%02x", bytearray[i] & 0xff));
            if (i < bytearray.length - 1 && whitespace) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static String createSHA1(File f) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        InputStream fis = new FileInputStream(f);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("SHA1");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return byteArrayToHex(complete.digest(), false);
    }

}
