/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.Signal;

import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Represents a group of Cepstrum.
 */
public class CepstraGroup {

    private Cepstrum[] cepstra;
    private String name;
   
    /**
     * Creates a default CepstraGroup with the given array of cepstra
     * and name.
     *
     * @param cepstra the array of cepstra
     * @param name a name for this CepstraGroup
     */
    public CepstraGroup(Cepstrum[] cepstra, String name) {
        this.cepstra = cepstra;
        this.name = name;
    }

    /**
     * Returns the array of Cepstrum in this group.
     */
    public Cepstrum[] getCepstra() {
        return cepstra;
    }

    /**
     * Returns the name of this CepstraGroup
     */
    public String getName() {
        return name;
    }
}
