/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package tests.frontend;

import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.BatchFileAudioSource;
import edu.cmu.sphinx.frontend.StreamAudioSource;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;


/**
 * Test module for a FrontEnd processor.
 */
public class ProcessorTest {

    AudioSource audioSource;
    boolean batchMode;
    boolean dumpTimes;
    boolean dumpValues;
    String context;

    /**
     * Constructs a ProcessorTest, with the given test name, propertiesFile,
     * and audioSourceFile. Whether this test is in batch mode is
     * determined by the Sphinx property:
     *
     * <pre>tests.frontend.<testName>.batchMode</pre>
     *
     * If batch mode is used, then the parameter "audioSourceFile" refers
     * to the file contain the list of files to test.
     *
     * If it is not in batch mode, then the paramter "audioSourceFile"
     * refers to the audio file to test.
     *
     * @param testName name of the test
     * @param propertiesFile the SphinxProperties file
     * @param audioSourceFile if in batch mode, this refers to the batch file
     *    containing the list of files to test, if not in batch mode, this
     *    refers to the audio file to test
     */
    public ProcessorTest
    (String testName, String propertiesFile, String audioSourceFile) throws
    Exception {
                             
        context = testName;
        
        batchMode = Boolean.getBoolean
            ("tests.frontend." + testName + ".batchMode");
        dumpTimes = Boolean.getBoolean
            ("tests.frontend." + testName + ".dumpTimes");
        dumpValues = Boolean.getBoolean
            ("tests.frontend." + testName + ".dumpValues");

        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (testName, new URL
             ("file://" + pwd + File.separatorChar + propertiesFile));
                
        if (batchMode) {
            audioSource = 
                (new BatchFileAudioSource
                 ("BatchFileAudioSource", context, audioSourceFile));
        } else {
            audioSource =
                (new StreamAudioSource
                 ("StreamAudioSource", context,
                  (new FileInputStream(audioSourceFile)), audioSourceFile));
        }
    }


    /**
     * Returns the AudioSource of this test.
     *
     * @return the AudioSource of this test
     */
    public AudioSource getAudioSource() {
        return audioSource;
    }


    /**
     * Returns the SphinxProperties used.
     *
     * @return the SphinxProperties
     */
    public SphinxProperties getSphinxProperties() {
	return SphinxProperties.getSphinxProperties(context);
    }


    /**
     * Returns true if this test will dump results.
     *
     * @return true if this test will dump results
     */
    public boolean getDump() {
        return dumpValues;
    }


    /**
     * Returns true if this test will dump the timing data.
     *
     * @return true if this test will dump the timing data
     */
    public boolean getDumpTimes() {
        return dumpTimes;
    }
}
