/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package tests.other;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.net.URL;

/**
 * Tests the SphinxProperties class.
 */
public class SphinxPropertiesTest {

    /**
     * A test program. args[0] is the SphinxProperties file for testing.
     */
    public static void main(String[] args) {
        String testFile = args[0];
        if (testFile == null) {
            System.out.println
                ("Usage: java SphinxProperties <propertiesFile>");
            System.exit(1);
        }
	try {
	    // an empty context
	    URL emptyURL = null;
	    SphinxProperties.initContext("sun", emptyURL);
	    // a populated context
	    SphinxProperties.initContext
                ("moon", new URL("file:./" + testFile));
    	} catch (IOException ioe) {
	    System.out.println("ioe " + ioe);
	}

	SphinxProperties sun = SphinxProperties.getSphinxProperties("sun");
	SphinxProperties moon = SphinxProperties.getSphinxProperties("moon");
	SphinxProperties star = SphinxProperties.getSphinxProperties("star");

	System.out.println("sun flare " +sun.getString("flare", "high"));
        assert sun.getString("flare", "high").equals("high");

	System.out.println("moon flare " + moon.getString("flare", "high"));
        assert moon.getString("flare", "high").equals("low");
        
	System.out.println("moon grav " + moon.getFloat("gravity", 9.8f));
        assert moon.getFloat("gravity", 9.8f) == 1.2f;
	
        System.out.println("moon lgrav " + moon.getDouble("lgravity", 19.8));
        assert moon.getDouble("lgravity", 19.8) == 1.9;
	
        System.out.println("moon int " + moon.getInt("craters", 5000));
        assert moon.getInt("craters", 5000) == 1000000;
	
        System.out.println("moon bool " + moon.getBoolean("tracing", false));
        assert moon.getBoolean("tracing", false) == true;

	System.out.println("io flare " + moon.getString("io.flare", "high"));
        assert moon.getString("io.flare", "high").equals("none at all");

	System.out.println("io grav " + moon.getFloat("io.gravity", 9.8f));
        assert moon.getFloat("io.gravity", 9.8f) == 99.99f;

	System.out.println("io lgrav " + moon.getDouble("io.lgravity", 19.8));
        assert moon.getDouble("io.lgravity", 19.8) == 999.999;

	System.out.println("io int " + moon.getInt("io.craters", 5000));
        assert moon.getInt("io.craters", 5000) == 1;

	System.out.println("io bool " + moon.getBoolean("io.tracing", false));
        assert moon.getBoolean("io.tracing", false) == false;

	System.out.println("p flare " + 
                           moon.getString("phobus", "flare", "high"));
        assert moon.getString("phobus", "flare", "high").equals("really high");

	System.out.println("p grav " + 
                           moon.getFloat("phobus", "gravity", 9.8f));
        assert moon.getFloat("phobus", "gravity", 9.8f) == 0.001f;
        
	System.out.println("p lgrav " + 
                           moon.getDouble("phobus", "lgravity", 19.8));
        assert moon.getDouble("phobus", "lgravity", 19.8) == 0.00001;

	System.out.println("p int " + moon.getInt("phobus", "craters", 5000));
        assert moon.getInt("phobus", "craters", 5000) == 14;

	System.out.println("p bool " +
                           moon.getBoolean("phobus", "tracing", false));
        assert moon.getBoolean("phobus", "tracing", false) == true;
        
        // instance tests
        System.out.println("ra string " +
                           moon.getString("ra", "flare", "medium"));
        assert moon.getString("ra", "flare", "medium").equals("low");

        System.out.println("ra float " +
                           moon.getFloat("ra", "gravity", 10.f));
        assert moon.getFloat("ra", "gravity", 10.f) == 1.2f;

        System.out.println("ra double " +
                           moon.getDouble("ra", "lgravity", 1000));
        assert moon.getDouble("ra", "lgravity", 1000) == 1.9;

	System.out.println("ra int " + moon.getInt("ra", "craters", 5000));
        assert moon.getInt("ra", "craters", 5000) == 1000000;

        System.out.println("ra bool " + 
                           moon.getBoolean("ra", "tracing", false));
        assert moon.getBoolean("ra", "tracing", false) == true;

        System.out.println("All SphinxProperties tests PASSED.");
    }
}
