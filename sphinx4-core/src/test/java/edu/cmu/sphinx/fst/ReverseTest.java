/**
 * 
 */
package edu.cmu.sphinx.fst;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;

import org.testng.annotations.Test;

import edu.cmu.sphinx.Sphinx4TestCase;
import edu.cmu.sphinx.fst.operations.Reverse;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;


/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class ReverseTest extends Sphinx4TestCase {

    @Test
    public void testReverse() {
        String path = "algorithms/reverse/A.fst";
        File parent = getResourceFile(path).getParentFile();

        path = new File(parent, "A").getPath();
        Fst fst = Convert.importFst(path, new TropicalSemiring());
        path = new File(parent, "fstreverse.fst.ser").getPath();
        Fst fstB = Fst.loadModel(path);

        Fst fstReversed = Reverse.get(fst);
        assertThat(fstB, equalTo(fstReversed));
    }

}
