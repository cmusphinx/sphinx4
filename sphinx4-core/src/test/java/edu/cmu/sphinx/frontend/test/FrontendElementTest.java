package edu.cmu.sphinx.frontend.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.Test;

import junit.framework.Assert;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class FrontendElementTest {
		
	public void runTest(String frontendName, String referenceFileName) throws IOException {

		ConfigurationManager cm = new ConfigurationManager("src/test/edu/cmu/sphinx/frontend/test/data/frontend.xml");
        AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
        dataSource.setAudioFile(new URL("file:src/test/edu/cmu/sphinx/frontend/test/data/test-feat.wav"), null);
        FrontEnd frontend = (FrontEnd)cm.lookup(frontendName);       

		compareDump(frontend, referenceFileName);
	}

	private void compareDump(FrontEnd frontend, String referenceFileName) throws FileNotFoundException,
			IOException {
		
		String line;
		FileInputStream fis = new FileInputStream(referenceFileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		while ((line = br.readLine()) != null) {
		    Data data = frontend.getData();
		    if (line.startsWith("DataStartSignal")) {
		    	Assert.assertTrue(data instanceof DataStartSignal);
		    }
		    if (line.startsWith("DataEndSignal")) {
		    	Assert.assertTrue(data instanceof DataEndSignal);
		    }
		    if (line.startsWith("SpeechStartSignal")) {
		    	Assert.assertTrue(data instanceof SpeechStartSignal);
		    }
		    if (line.startsWith("SpeechEndSignal")) {
		    	Assert.assertTrue(data instanceof SpeechEndSignal);
		    }
		    if (line.startsWith("Frame")) {
		    	String[] tokens = line.split(" ");
		    	Assert.assertTrue(data instanceof DoubleData);
		    	double[] values = ((DoubleData)data).getValues();
		    	Assert.assertEquals(values.length, (int)Integer.valueOf(tokens[1]));
		    	for (int i = 0; i < values.length; i++) {
		    		Assert.assertEquals(values[i], (double)Double.valueOf(tokens[2 + i]), Math.abs(0.001 * values[i]));
		    	}
		    }
		    if (line.startsWith("FloatFrame")) {
		    	String[] tokens = line.split(" ");
		    	Assert.assertTrue(data instanceof FloatData);
		    	float[] values = ((FloatData)data).getValues();
		    	Assert.assertEquals(values.length, (int)Integer.valueOf(tokens[1]));
		    	for (int i = 0; i < values.length; i++) {
		    		Assert.assertEquals(values[i], (double)Float.valueOf(tokens[2 + i]), Math.abs(0.001 * values[i]));
		    	}
		    }
		}
		fis.close();
	}
	
	@Test
	public void testPreemp() throws IOException {
		runTest("preempTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-preemp.dump");
	}

	@Test
	public void testWindow() throws IOException {
		runTest("windowTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-window.dump");
	}
	@Test
	public void testFFT() throws IOException {
		runTest("fftTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-fft.dump");
	}
	@Test
	public void testMel() throws IOException {
		runTest("melTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-mel.dump");
	}
	@Test
	public void testDct() throws IOException {
		runTest("dctTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-dct.dump");
	}
	@Test
	public void testCMN() throws IOException {
		runTest("cmnTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-cmn.dump");
	}
	@Test
	public void testFeature() throws IOException {
		runTest("feTest", "src/test/edu/cmu/sphinx/frontend/test/data/after-feature.dump");
	}
}
