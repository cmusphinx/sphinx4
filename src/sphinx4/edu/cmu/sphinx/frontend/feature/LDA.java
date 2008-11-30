/*
 * Copyright 2008 Nickolay V. Shmyrev <nshmyrev@yandex.ru>
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.feature;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.model.acoustic.*;
import edu.cmu.sphinx.util.props.*;

import java.util.*;

/**
 * Implements an LDA transformation. The dimension of the feature stream is
 * reduced with a matrix transform thus reducing speed and increasing accuracy.
 * This component requires special model trained with LDA/MLLT transform.
 */
public class LDA extends BaseDataProcessor {

	/** The name of the transform matrix file */
	@S4Component(type = Loader.class)
	public final static String PROP_LOADER = "loader";

	float[][] transformMatrix;
	protected Loader loader;
	
	int rows;
	int values;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
	 * .props.PropertySheet)
	 */
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		
		loader = (Loader)ps.getComponent(PROP_LOADER);
		
		try {
			loader.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		transformMatrix = loader.getTransformMatrix();
		
		rows = transformMatrix.length;
		values = transformMatrix[0].length;
	}

	/**
	 * Returns the next Data object being processed by this LDA, or if it is a
	 * Signal, it is returned without modification.
	 * 
	 * @return the next available Data object, returns null if no Data object is
	 *         available
	 * @throws DataProcessingException
	 *             if there is a processing error
	 * @see Data
	 */
	public Data getData() throws DataProcessingException {
		Data input = getPredecessor().getData();
		Data output;
		getTimer().start();
		if (input != null && input instanceof FloatData) {
			FloatData inputData = (FloatData) input;
			float[] in = inputData.getValues();
			float[] out = new float[rows];
			
			assert in.length == values; 

			for (int i = 0; i < rows; i++) {
				out [i] = 0;
				String res = "";
				for (int j = 0; j < values; j++) {
					out[i] += in[j] * transformMatrix[i][j];
					res = res + in[j] + " on " + transformMatrix[i][j] + " ";
				}
				System.out.println (res + "Result " + out[i]);
			}
			output = new FloatData(out, inputData.getSampleRate(), inputData
					.getCollectTime(), inputData.getFirstSampleNumber());
		} else {
			output = input;
		}
		getTimer().stop();
		return output;
	}
}
