/**
 * 
 */
package edu.cmu.sphinx.api;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Vladisav Jelisavcic
 *
 */
public class FeatureExtractorTest {

	static final String FRONTEND_NAME = "cepstraFrontEnd";
	private float[] features;
	final int sampleRate = 16000;

	@BeforeTest
	public void setUp() {
		InputStream asciiStream = FeatureExtractorTest.class
				.getResourceAsStream("/edu/cmu/sphinx/api/10001-90210-01803.features");
		Scanner sc = new Scanner(asciiStream);
		int numDataPoints = sc.nextInt();
		System.out.println(numDataPoints);

		features = new float[numDataPoints];
		int i = 0;
		while (sc.hasNextFloat()) {
			features[i++] = sc.nextFloat();
		}
		sc.close();
	}

	@AfterTest
	public void tearDown() {

	}

	/**
	 * Test method for
	 * {@link edu.cmu.sphinx.api.FeatureExtractor#getAllFeatures()}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetAllFeatures() throws IOException {
		InputStream audioStream = FeatureExtractorTest.class
				.getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");

		FeatureExtractor fe = new FeatureExtractor(audioStream, sampleRate);
		List<float[]> data = fe.getAllFeatures();

		int numSamples = data.size();
		int numFeatures = data.get(0).length;

		int numDataPoints = numSamples * numFeatures;

		assertEquals(features.length, numDataPoints); // check if all data
														// points are loaded
		for (int i = 0; i < numSamples; i++) {
			for (int j = 0; j < numFeatures; j++) {
				assertEquals(features[i * numFeatures + j], data.get(i)[j]);
			}
		}
	}

	/**
	 * Test method for
	 * {@link edu.cmu.sphinx.api.FeatureExtractor#getNextFeatureFrame()}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetNextFeatureFrame() throws IOException {
		InputStream audioStream = FeatureExtractorTest.class
				.getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");

		FeatureExtractor fe = new FeatureExtractor(audioStream, sampleRate);

		int numDataPoints = 0;
		float[] data;
		while ((data = fe.getNextFeatureFrame()) != null) {
			for (int i = 0; i < data.length; i++) {
				assertEquals(features[i + numDataPoints], data[i]);
			}
			numDataPoints += data.length;
		}

		assertEquals(features.length, numDataPoints); // check if all data points are loaded
	}

}
