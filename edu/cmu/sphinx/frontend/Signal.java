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


package edu.cmu.sphinx.frontend;

/**
 * Indicates events like beginning or end of data, data dropped,
 * quality changed, etc.. It implements the Data interface, and it will
 * pass between DataProcessors to inform them about the Data that is
 * passed between DataProcessors.
 *
 * @see Data
 * @see DataProcessor
 */
public class Signal implements Data {

    private long time;  // the time this Signal was issued

    /**
     * Constructs a Signal with the given name.
     *
     * @param time the time this Signal is created
     */
    protected Signal(long time) {
        this.time = time;
    }

    /**
     * Returns the time this Signal was created.
     *
     * @return the time this Signal was created
     */
    public long getTime() {
        return time;
    }
}
