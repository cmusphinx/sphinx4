/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.CepstralMeanNormalizer;
import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumFrame;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.FeatureExtractor;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.SegmentEndPointSignal;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;


/**
 * Test program for the CepstralMeanNormalizer and the FeatureExtractor.
 * Reads in a file of CepstrumFrames, and test the process() method of
 * both processors on each Frame.
 */
public class CmnFeatureExtractorTest {

    private static final String CEPSTRUM_FRAME = "CEPSTRUM_PRODUCER";
    private static final String CEPSTRUM = "CEPSTRUM";

    private BufferedReader reader;
    private CepstralMeanNormalizer cmn;
    private FeatureExtractor featureExtractor;


    /**
     * Constructs a CmnFeatureExtractorTest with the given cepstra
     * input file.
     *
     * @param cepstrumFile a cepstra input file
     */
    public CmnFeatureExtractorTest(String cepstrumFile) throws IOException {
	this.reader = new BufferedReader(new FileReader(cepstrumFile));
	cmn = new CepstralMeanNormalizer();
	featureExtractor = new FeatureExtractor();
    }


    /**
     * Runs the CmnFeatureExtractorTest.
     */
    public void run() {
	Data cepstrumFrame = readCepstrumFrame();
	Data nextFrame;
	boolean start = true;

	do {
	    if (cepstrumFrame != null) {

		nextFrame = readCepstrumFrame();

		if (start) { // start of segment
		    cepstrumFrame =
			SegmentEndPointSignal.createSegmentStartSignal
			(cepstrumFrame);
		    start = false;
		} else if (nextFrame == null) { // end of segment
		    cepstrumFrame =
			SegmentEndPointSignal.createSegmentEndSignal
			(cepstrumFrame);
		}

		FeatureFrame featureFrame = null;
		Data result = featureExtractor.process
		    (cmn.process(cepstrumFrame));
		if (result instanceof SegmentEndPointSignal) {
		    featureFrame = (FeatureFrame)
			((SegmentEndPointSignal) result).getData();
		} else {
		    featureFrame = (FeatureFrame) result;
		}

		Util.dumpFeatureFrame(featureFrame);
		
		cepstrumFrame = nextFrame;
	    }
	} while (cepstrumFrame != null);
    }


    /**
     * Returns the next available cepstrum frame, or null if no more
     * cepstrum frames.
     *
     * @returns the next available cepstrum frame, or null
     */ 
    private CepstrumFrame readCepstrumFrame() {
	CepstrumFrame frame = null;
	try {
	    String line = reader.readLine();
	    
	    if (line != null && line.startsWith(CEPSTRUM_FRAME)) {
		int numberCepstrum = Integer.parseInt
		    (line.substring(line.lastIndexOf(' ') + 1));
		
		Cepstrum[] cepstra = new Cepstrum[numberCepstrum];
		
		for (int i = 0; i < numberCepstrum; i++) {
		    line = reader.readLine();
		    String[] data = line.split("\\s+");
		    
		    int cepstrumLength = Integer.parseInt(data[1]);
		    float[] cepstrumData = new float[cepstrumLength];
		    for (int j = 2; j < data.length; j++) {
			cepstrumData[j-2] = Float.parseFloat(data[j]);
		    }
		    cepstra[i] = new Cepstrum(cepstrumData);
		}
		frame = new CepstrumFrame(cepstra);
	    }
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}

	return frame;
    }


    /**
     * Main method for the CmnFeatureExtractorTest.
     *
     * @param argv expects one argument, the name of the Cepstra file
     */
    public static void main(String[] argv) {
	try {
	    CmnFeatureExtractorTest test = 
		new CmnFeatureExtractorTest(argv[0]);
	    test.run();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }
}
