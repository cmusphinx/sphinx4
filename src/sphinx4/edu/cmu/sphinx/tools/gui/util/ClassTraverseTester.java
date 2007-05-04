/*
 * ClassTraverseTester.java
 *
 * Created on November 22, 2006, 6:06 PM
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

/**
 * This class is used to traverse the sphinx system; used to test if model built is correct
 *
 * @author Ariani
 */
public class ClassTraverseTester {


    /**
     * The main method
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {

            // specify the folder name of package root.
            // the folder must be reachable from one of the classpath
            // the package name will start from the folder specified 
            // and that root folder is excluded in the package name

// this test will test the ClassFinder and see if it scans the classes property
// the classes should be a descendant of 'configurable' and have at least one PROP_ 
//            ArrayList myClasses = new ArrayList();
//            ClassFinder.findClasses("edu/cmu/sphinx/model/acoustic"  ,
//                    "edu.cmu.sphinx.model.acoustic",myClasses);
//            for(Iterator it=myClasses.iterator();it.hasNext();){
//                System.out.println(((Class)it.next()).getName());
//            }

            // this one will test the ModelBuilder, ConfigurableComponent, 
            // and ConfigurableProperty
            ModelBuilder mb = ModelBuilder.getInstance();
            //  mb.refresh("edu/cmu/sphinx/model/acoustic"  ,
            //          "edu.cmu.sphinx.model.acoustic");
            mb.refresh(); // load the model
            mb.printModel(); // print the model out


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
