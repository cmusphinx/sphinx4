/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.util.List;
import java.util.ListIterator;

/**
 * Translates audio into speech cepstra. The Cepstra producer takes a
 * DoubleAudioFrame as input, and outputs a CepstraFrame (which is an
 * array of Cepstra).
 *
 * The audio is processed in windows (called a frame). We apply to each
 * audio frame a HammingWindow, followed by a Fast-Fourier Transform.
 * Each of these frames now become a Cepstrum. All these frames together
 * give a CepstrumFrame.
 */
public class CepstrumProducer implements Processor {

    private static final String PROP_FRAME_SIZE =
	"edu.cmu.sphinx.frontend.frameSize";
    private static final String PROP_FRAME_SHIFT =
	"edu.cmu.sphinx.frontend.frameShift";
    private static final String PROP_CEPSTRUM_SIZE =
	"edu.cmu.sphinx.frontend.cepstrumSize";

    private int frameSize;
    private int frameShift;
    private int cepstrumSize;

    private Processor hammingWindower;
    private Processor fastFourierTransformer;

    // a DoubleAudioFrame template for producing cepstrum
    private DoubleAudioFrame template;


    /**
     * Constructs a default CepstrumProducer with the given Pre-emphasis Factor
     * value.
     */
    public CepstrumProducer() {

	getSphinxProperties();
	template = new DoubleAudioFrame(frameSize);

	// hammingWindower = new HammingWindower();
	// fastFourierTransformer = new FastFourierTransformer();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void getSphinxProperties() {
	// TODO : specify the context
	SphinxProperties properties = SphinxProperties.getSphinxProperties("");

	frameSize = properties.getInt(PROP_FRAME_SIZE, 410);
	frameShift = properties.getInt(PROP_FRAME_SHIFT, 160);
	cepstrumSize = properties.getInt(PROP_CEPSTRUM_SIZE, 13);
    }
	

    /**
     * Converts the given input DoubleAudioFrame into a CepstrumFrame.
     *
     * @param input the input DoubleAudioFrame
     *
     * @return a CepstrumFrame
     */
    public Data process(Data input) {

	DoubleAudioFrame audioDataFrame = (DoubleAudioFrame) input;
	double[] samples = audioDataFrame.getData();

	if (samples.length != template.getData().length) {
	    template = new DoubleAudioFrame(samples.length);
	}

	int frameCount = getFrameCount(samples.length);
	Cepstrum[] cepstra = new Cepstrum[frameCount];

	for (int i = 0, frameStart = 0; i < frameCount;
	     i++, frameStart += frameShift) {

	    // copy the frame into the template
	    System.arraycopy(samples, frameStart, template, 0, frameSize);

	    // apply hamming window
	    hammingWindower.process(template);

	    // apply FFT
	    cepstra[i] = (Cepstrum) fastFourierTransformer.process(template);
	}

	return (new CepstrumFrame(cepstra));
    }
    
    
    /**
     * Returns the number of frames in the given array.
     *
     * @param arrayLength the length of the array
     *
     * @return the number of frames
     */
    private int getFrameCount(int arrayLength) {
	int frameCount = 0;
	for (int frameStart = 0; frameStart + frameSize < arrayLength;
	     frameStart += frameShift) {
	    frameCount++;
	}
	return frameCount;
    }
}
