/*
 * ClassFinder.java
 *
 * Created on November 22, 2006, 12:46 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used mainly to facilitate scanning all the folders of sphinx system, and to filter classes that are
 * Configurable (implements 'edu.cmu.sphinx.util.props.Configurable' in one of the ancestors) and are not of Interface
 * type.
 * <p/>
 * This class is used by ModelBuilder, and contains only static methods
 *
 * @author Ariani
 */
public class ClassFinder {

    private static final String COMMON_SPHINX_PACKAGE = "edu";

    private static final Logger logger = Logger.getLogger("ClassFinder");
    
    /**
     * find a File, open it and return the content as BufferedReader
     *
     * @param filename name of file to be found
     * @return BufferedReader with file content
     */
    public static BufferedReader findFile(String filename) throws
            ClassNotFoundException, FileNotFoundException {
        File file = getStartingDir(filename);
        BufferedReader br = new BufferedReader(new FileReader(file));
        return br;
    }


    /**
     * find a list of folder names that are directly under 'startDir', not recursive
     *
     * @param startDir String name of directory to start the search from
     * @return list of folders under 'startDir'
     * @throws ClassNotFoundException the class we're looking for is not found
     * @throws FileNotFoundException  the folder does not exist
     */
    public static List<String> findFolder(String startDir) throws
            ClassNotFoundException, FileNotFoundException {
        List<String> folderList = new ArrayList<String>();

        File tempDir = getStartingDir(startDir);
        validateDirectory(tempDir);
        File[] myFilesDirs = tempDir.listFiles();

        for (File file : myFilesDirs) {
            if (file.isDirectory()) {
                // if it's directory, add the name to result list
                folderList.add(file.getName());
            }
        }
        return folderList;        // list of String folderName 
    }


    /**
     * find a list of java classes inside 'startDir', that meets the ancestor and fields requirement for the model
     * recursively goes down to the bottom level
     *
     * @param startDir     String name directory to list
     * @param startPackage String preceding package names
     * @param classList    Set of .class files e.g. "nextDir.nextclass", that passes the filter, and will be in the
     *                     model startDir and '.class' extension not included as package name
     * @throws ClassNotFoundException, FileNotFoundException
     */
    public static void findClasses(String startDir, String startPackage, Set<Class<?>> classList)
            throws ClassNotFoundException, FileNotFoundException {
        File tempDir = getStartingDir(startDir);
        validateDirectory(tempDir);

        File[] myFilesDirs = tempDir.listFiles();
        String tempFile = null;

        for (File file : myFilesDirs) {
            // go through each file and directory
            tempFile = file.getName();
            if (file.isDirectory()) {
                // if it's directory, go deeper recursively
                findClasses(startDir + '/' + tempFile, startPackage + '.' + tempFile, classList);
            } else if (tempFile.endsWith(".class")) {
                // removes the .class extension
                // if it's a file, check if it's a java .class file
                logger.log(Level.FINE, "Checking class" + tempFile);
                validateFile(file);
                try {
                    String classname = startPackage + '.' + tempFile.substring(0, tempFile.length() - 6);
                    Class<?> addclass = Class.forName(classname);
                    if (filterClass(addclass)) {
                        // if it's an interface, shouldn't display because interface cannot be instantiated
                        if (!addclass.isInterface() && !Modifier.isAbstract(addclass.getModifiers())) {
                            classList.add(addclass);
                        	logger.log(Level.FINE, "Adding class" + tempFile);
                        }
                    }

                } catch (NoClassDefFoundError e) {
                    System.err.flush();
                    System.err.println("error loading " + tempFile + " in " + startPackage);
                    System.err.flush();
                }
            }
        }
    }


    /**
     * check this class, if one of its ancestor classes or interfaces implements 'edu.cmu.sphinx.util.props.Configurable'
     *
     * @param c class to be examined
     * @return boolean true if the class passes the filter
     */
    private static boolean filterClass(Class<?> c) {

        String interfaceName;
        Class<?> superclass;

        //check the implemented interfaces
        Class<?>[] theInterfaces = c.getInterfaces();

        for (Class<?> cls : theInterfaces) {
            interfaceName = cls.getName();
            logger.log(Level.FINE, "The class " + c.getName() + " has interface " + interfaceName);
            if (interfaceName.equalsIgnoreCase("edu.cmu.sphinx.util.props.Configurable")) {
                return true;
            } else if (interfaceName.startsWith(COMMON_SPHINX_PACKAGE)) {
                //check the implemented interface only if they starts with 'edu'
                //so, java library classes are not traversed
                if (filterClass(cls)) {
                    return true;
                }
            }
        }

        // check the ancestor classes
        superclass = c.getSuperclass();
        return superclass != null && superclass.getName().startsWith(COMMON_SPHINX_PACKAGE) && filterClass(superclass);
    }


    /**
     * find directory
     *
     * @param name directory/file to be found
     * @return File of directory/file if found
     * @throws ClassNotFoundException if the dirname/file name is invalid or not found
     */
    private static File getStartingDir(String name) throws ClassNotFoundException {

        // Get a File object for the directory
        File file = null;
        try {

            URL url = Thread.currentThread().getContextClassLoader().
                    getResource(name.replace('.', '/'));
            file = new File(url.getFile());

        } catch (NullPointerException x) {
            throw new ClassNotFoundException(name + " cannot be found; " +
                    "it does not appear to be a valid file/directory");
        }

        if (file.exists()) {
            return file;
        } else {
            throw new ClassNotFoundException(name + " does not appear to be a valid file/dir");
        }
    }


    /** Directory is valid if it exists, does not represent a file, and can be read. */
    static private void validateDirectory(File aDirectory) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
        }
    }


    /** Class is valid if it exists,  and can be read. */
    static private void validateFile(File aFile) throws FileNotFoundException {
        if (aFile == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aFile.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aFile);
        }
        if (!aFile.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aFile);
        }
    }

} // end of class

