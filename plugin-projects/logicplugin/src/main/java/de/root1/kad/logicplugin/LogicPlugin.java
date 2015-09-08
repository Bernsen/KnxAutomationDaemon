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
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public class LogicPlugin extends Plugin {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final File basedir = new File("."); // apache felix base dir
    private final File scriptsdir = new File(basedir, "scripts");
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private final List<Logic> logicList = new ArrayList<>();
    private final List<SourceContainer> sourceContainerList = new ArrayList<>();

    private final Map<String, List<Logic>> gaLogicMap = new HashMap<>();

    private Knx knx;

    /*
     * Filter for files with .java extension + folders. nothing else.
     */
    FileFilter filter = new FileFilter() {
        @Override
        public boolean accept(File file) {

            return (file.isFile() && file.getName().endsWith(".java")) || file.isDirectory();

        }
    };

    public LogicPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        try {
            logger.info("Starting Plugin {}", getClass().getCanonicalName());

            readKnxProjectData();

            // initial read source files
            sourceContainerList.addAll(getSourceContainers());

            for (SourceContainer sc : sourceContainerList) {

                logger.info("Loading script: " + sc.getCanonicalClassName());

                Logic logic = sc.loadLogic(getClass().getClassLoader());

                logicList.add(logic);
                addToMap(logic);

            }

            knx = new Knx();

            knx.setGlobalGroupAddressListener(new GroupAddressListener() {

                @Override
                public void readRequest(GroupAddressEvent event) {
                    // rigth now, we are only interested write-requests to the bus
                }

                @Override
                public void readResponse(GroupAddressEvent event) {
                    // rigth now, we are only interested write-requests to the bus
                }

                @Override
                public void write(GroupAddressEvent event) {

                    // forward events from KNX to relevant logic
                    String ga = event.getDestination();
                    List<Logic> list = gaLogicMap.get(ga);
                    if (list != null) {
                        for (Logic logic : list) {
                            logger.info("Forwarding {} to {}", event, logic);
                            try {
                                logic.knxEvent(event);
                            } catch (KnxException ex) {
                                log.error("Error passing [{}] to {}", event, logic, ex);
                            }
                        }
                    }
                }
            });
            logger.info("Starting Plugin {} *DONE*", getClass().getCanonicalName());
        } catch (LoadSourceException ex) {
            ex.printStackTrace();
        } 
        catch (KnxException ex) {
            ex.printStackTrace();
        }
    }

    private List<SourceContainer> getSourceContainers() {
        List<SourceContainer> result = new ArrayList<>();

        Stack<File> stack = new Stack<>();
        stack.addAll(Arrays.asList(scriptsdir.listFiles(filter)));

        while (!stack.empty()) {

            File f = stack.pop();

            if (f.isDirectory()) {

                stack.addAll(Arrays.asList(f.listFiles(filter)));

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

    @Override
    public void stop() {
        logger.info("Stopping Bundle {}", getClass().getCanonicalName());
        knx.setGlobalGroupAddressListener(null);
        knx = null;
        logger.info("Stopping Bundle {} *DONE*", getClass().getCanonicalName());
    }

    /**
     *
     * @param lc
     */
    private void addToMap(Logic lc) {
        List<String> interestedGAs = lc.getGroupAddresses();

        for (String interestedGA : interestedGAs) {
            List<Logic> list = gaLogicMap.get(interestedGA);
            if (list == null) {
                list = new ArrayList<>();
                logger.info("Creating new gaLogicMap list for {}", interestedGA);
                gaLogicMap.put(interestedGA, list);
            }

            logger.info("Adding {} to list for {}", lc, interestedGA);
            list.add(lc);
        }

    }

    private void readKnxProjectData() {
        try {
            logger.info("Reading knx project data ...");
            KnxProjReader kpr = new KnxProjReader(new File("./conf/knxproject.knxproj"));

            List<GroupAddress> groupaddressList = kpr.getProjects().get(0).getGroupaddressList();
            Map<String, String> gaMap = new HashMap<>();
            for (GroupAddress ga : groupaddressList) {
                gaMap.put(ga.getName(), ga.getAddress());
            }
            GaProvider.setGaMap(gaMap);
            logger.info("Reading knx project data ... *DONE*");

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JDOMException ex) {
            ex.printStackTrace();
        }
    }

}
