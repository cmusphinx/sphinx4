
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
package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.AudioSource;

import edu.cmu.sphinx.util.BatchFile;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.SequenceInputStream;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * An AudioSource that concatenates a list of audio files as one continuous
 * audio InputStream. An UTTERANCE_START will be placed before
 * the start of the first file, and an UTTERANCE_END after the last file.
 * No UTTERANCE_STARTs or UTTERANCE_ENDs will be placed between them. 
 * Optionally, silence can be added in-between the audio files by setting
 * the property:
 * <pre>edu.cmu.sphinx.frontend.util.ConcatFileAudioSource.silenceFile</pre>
 * to a audio file for silence. By default, no silence is added.
 * Moreover, one can also specify how many files to skip for every file read.
 */
public class ConcatFileAudioSource implements AudioSource {

    /**
     * The prefix for the SphinxProperties in this class.
     */
    public static final String PROP_PREFIX =
        "edu.cmu.sphinx.frontend.util.ConcatFileAudioSource.";

    /**
     * The SphinxProperty that specifies the number of files to skip
     * for every file read.
     */
    public static final String PROP_SKIP = PROP_PREFIX + "skip";

    /**
     * The default value for PROP_SKIP.
     */
    public static final int PROP_SKIP_DEFAULT = 0;

    /**
     * The SphinxProperty that specifies the silence audio file, if any.
     * If this property is null, then no silences are added in between
     * files.
     */
    public static final String PROP_SILENCE_FILE =
        PROP_PREFIX + "silenceFile";

    /**
     * The default value for PROP_SILENCE_FILE.
     */
    public static final String PROP_SILENCE_FILE_DEFAULT = null;

    /**
     * The SphinxProperty that specifies the maximum amount of silence
     * allowed between files. The amount of time is defined as a multiple
     * of the silence file.
     */
    public static final String PROP_MAX_SILENCE = PROP_PREFIX + "maxSilence";

    /**
     * The default value of PROP_MAX_SILENCE.
     */
    public static final int PROP_MAX_SILENCE_DEFAULT = 3;


    private int skip;
    private int maxSilence;
    private int silenceCount;
    private String silenceFileName = null;
    private String nextFile = null;
    private StreamAudioSource streamAudioSource;
    private List referenceList;


    /**
     * Constructs a ConcatFileAudioSource.
     *
     * @param name the name of this ConcatFileAudioSource
     * @param context the context used
     * @param props the SphinxProperties to use
     * @param batchFile the file containing a list of audio files to read from
     */
    public ConcatFileAudioSource(String name, String context,
                                 SphinxProperties props, String batchFile)
        throws IOException {
        maxSilence = props.getInt(PROP_MAX_SILENCE, PROP_MAX_SILENCE_DEFAULT);
        skip = props.getInt(PROP_SKIP, PROP_SKIP_DEFAULT);
        silenceFileName = 
            props.getString(PROP_SILENCE_FILE, PROP_SILENCE_FILE_DEFAULT);
       
        if (batchFile == null) {
            throw new Error("BatchFile cannot be null!");
        }
        streamAudioSource = new StreamAudioSource
            ("StreamAudioSource", context,
             new SequenceInputStream(new InputStreamEnumeration(batchFile)),
             batchFile);
        referenceList = new LinkedList();
    }

    /**
     * Returns the next Audio object.
     * Returns null if all the audio data in all the files have been read.
     *
     * @return the next Audio or <code>null</code> if no more is
     *     available
     *
     * @throws java.io.IOException
     */
    public Audio getAudio() throws IOException {
        return streamAudioSource.getAudio();
    }

    /**
     * Returns a list of all reference text.
     *
     * @return a list of all reference text
     */
    public List getReferences() {
        return referenceList;
    }

    /**
     * The work of the concatenating of the audio files are
     * done here. The idea here is to turn the list of audio files into
     * an Enumeration, and then fed it to a SequenceInputStream, giving
     * the illusion that the audio files are concatenated, but only
     * logically.
     */
    class InputStreamEnumeration implements Enumeration {

        private boolean inSilence;
        private Random silenceRandom;
        private BufferedReader reader;

        InputStreamEnumeration(String batchFile) throws IOException {
            reader = new BufferedReader(new FileReader(batchFile));
            if (silenceFileName != null) {
                inSilence = true;
                silenceRandom = new Random(System.currentTimeMillis());
                silenceCount = silenceRandom.nextInt(maxSilence) + 1;
            }
        }
        
        /**
         * Tests if this enumeration contains more elements.
         *
         * @return true if and only if this enumeration object contains 
         * at least one more element to provide; false otherwise.
         */
        public boolean hasMoreElements() {
            if (nextFile == null) {
                nextFile = readNext();
            }
            return (nextFile != null);
        }
        
        /**
         * Returns the next element of this enumeration if this 
         * enumeration object has at least one more element to provide.
         *
         * @return the next element of this enumeration.
         */
        public Object nextElement() {
            Object stream = null;
            if (nextFile == null) {
                nextFile = readNext();
            }
            if (nextFile != null) {
                try {
                    stream = new FileInputStream(nextFile);
                    // System.out.println(nextFile);
                    nextFile = null;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw new Error("Cannot convert " + nextFile +
                                    " to a FileInputStream");
                }
            }
            return stream;
        }
        
        /**
         * Returns the name of next audio file, taking into account
         * file skipping and the adding of silence.
         *
         * @return the name of the appropriate audio file
         */
        public String readNext() {
            if (!inSilence) {
                try {
                    String next = reader.readLine();
                    if (next != null) {
                        referenceList.add(BatchFile.getReference(next));
                        next = BatchFile.getFilename(next);
                        for (int i = 1; i < skip; i++) {
                            reader.readLine();
                        }
                        if (silenceFileName != null && maxSilence > 0) {
                            silenceCount = 
                                silenceRandom.nextInt(maxSilence) + 1;
                            inSilence = true;
                        }
                    }
                    return next;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw new Error("Problem reading from batch file");
                }
            } else {
                String next = null;
                if (silenceCount > 0) {
                    next = silenceFileName;
                    silenceCount--;
                    if (silenceCount <= 0) {
                        inSilence = false;
                    }
                }
                return next;
            }
        }
    }
}
