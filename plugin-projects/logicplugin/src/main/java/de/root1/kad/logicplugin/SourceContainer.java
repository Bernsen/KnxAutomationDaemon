/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This logicFile is part of KAD Logic Plugin (KLP).
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
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
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
    private File logicFile;

    /**
     * e.g. /opt/kad/scripts
     */
    private File srcDir;

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
     * Site in bytes of the java source logicFile
     */
    private long fileSize;
    private ClassLoader kadClassloader;
    private final File libDir;

    public SourceContainer(File srcDir, File libDir, File logicFile) throws IOException {

        // shorten paths in logicFile objects if necessary
        srcDir = de.root1.kad.Utils.shortenFile(srcDir);
        libDir = de.root1.kad.Utils.shortenFile(libDir);
        logicFile = de.root1.kad.Utils.shortenFile(logicFile);

        this.logicFile = logicFile;
        this.srcDir = srcDir;
        this.libDir = libDir;

        String basedirpath = srcDir.getAbsolutePath();
        String filepath = logicFile.getAbsolutePath();

        if (!filepath.startsWith(basedirpath)) {
            throw new IllegalArgumentException("Given basedir is not a parent of file");
        }

        try {
            checksum = de.root1.kad.Utils.createSHA1(logicFile);
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
        fileSize = logicFile.length();

    }

    public String getChecksum() {
        return checksum;
    }

    /**
     * e.g. de.mypackage.MyClass
     *
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
                jrc.addClassPath((URLClassLoader) kadClassloader);
            }
            
            jrc.addClassPath(srcDir);
            jrc.addClassPath(libDir);

            File compiledScriptsFolder = new File(System.getProperty("kad.basedir"), "compiledLogic");

            CompileResult compileResult = jrc.compileToFile(getCanonicalClassName(), getFile(), compiledScriptsFolder.getCanonicalFile());

            log.debug("compile result: {}", compileResult);

            SourceClassLoader scl = new SourceClassLoader(this.getClass().getClassLoader(), libDir);
            scl.setCompileResult(compileResult);

            Class<?> sourceClass = scl.loadClass(getCanonicalClassName());

            if (!Logic.class.isAssignableFrom(sourceClass)) {
                throw new CompileException("Class '" + packageName + "." + className + "' is not of type " + Logic.class.getCanonicalName() + ": " + sourceClass, null, null);
            }

            Logic logic = (Logic) sourceClass.newInstance();
            return logic;

        } catch (CompileException | ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
            throw new LoadSourceException(ex);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.logicFile);
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
        if (!Objects.equals(this.logicFile, other.logicFile)) {
            return false;
        }
        return this.fileSize == other.fileSize;
    }

    /**
     * e.g. /opt/kad/scripts/de/mypackage/MyClass.java
     *
     * @return
     */
    public File getFile() {
        return logicFile;
    }

    /**
     * e.g. /opt/kad/logic/src
     *
     * @return
     */
    public File getSrcDir() {
        return srcDir;
    }

    /**
     * e.g. /opt/kad/logic/lib
     * @return 
     */
    public File getLibDir() {
        return libDir;
    }
    
    /**
     * e.g. de.mypackage
     *
     * @return
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * e.g. MyClass
     *
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * e.g. de/mypackage
     *
     * @return
     */
    public String getPackagePath() {
        return packagePath;
    }

    /**
     * e.g. MyClass.java as String
     *
     * @return
     */
    public String getJavaSourceFile() {
        return javaSourceFile;
    }

    public long getFileSize() {
        return fileSize;
    }

    void setKadClassloader(ClassLoader kadClassLoader) {
        this.kadClassloader = kadClassLoader;
    }

    @Override
    public String toString() {
        return "SourceContainer("+getCanonicalClassName()+")";
    }
    
    

}
