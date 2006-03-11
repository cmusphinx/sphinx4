package edu.cmu.sphinx.tools.corpus;

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
 * Date: Mar 11, 2006
 * Time: 10:23:08 AM
 */
public class PCMFileAudioDatabase extends AudioDatabase {

    public byte[] readPcmAsBytes(int beginTime, int endTime) {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public short[] readPcmAsShorts(int beginTime, int endTime) {
        return new short[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double [] readPitch(int beginTime, int endTime) {
        return new double[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double [] readEnergy(int beginTime, int endTime) {
        return new double[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
