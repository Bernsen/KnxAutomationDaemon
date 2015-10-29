/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of KnxAutomationDaemon (KAD).
 *
 *   KAD is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   KAD is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KAD.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.kad.knxservice;

import java.io.Serializable;
import java.util.Date;

class CacheEntry implements Serializable {

        private final String address;
        private String value;
        private long lastUpdate;

        public CacheEntry(String address, String value) {
            this.address = address;
            this.lastUpdate = System.currentTimeMillis();
            this.value = value;
        }

        public String getAddress() {
            return address;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "CacheEntry{" + "address=" + address + ", value=" + value + ", lastUpdate=" + new Date(lastUpdate) + "}";
        }

        void setValue(String value) {
            this.value = value;
            lastUpdate = System.currentTimeMillis();
        }

    }