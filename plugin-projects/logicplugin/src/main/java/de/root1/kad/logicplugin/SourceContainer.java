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

import de.root1.jrc.CompileException;
import de.root1.jrc.CompileResult;
import de.root1.jrc.JavaRuntimeCompiler;
import de.root1.kad.KadMain;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class SourceContainer {

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * e.g. /opt/kad/scripts/de/mypackage/MyClass.java
     */
    private File file;

    /**
     * e.g. /opt/kad/scripts
     */
    private File basedir;

    /**
     * e.g. de.mypackage
     */
    private String packageName;

    /**
     * e.g. MyClass
     */
    private String className;

    private String checksum;

    /**
     * e.g. de/mypackage
     */
    private String packagePath;

    /**
     * e.g. MyClass.java
     */
    private String javaSourceFile;

    /**
     * Site in bytes of the java source file
     */
    private long fileSize;

    public SourceContainer(File basedir, File file) throws IOException {

        // shorten paths in file objects if necessary
        basedir = de.root1.kad.Utils.shortenFile(basedir);
        file = de.root1.kad.Utils.shortenFile(file);

        this.file = file;
        this.basedir = basedir;

        String basedirpath = basedir.getAbsolutePath();
        String filepath = file.getAbsolutePath();

        if (!filepath.startsWith(basedirpath)) {
            throw new IllegalArgumentException("Given basedir is not a parent of file");
        }

        try {
            checksum = Utils.createSHA1(file);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Checksumcreation failed", ex);
        }

        String pathToFile = filepath.substring(basedirpath.length() + 1);

        log.debug("pathToFile={}", pathToFile);

        javaSourceFile = pathToFile;
        if (pathToFile.contains(File.separator)) {
            javaSourceFile = pathToFile.substring(pathToFile.lastIndexOf(File.separator) + 1);
        }

        packagePath = pathToFile.substring(0, pathToFile.lastIndexOf(File.separator));

        log.debug("packagePath={}", packagePath);

        packageName = packagePath.replace(File.separator, ".");
        log.debug("packageName={}", packageName);

        log.debug("javaSourceFile=" + javaSourceFile);

        className = javaSourceFile.substring(0, javaSourceFile.lastIndexOf("."));

        log.debug("className={}", className);
        fileSize = file.length();

    }

    public String getChecksum() {
        return checksum;
    }

    /**
     * e.g. de.mypackage.MyClass
     * @return 
     */
    public String getCanonicalClassName() {
        return packageName + "." + className;
    }

    public Logic loadLogic() throws LoadSourceException {
        try {
            JavaRuntimeCompiler jrc = new JavaRuntimeCompiler();

            ClassLoader cl = getClass().getClassLoader();

            if (cl instanceof URLClassLoader) {
                jrc.setClassPath((URLClassLoader) getClass().getClassLoader());
                jrc.addClassPath((URLClassLoader) KadMain.class.getClassLoader());
            }

            File compiledScriptsFolder = new File("compiledScripts");

            CompileResult compileResult = jrc.compileToFile(getCanonicalClassName(), getFile(), compiledScriptsFolder.getCanonicalFile());

            log.debug("compile result: {}", compileResult);

            SourceClassLoader scl = new SourceClassLoader(this.getClass().getClassLoader());
            scl.setCompileResult(compileResult);

            Class<?> sourceClass = scl.loadClass(getCanonicalClassName());

            if (!Logic.class.isAssignableFrom(sourceClass)) {
                throw new CompileException("Class '" + packageName + "." + className + "' is not of type " + Logic.class.getCanonicalName() + ": " + sourceClass, null, null);
            }

                Logic logic = (Logic) sourceClass.newInstance();
                log.debug("Initialize logic {} ...", getCanonicalClassName());
                logic.init();
                log.debug("Initialize logic {} ... *DONE*", getCanonicalClassName());
                return logic;

        } catch (CompileException | ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
            throw new LoadSourceException(ex);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.file);
        hash = 79 * hash + (int) (this.fileSize ^ (this.fileSize >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SourceContainer other = (SourceContainer) obj;
        if (!Objects.equals(this.file, other.file)) {
            return false;
        }
        if (this.fileSize != other.fileSize) {
            return false;
        }
        return true;
    }

    /**
     * e.g. /opt/kad/scripts/de/mypackage/MyClass.java
     * @return 
     */
    public File getFile() {
        return file;
    }

    /**
     * e.g. /opt/kad/scripts
     * @return 
     */
    public File getBasedir() {
        return basedir;
    }

    /**
     * e.g. de.mypackage
     * @return 
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * e.g. MyClass
     * @return 
     */
    public String getClassName() {
        return className;
    }

    /**
     * e.g. de/mypackage
     * @return 
     */
    public String getPackagePath() {
        return packagePath;
    }

    /**
     * e.g. MyClass.java
     * @return 
     */
    public String getJavaSourceFile() {
        return javaSourceFile;
    }

    public long getFileSize() {
        return fileSize;
    }

}
