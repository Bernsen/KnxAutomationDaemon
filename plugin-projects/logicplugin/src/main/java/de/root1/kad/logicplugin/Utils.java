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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author achristian
 */
public class Utils {

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

    public static String createSHA1(File f) throws FileNotFoundException, NoSuchAlgorithmException, IOException  {
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
