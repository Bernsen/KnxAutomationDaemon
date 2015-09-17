/*
 * Copyright 2015 Alexander Christian
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with
 * the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.root1.kad.pf4j.util;

import ro.fortsoft.pf4j.util.ExtensionFileFilter;

/**
 * File filter that accepts all files ending with .JAR.
 * This filter is case insensitive.
 *
 * @author Alexander Christian
 */
public class JarFileFilter extends ExtensionFileFilter {

    /**
     * The extension that this filter will search for.
     */
    private static final String JAR_EXTENSION = "-kadplugin.JAR";

    public JarFileFilter() {
        super(JAR_EXTENSION);
    }

}
