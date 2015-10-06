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
package de.root1.kad;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author achristian
 */
public class Utils {
    
    public static String shortenPath(String path) {
        return path.replace(File.separator + "." + File.separator, File.separator);
    }

    public static File shortenFile(File file) throws IOException {
        String absolutePath = file.getCanonicalPath();
        String newPath = absolutePath.replace(File.separator + "." + File.separator, File.separator);
        if (absolutePath.equals(newPath)) {
            return file.getCanonicalFile();
        } else {
            return new File(newPath);
        }
    }
    
}
