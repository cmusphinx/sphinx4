/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package demo.sphinx.helloworld;

import edu.cmu.sphinx.frontend.util.Microphone;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.io.File;
import java.io.IOException;
import java.net.URL;


/**
 * A simple HelloWorld demo showing a simple speech application 
 * built using Sphinx-4.
 */
public class HelloWorld implements Configurable {

    /**
     * The sphinx property for the recognizer to use
     */
    public static final String PROP_RECOGNIZER = "recognizer";

    /**
     * The sphinx property for the microphone to use
     */
    public static final String PROP_MICROPHONE = "microphone";

    
    private String name;
    private Recognizer recognizer;
    private Microphone microphone;


    /**
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        this.name = name;
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_MICROPHONE, PropertyType.COMPONENT);
    }


    /**
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        recognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER,
                                                  Recognizer.class);
        microphone = (Microphone) ps.getComponent(PROP_MICROPHONE,
                                                  Microphone.class);
    }


    /**
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    
    /**
     * Initialize the hello world demo by loading the recognizer.
     */
    public void initialize() {
        try {
            recognizer.allocate();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    /**
     * Runs the hello world demo.
     */
    public void run() {
        if (microphone.startRecording()) {
            System.out.println
                ("Say any digit(s): e.g. \"two oh oh four\", " +
                 "\"three six five\".");
            while (true) {
                System.out.println
                    ("Start speaking. " + 
                     "Press Ctrl-C or say 'good bye' to quit.");
                Result result = recognizer.recognize();
                        
                if (result != null) {
                    String resultText = result.getBestResultNoFiller();
                    System.out.println("You said: " + resultText + "\n");
                    if (resultText.equals("good bye")) {
                        System.exit(0);
                    }
                } else {
                    System.out.println("I can't hear what you said.\n");
                }
            }
        } else {
            System.out.println("Cannot start microphone.");
            System.exit(1);
        }
    }


    /**
     * Main method for running the HelloWorld demo.
     */
    public static void main(String[] args) {
        try {
            URL url;
            if (args.length > 0) {
                url = new File(args[0]).toURI().toURL();
            } else {
                url = HelloWorld.class.getResource("helloworld.config.xml");
            }
            ConfigurationManager cm = new ConfigurationManager(url);
            HelloWorld helloWorld = (HelloWorld) cm.lookup("helloworld");
            helloWorld.initialize();
            helloWorld.run();
        } catch (IOException e) {
            System.err.println("Problem when loading HelloWorld: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring HelloWorld: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating HelloWorld: " + e);
            e.printStackTrace();
        }
    }
}
