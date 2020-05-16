package edu.cmu.sphinx.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.FrontEnd;

/**
 * Extracts features from input stream
 * 
 * @author Vladisav Jelisavcic
 * 
 */
class FeatureExtractor {

	private FrontEnd frontEnd;
	private int featureLength = -1;

	/**
	 * Constructs a FeatureExtractor.
	 * 
	 * @param cm
	 *            the configuration manager
	 * @param frontEndName
	 *            the name for the frontEnd to be used
	 * @param inputStream
	 *            data stream
	 * 
	 * @throws IOException
	 *             if error occurred
	 */
	public FeatureExtractor(InputStream inputStream, int sampleRate)
			throws IOException {
		Configuration configuration = new Configuration();

		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
		configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

		Context ctx = new Context(configuration);
		ctx.setSampleRate(sampleRate);
		ctx.setSpeechSource(inputStream);

		frontEnd = (FrontEnd) ctx.getInstance(FrontEnd.class);
	}

	/**
	 * Extracts all features from the supplied InputStream.
	 * 
	 * @return float[][] when called first time, null otherwise
	 * 
	 * @throws DataProcessinException
	 *             if error occurred
	 */
	public List<float[]> getAllFeatures() throws DataProcessingException {
		List<float[]> featureList = new ArrayList<float[]>();

		Data feature = frontEnd.getData();
		if (feature == null)
			return null;

		while (!(feature instanceof DataEndSignal)) {
			if (feature instanceof DoubleData) {
				double[] featureData = ((DoubleData) feature).getValues();
				if (featureLength < 0) {
					featureLength = featureData.length;
				}
				float[] convertedData = new float[featureData.length];
				for (int i = 0; i < featureData.length; i++) {
					convertedData[i] = (float) featureData[i];
				}
				featureList.add(convertedData);
			} else if (feature instanceof FloatData) {
				float[] featureData = ((FloatData) feature).getValues();
				if (featureLength < 0) {
					featureLength = featureData.length;
				}
				featureList.add(featureData);
			}
			feature = frontEnd.getData();
		}

		return featureList;
	}

	/**
	 * Extracts a single feature frame from the supplied InputStream.
	 * 
	 * @return float[] or null if end of stream reached
	 * 
	 * @throws DataProcessinException
	 *             if error occurred
	 */
	public float[] getNextFeatureFrame() throws DataProcessingException {
		Data feature = frontEnd.getData();
		if (feature == null)
			return null;

		while (!(feature instanceof DoubleData || feature instanceof FloatData)) {
			feature = frontEnd.getData();
			if (feature == null)
				return null;
		}

		if (feature instanceof DoubleData) {
			double[] featureData = ((DoubleData) feature).getValues();
			if (featureLength < 0) {
				featureLength = featureData.length;
			}
			float[] convertedData = new float[featureData.length];
			for (int i = 0; i < featureData.length; i++) {
				convertedData[i] = (float) featureData[i];
			}

			return convertedData;
		} else if (feature instanceof FloatData) {
			float[] featureData = ((FloatData) feature).getValues();
			if (featureLength < 0) {
				featureLength = featureData.length;
			}

			return featureData;
		}

		return null;
	}

}

