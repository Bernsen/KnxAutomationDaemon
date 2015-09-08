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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author achristian
 */
public class SourceClassLoader extends ClassLoader {
    private File f;
    private String className;

    SourceClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    void setClass(File f, String className) {
        this.f = f;
        this.className = className;
    }
    
    @Override
    public Class<?> loadClass(String string) throws ClassNotFoundException {
        
        if (className.equals(string)) {
            
            DataInputStream dis = null;
            try {
                
                if (f.length()>Integer.MAX_VALUE) {
                    throw new RuntimeException("Classfile "+f.getAbsolutePath()+" exceed Integer.MAX_VALUE length. Cannot load it.");
                }
                byte[] byteCode = new byte[(int)f.length()];
                dis = new DataInputStream(new FileInputStream(f));
                dis.readFully(byteCode);
                return defineClass(className, byteCode, 0, byteCode.length);
                
            } catch (FileNotFoundException ex) {
                throw new ClassNotFoundException("Classfile not found",ex);
            } catch (IOException ex) {
                throw new ClassNotFoundException("Error loading classfile",ex);
            } finally {
                try {
                    dis.close();
                } catch (IOException ex) {
                }
            }
            
        } else {
            return super.loadClass(string); 
        }
    }
    
}
