/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.taglet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

import com.sun.tools.doclets.Taglet;
import com.sun.tools.doclets.DirectoryManager;

//import com.sun.tools.doclets.standard.*;
//import com.sun.tools.doclets.standard.tags.*;

import java.util.Map;

public class HrefTaglet implements Taglet {

    private static final String NAME = "href.tag";

    /**
     * Register this taglet.
     */
    public static void register(Map tagletMap) {
       HrefTaglet tag = new HrefTaglet();
       Taglet t = (Taglet) tagletMap.get(tag.getName());
       if (t != null) {
           tagletMap.remove(tag.getName());
       }
       tagletMap.put(tag.getName(), tag);
    }
    
    /**
     * Return the name of this custom tag.
     */
    public String getName() {
        return NAME;
    }
    
    /**
     * @return false since this is an inline tag.
     */
    public boolean inField() {
        return false;
    }

    /**
     * @return false since this is an inline tag.
     */
    public boolean inConstructor() {
        return false;
    }
    
    /**
     * @return false since this is an inline tag.
     */
    public boolean inMethod() {
        return false;
    }
    
    /**
     * @return false since this is an inline tag.
     */
    public boolean inOverview() {
        return false;
    }

    /**
     * @return false since this is an inline tag.
     */
    public boolean inPackage() {
        return false;
    }

    /**
     * @return false since this is an inline tag.
     */
    public boolean inType() {
        return false;
    }
    
    /**
     * @return true since this is an inline tag.
     */
    
    public boolean isInlineTag() {
        return true;
    }
    
    /**
     * Returns null to force the taglet infrastructure to call
     * the more sophisticated toString method below.  FIXME: this
     * seems to be broken.
     */
    public String toString(Tag tag) {
        if (tag.holder() instanceof RootDoc) {
            RootDoc doc = (RootDoc) tag.holder();
            ClassDoc classDoc = doc.classNamed(tag.text());
            if (classDoc != null) {
                try {
                    return "href=\"" +
                        DirectoryManager.getPathToPackage(
                            classDoc.containingPackage(),
                            classDoc.name())
                        + ".html\"";
                } catch (Exception e) {
                    System.err.println(
                        NAME + " tag text does not refer to a valid classref: "
                        + tag.text());
                }
            }
        }
        return "";
    }

    /**
     */
    public String toString(Tag[] tags) {
        return null;
    }

    public static void main(String[] args) {
        System.out.println(
            "When run with the javadoc tool, the HrefTaglet\n"
            + "will replace inline references such as\n\n"
            + "  {@" + NAME + " com.acme.babblefish.BabbleFish}\n\n"
            + "with a string such as\n\n"
            + "  com/acme/babblefish/BabbleFish.html");
    }
}
