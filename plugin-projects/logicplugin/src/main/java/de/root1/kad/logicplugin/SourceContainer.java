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
import de.root1.jrc.JavaRuntimeCompiler;
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
     * e.g. /home/achristian/kle/scripts/de/mypackage/MyClass.java
     */
    private File file;

    /**
     * e.g. /home/achristian/kle/scripts
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
        basedir = shortenFile(basedir);
        file = shortenFile(file);

        this.file = file;
        this.basedir = basedir;

        String basedirpath = basedir.getAbsolutePath();
        String filepath = file.getAbsolutePath();

        if (!filepath.startsWith(basedirpath)) {
            throw new IllegalArgumentException("Given basedir is not a parent of file");
        }

        try {
            createChecksum();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Checksumcreation failed", ex);
        }

        String pathToFile = filepath.substring(basedirpath.length() + 1);

        log.info("pathToFile={}", pathToFile);

        javaSourceFile = pathToFile;
        if (pathToFile.contains(File.separator)) {
            javaSourceFile = pathToFile.substring(pathToFile.lastIndexOf(File.separator) + 1);
        }

        packagePath = pathToFile.substring(0, pathToFile.lastIndexOf(File.separator));

        log.info("packagePath={}", packagePath);

        packageName = packagePath.replace(File.separator, ".");
        log.info("packageName={}", packageName);

        log.info("javaSourceFile=" + javaSourceFile);

        className = javaSourceFile.substring(0, javaSourceFile.lastIndexOf("."));

        log.info("className={}", className);
        fileSize = file.length();

    }

    private static String shortenPath(String path) {
        return path.replace(File.separator + "." + File.separator, File.separator);
    }

    private static File shortenFile(File file) throws IOException {
        String absolutePath = file.getCanonicalPath();
        String newPath = absolutePath.replace(File.separator + "." + File.separator, File.separator);
        if (absolutePath.equals(newPath)) {
            return file.getCanonicalFile();
        } else {
            return new File(newPath);
        }
    }

    private void createChecksum() throws FileNotFoundException, NoSuchAlgorithmException, IOException {

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream fis = new BufferedInputStream(new FileInputStream(file));
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        checksum = Utils.byteArrayToHex(digest.digest(), false);
    }

    public String getChecksum() {
        return checksum;
    }

    public String getCanonicalClassName() {
        return packageName + "." + className;
    }

    public Logic loadLogic(ClassLoader parentCl) throws LoadSourceException {
        try {
            JavaRuntimeCompiler jrc = new JavaRuntimeCompiler();

            ClassLoader cl = getClass().getClassLoader();

            if (cl instanceof URLClassLoader) {
                jrc.setClassPath((URLClassLoader) getClass().getClassLoader());
            }

            File compiledScriptsFolder = new File("compiledScripts");

            File compileToFile = jrc.compileToFile(getCanonicalClassName(), getFile(), compiledScriptsFolder.getCanonicalFile());

            log.info("compiled file: {}", compileToFile.getAbsolutePath());

            SourceClassLoader scl = new SourceClassLoader(this.getClass().getClassLoader());
            scl.setClass(compileToFile, getCanonicalClassName());

            Class<?> sourceClass = scl.loadClass(getCanonicalClassName());

            if (!Logic.class.isAssignableFrom(sourceClass)) {
                throw new CompileException("Class '" + packageName + "." + className + "' is not of type " + Logic.class.getCanonicalName() + ": " + sourceClass, null, null);
            }

            Logic logic = (Logic) sourceClass.newInstance();
            log.info("Initialize logic {} ...", getCanonicalClassName());
            logic.init();
            log.info("Initialize logic {} ... *DONE*", getCanonicalClassName());
            return logic;

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (CompileException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (LogicException ex) {
            ex.printStackTrace();
        }
        throw new LoadSourceException();
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

    public File getFile() {
        return file;
    }

    public File getBasedir() {
        return basedir;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String getJavaSourceFile() {
        return javaSourceFile;
    }

    public long getFileSize() {
        return fileSize;
    }

}
