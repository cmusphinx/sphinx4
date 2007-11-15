/*
 * DocletHelper.java
 *
 * Created on February 5, 2007, 2:55 PM
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
 * This class is used as the Javadoc Doclet handler that customize the javadoc output of the classes
 *
 * @author Ariani
 */
class DocletHelper {

}

// commented because not compilable.
//public class DocletHelper extends Doclet {
//
//    /** the start of javadoc */
//    public static boolean start(RootDoc root) {
//
//        ClassDoc[] classes = root.classes();
//        for (int i = 0; i < classes.length; ++i) {
//            ClassDoc cd = classes[i];
//            printMembers(cd.fields(true)); // only interested in the fields
//        }
//        return true;
//    }
//
//
//    /**
//     * this helper function describe what should be written for each class
//     * @param mems array of fields that belong to this class
//     */
//    static void printMembers(FieldDoc[] mems) {
//        for (int i = 0; i < mems.length; ++i) {
//            if ( mems[i].isPublic() && mems[i].isStatic() ){
//                /* the javadoc output expected would be :
//                 * ===[property_name]***[property_comment]
//                 * ===[\n]
//                 */
//
//                System.out.print("===" +mems[i].name() ); // property name
//                System.out.println("=="+mems[i].commentText()); // comment
//            }
//        }
//    }
//
//}
