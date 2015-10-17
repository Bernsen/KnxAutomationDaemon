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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class Utils {

    public static final Logger log = LoggerFactory.getLogger(Utils.class);
    
    /*
     * Filter for files with .java extension + folders. nothing else.
     */
    public static final FileFilter scriptFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {

            return (file.isFile() && file.getName().endsWith(".java")) || file.isDirectory();

        }
    };
    
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



}
