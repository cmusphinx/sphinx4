package edu.cmu.sphinx.tools.corpus;

import edu.cmu.sphinx.tools.corpus.AudioDatabase;
import edu.cmu.sphinx.tools.corpus.RegionOfAudioData;

/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf
 * Date: Mar 9, 2006
 * Time: 9:30:52 PM
 */
public class AudioRecorder {

    public void open( String fileName ) {

    }

    public AudioDatabase close() {
        return null;
    }

    public double getRmsDB() {
        return 0;
    }

    public double getAverageDB() {
        return 0;
    }

    public double getPeakDB() {
        return 0;
    }

    public boolean isStarted() {
        return false;
    }

    public void start() {

    }
    public RegionOfAudioData stop() {
        return null;
    }


}
