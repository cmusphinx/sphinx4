/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Dumps out information about an acoustic model.
 */
public class PropertiesDumper {

    private Properties props;

    /**
     * Dumps the properties file 'model.props' that is in the same
     * directory as this class.
     */
    public static void main(String[] argv) {
        try {
            PropertiesDumper dumper = new PropertiesDumper("model.props");
            System.out.println();
            System.out.println(dumper.toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Constructs a PropertiesDumper of the given acoustic model
     * properties file.
     *
     * @param propsFile the properties file to dump
     */
    public PropertiesDumper(String propsFile) throws IOException {
        props = new Properties();
        props.load(getClass().getResource(propsFile).openStream());
    }

    /**
     * Constructs a PropertiesDumper of the given acoustic model
     * properties.
     *
     * @param properties the Properties object to dump
     */
    public PropertiesDumper(Properties properties) throws IOException {
        props = properties;
    }

    /**
     * Returns a string of the properties.
     *
     * @return a string of the properties
     */
    public String toString() {
        String result = ((String) props.get("description")) + "\n";
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String value = (String) props.get(key);
            result += ("\n\t" + getReadableForm(key) + ": " + value);
        }
        result += "\n";
        return result;
    }

    /**
     * Converts strings like "thisIsAString" into "This Is A String".
     *
     * @param original the original string
     *
     * @return a readable form of strings like "thisIsAString"
     */
    private String getReadableForm(String original) {
        if (original.length() > 0) {
            StringBuffer sb = new StringBuffer(original.length() * 2);
            int i = 0;
            sb.append(Character.toUpperCase(original.charAt(i++)));
            for (; i < original.length(); i++) {
                char c = original.charAt(i);
                if (Character.isUpperCase(c)) {
                    sb.append(" ");
                }
                sb.append(c);
            }
            return sb.toString();
        } else {
            return original;
        }
    }
}


