/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

import java.io.IOException;
import java.util.*;

/**
 * Groups Cepstra of an single Utterance together into a CepstraGroup.
 * It reads in Cepstra from its predecessor, groups them together,
 * and returns the same Cepstra in the getCepstrum() method unmodified.
 */
public abstract class CepstraGroupProducer 
extends DataProcessor implements CepstrumSource {

    private CepstrumSource predecessor;
    private List cepstraGroupList = new LinkedList();
    private List cepstraList = new LinkedList();


    /**
     * Constructs a CepstraGroupProducer with the given name, context,
     * and predecessor.
     */
    public CepstraGroupProducer(String name, String context, 
                                CepstrumSource predecessor) {
        super(name, context);
        this.predecessor = predecessor;
    }

    /**
     * Override this method to specify what should be done when
     * a CepstraGroup is produced.
     */
    public abstract void cepstraGroupProduced(CepstraGroup cepstraGroup);

    /**
     * Returns the next available Cepstrum.
     */
    public Cepstrum getCepstrum() throws IOException {
        
        Cepstrum cepstrum = predecessor.getCepstrum();
        if (cepstrum != null) {
            Cepstrum copy = cepstrum;

            if (cepstrum.hasContent()) {
                // Need to replicate the Cepstrum since the original
                // Cepstrum will be normalized in a later step.
                // Here, though, we only need the first element (energy).
                float[] data = new float[1];
                data[0] = cepstrum.getCepstrumData()[0];
                copy = new Cepstrum(data, cepstrum.getUtterance());
            }

            cepstraList.add(copy);
            
            Signal signal = copy.getSignal();
            
            // If this is the end of an Utterance, group all
            // the Cepstrum found so far together to create a
            // CepstrumGroup, and give it to the CepstraViewer.
            
            if (signal != null && signal.equals(Signal.UTTERANCE_END)) {
                
                // an Utterance has ended
                Cepstrum[] cepstra = new Cepstrum[cepstraList.size()];
                cepstraList.toArray(cepstra);
                String name = "no name";
                
                // find the name of this group
                for (int i = 0; i < cepstra.length; i++) {
                    if (cepstra[i].getUtterance() != null) {
                        name = cepstra[i].getUtterance().getName();
                        break;
                    }
                }
                
                // create the CepstraGroup and add it to the viewer
                CepstraGroup cepstraGroup = new CepstraGroup
                    (cepstra, name);
                cepstraGroupProduced(cepstraGroup);
                cepstraList.clear();
            }
        }

        return cepstrum;
    }
}
