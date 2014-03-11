/**
 * 
 */
package edu.cmu.sphinx.fst;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;

import org.testng.annotations.Test;

import edu.cmu.sphinx.Sphinx4TestCase;
import edu.cmu.sphinx.fst.operations.Determinize;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class DeterminizeTest extends Sphinx4TestCase {
    @Test
    public void testDeterminize() {
        String path = "algorithms/determinize/fstdeterminize.fst.ser";
        File parent = getResourceFile(path).getParentFile();
        
        path = new File(parent, "A").getPath();
        Fst fstA = Convert.importFst(path, new TropicalSemiring());
        path = new File(parent, "fstdeterminize.fst.ser").getPath();
        Fst determinized = Fst.loadModel(path);

        Fst fstDeterminized = Determinize.get(fstA);
        assertThat(determinized, equalTo(fstDeterminized));
    }
}
