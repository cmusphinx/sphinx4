/*
 * Copyright 2008 Nickolay V. Shmyrev <nshmyrev@yandex.ru>
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.filter;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.*;

import java.util.*;

/**
 * Implements a dither for the incoming packet. The small random noise is added
 * to the signal to avoid floating point errors and prevent the energy from
 * being zero. <p/> Other {@link Data}objects are passed along unchanged through
 * this Dither processor. <p/>
 */
public class Dither extends BaseDataProcessor {

	Random random;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
	 * .props.PropertySheet)
	 */
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		random = new Random(0);
	}

	/**
	 * Returns the next Data object being processed by this Dither, or if it is
	 * a Signal, it is returned without modification.
	 * 
	 * @return the next available Data object, returns null if no Data object is
	 *         available
	 * @throws DataProcessingException
	 *             if there is a processing error
	 * @see Data
	 */
	public Data getData() throws DataProcessingException {
		Data input = getPredecessor().getData();
		getTimer().start();
		if (input != null && input instanceof DoubleData) {
			applyDither(((DoubleData) input).getValues());
		}
		getTimer().stop();
		return input;
	}

	/**
	 * Applies dither to the given Audio. The preemphasis is applied in place.
	 * 
	 * @param in
	 *            audio data
	 */
	private void applyDither(double[] in) {

		for (int i = 0; i < in.length; i++) {
			if (random.nextBoolean()) {
				in[i] = in[i] + 1;
			}
		}
	}
}
