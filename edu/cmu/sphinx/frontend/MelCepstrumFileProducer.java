/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;

import java.io.IOException;


/**
 * Produces MelCepstrum data from a file.
 */
public class MelCepstrumFileProducer extends DataProcessor {

    public final static String PROP_CEPSTRUM_FILE =
	"edu.cmu.sphinx.frontend.MelCepstrumFileProducer.file";
    private String path;
    private ExtendedStreamTokenizer est;
    private int numFrames;
    private int curFrame;
    private int cepstrumLength;

    /**
     * Constructs a MelCepstrumFileProducer that reads
     * MelCepstrum data from the given path.
     *
     * @param context the context for the producer
     *
     * @throws IOException if an error occurs while reading the data
     */
    public MelCepstrumFileProducer(String context) throws IOException {
	super("MelCepstrumFileProducer", context);
	initSphinxProperties(context);
	est = new ExtendedStreamTokenizer(path);
	numFrames = est.getInt("num_frames");
	est.expectString("frames");
    }

    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties(String context) {
	SphinxProperties properties = getSphinxProperties();
	cepstrumLength = properties.getInt(FrontEnd.PROP_CEPSTRUM_SIZE, 13);
	path = properties.getString(PROP_CEPSTRUM_FILE, "file");
	System.out.println("File is " + path);
    }


    /**
     * Returns the next Data object, which is the mel cepstrum of the
     * input frame. However, it can also be other Data objects
     * like a EndPointSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

	Data data = null;

	if (curFrame == 0) {
	    data = EndPointSignal.SEGMENT_START;
	} else if (curFrame == numFrames) {
            data = EndPointSignal.SEGMENT_END;
	} else if (curFrame > numFrames) {
            data = null;
	} else {
	    float[] vectorData = new float[cepstrumLength];
	    for (int i = 0; i < cepstrumLength; i++) {
		vectorData[i] = est.getFloat("cepstrum data");
	    }
	    data  = new Cepstrum(vectorData);
	    System.out.println("CP " + data);
	}
	curFrame++;
	return data;
    }
}
