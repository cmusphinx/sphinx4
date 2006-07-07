
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


package demo.jsapi.cardmatch;

/**
 * A recording device.
 */
public interface Recorder {

    /**
     * Starts recording.
     *
     * @return <code>true</code> if the recording was started properly
     */
    public boolean startRecording();

    /**
     * Stops recording.
     *
     * @return <code>true</code> if the recording was started properly
     */
    public boolean stopRecording();

    /**
     * Returns true if this Recorder is recording.
     *
     * @return <code>true</code> if this Recorder is recording
     */
    public boolean isRecording();
}
