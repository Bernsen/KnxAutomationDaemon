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

import de.root1.kad.KadPlugin;
import de.root1.kad.knxservice.KnxService;
import de.root1.kad.knxservice.KnxServiceDataListener;
import de.root1.kad.knxservice.KnxServiceException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 *
 * @author achristian
 */
public class LogicPlugin extends KadPlugin {

    private final File scriptsdir = new File(System.getProperty("kad.basedir"), "scripts");
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private final List<Logic> logicList = new ArrayList<>();
    private final List<SourceContainer> sourceContainerList = new ArrayList<>();

    private final Map<String, List<Logic>> gaLogicMap = new HashMap<>();

    private KnxService knx;

    KnxServiceDataListener listener = new KnxServiceDataListener() {

        @Override
        public void onData(String gaName, String value) {

            // forward events from KNX to relevant logic
            List<Logic> list = gaLogicMap.get(gaName);
            if (list != null) {
                for (Logic logic : list) {
                    log.info("Forwarding value '{}' with DPT '{}' to [{}@{}]'", new Object[]{value, knx.getDPT(gaName), gaName, knx.translateNameToGa(gaName)});
                    try {
                        logic.onData(gaName, value);
                    } catch (KnxServiceException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    };

    public LogicPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        try {
            knx = getService(KnxService.class).get(0);
            knx.registerListener("*", listener);

            log.info("Starting Plugin {}", getClass().getCanonicalName());

            // initial read source files
            sourceContainerList.addAll(Utils.getSourceContainers(scriptsdir));

            for (SourceContainer sc : sourceContainerList) {

                log.info("Loading script: " + sc.getCanonicalClassName());

                try {
                    sc.setKadClassloader(getKadClassLoader());
                    Logic logic = sc.loadLogic();
                    logic.setKnxService(knx);
                    log.info("Initialize logic {} ...", logic.getClass().getCanonicalName());
                    logic.init();
                    logicList.add(logic);
                    addToMap(logic);
                } catch (LogicException ex) {
                    log.error("Error loading script '{}': {}", sc.getPackagePath() + File.separator + sc.getJavaSourceFile(), ex.getMessage());
                }

            }

            log.info("Starting Plugin {} *DONE*", getClass().getCanonicalName());
        } catch (LoadSourceException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void stop() {
        log.info("Stopping Plugin {}", getClass().getCanonicalName());
        knx.unregisterListener("*", listener);
        knx = null;
        log.info("Stopping Plugin {} *DONE*", getClass().getCanonicalName());
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
                log.debug("Creating new gaLogicMap list for {}", interestedGA);
                gaLogicMap.put(interestedGA, list);
            }

            log.debug("Adding {} to list for {}", lc, interestedGA);
            list.add(lc);
        }

    }

}
