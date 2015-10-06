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

import de.root1.jrc.CompileResult;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class SourceClassLoader extends ClassLoader {
    
    private Logger log = LoggerFactory.getLogger(getClass());
    private CompileResult compileResult;

    SourceClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public Class<?> loadClass(String classname) throws ClassNotFoundException {
        
        if (compileResult.containsClass(classname)) {
            
            log.debug("Trying to load class [{}] as script from disk", classname);
            DataInputStream dis = null;
            try {
                
                File f = compileResult.getFileFor(classname);
                
                if (f.length()>Integer.MAX_VALUE) {
                    throw new RuntimeException("Classfile "+f.getAbsolutePath()+" exceed Integer.MAX_VALUE length. Cannot load it.");
                }
                byte[] byteCode = new byte[(int)f.length()];
                dis = new DataInputStream(new FileInputStream(f));
                dis.readFully(byteCode);
                return defineClass(classname, byteCode, 0, byteCode.length);
                
            } catch (FileNotFoundException ex) {
                throw new ClassNotFoundException("Classfile not found",ex);
//                return super.loadClass(classname);
            } catch (IOException ex) {
                throw new ClassNotFoundException("Error loading classfile",ex);
            } finally {
                try {
                    dis.close();
                } catch (IOException ex) {
                }
            }
            
        } else {
            return super.loadClass(classname); 
        }
    }

    void setCompileResult(CompileResult compileResult) {
        this.compileResult = compileResult;
    }
    
}
