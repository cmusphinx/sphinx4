/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.taglet;

import com.sun.javadoc.*;

import com.sun.tools.doclets.*;
import com.sun.tools.doclets.standard.*;
import com.sun.tools.doclets.standard.tags.*;

import java.util.*;

public class HrefTaglet extends AbstractInlineTaglet {

    private static final String NAME = "href";
    private String name;

    public HrefTaglet() {
        name = NAME;
    }

    private String getName() {
        return name;
    }

    public static void register(Map tagletMap) {
       HrefTaglet tag = new HrefTaglet();
       Taglet t = (Taglet) tagletMap.get(tag.getName());
       if (t != null) {
           tagletMap.remove(tag.getName());
       }
       tagletMap.put(tag.getName(), tag);
    }


    public String toString(Tag tag, Doc doc, HtmlStandardWriter writer) {
        StringBuffer result = new StringBuffer("href=\""+
            writer.relativepath);
        ClassDoc classDoc = writer.configuration().root.classNamed(tag.text());
        if (classDoc == null) {
            //Handle bad link here.
        }
        try {
            result.append(
                DirectoryManager.getPathToPackage(
                    classDoc.containingPackage(),
                    classDoc.name() + ".html"));
            result.append("\"");
            return result.toString();
        } catch (Exception e) {
            System.err.println(
                "@href tag text does not refer to a valid classref: "
                + tag.text());
            return "";
        }
    }

    public static void main(String[] args) {
        System.out.println(
            "When run with the javadoc tool, the HrefTaglet\n"
            + "will replace inline references such as\n\n"
            + "  {@href com.acme.babblefish.BabbleFish}\n\n"
            + "with a string such as\n\n"
            + "  com/acme/babblefish/BabbleFish.html\n\n");
    }
}
