/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Apply Cepstral Mean Normalization (CMN) in batch mode (only use
 * this in batch mode). It will read in all the cepstra belonging
 * to one Utterance, then calculate the mean cepstrum, and subtract
 * this mean cepstrum from all the cepstra.
 *
 * @see Cepstrum
 */
public class BatchCMN extends DataProcessor implements CepstrumSource {

    private float[] sums;           // array of current sums
    private int cepstrumLength;     // length of a Cepstrum
    private CepstrumSource predecessor;
    private List cepstraList;


    /**
     * Constructs a default BatchCMN with the given
     * SphinxProperties context.
     *
     * @param name the name of this BatchCMN
     * @param context the context of the SphinxProperties to use
     * @param predecessor the CepstrumSource from which this normalizer
     *    obtains Cepstrum to normalize
     */
    public BatchCMN(String name, String context, CepstrumSource predecessor) {
        super(name, context);
        initSphinxProperties();
        sums = new float[cepstrumLength];
        this.predecessor = predecessor;
        cepstraList = new LinkedList();
    }


    /**
     * Sets the predecessor (i.e., the previous processor)
     * of this LiveCMN processor.
     *
     * @param predecessor the predecessor
     */
    public void setPredecessor(CepstrumSource predecessor) {
        this.predecessor = predecessor;
    }


    /**
     * Initializes the sums array and clears the cepstra list.
     */
    private void reset() {
        Arrays.fill(sums, 0.0f);
        cepstraList.clear();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
        SphinxProperties properties = getSphinxProperties();
	cepstrumLength = properties.getInt(FrontEnd.PROP_CEPSTRUM_SIZE, 13);
    }
	

    /**
     * Returns the next Cepstrum object, which is a normalized Cepstrum
     * produced by this class. However, it can also be a Cepstrum object
     * carrying a Signal.UTTERANCE_END signal.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Cepstrum objects
     */
    public Cepstrum getCepstrum() throws IOException {

        Cepstrum output = null;

        if (cepstraList.size() > 0) {
            output = (Cepstrum) cepstraList.remove(0);
        } else {

            reset();

            // read the cepstra of the entire utterance, calculate
            // and apply the cepstral mean
            if (readUtterance() > 0) {
                normalizeList();
                output = getCepstrum();
            }            
        }

        return output;
    }


    /**
     * Reads the cepstra of the entire Utterance into the cepstraList.
     *
     * @return the number cepstra (with Data) read
     *
     * @throws IOException if an error occurred reading the Cepstrum
     */
    private int readUtterance() throws IOException {

        int count = 0;

        Cepstrum input = null;

        do {
            input = predecessor.getCepstrum();
            if (input != null) {
                if (input.hasContent()) {
                    count++;
                    float[] cepstrumData = input.getCepstrumData();
                    for (int j = 0; j < cepstrumData.length; j++) {
                        sums[j] += cepstrumData[j];
                    }
                    cepstraList.add(input);

                } else if (input.hasUtteranceEndSignal()) {
                    cepstraList.add(input);
                    break;
                } else { // UTTERANCE_START
                    cepstraList.add(input);
                }
            }
        } while (input != null);

        return count;
    }
        

    /**
     * Normalizes the list of Cepstrum.
     */
    private void normalizeList() {

        int numberCepstrum = cepstraList.size();

        // calculate the mean first
        for (int i = 0; i < sums.length; i++) {
            sums[i] /= numberCepstrum;
        }

        for (Iterator iterator = cepstraList.iterator();
             iterator.hasNext();) {
            Cepstrum cepstrumObject = (Cepstrum) iterator.next();

            if (cepstrumObject.hasContent()) {
                float[] cepstrum = cepstrumObject.getCepstrumData();
                for (int j = 0; j < cepstrum.length; j++) {
                    cepstrum[j] -= sums[j]; // sums[] is now the means[]
                }
            }   

            if (getDump()) {
                System.out.println("CMN_CEPSTRUM " + 
                                   cepstrumObject.toString());
            }
        }
    }
}
