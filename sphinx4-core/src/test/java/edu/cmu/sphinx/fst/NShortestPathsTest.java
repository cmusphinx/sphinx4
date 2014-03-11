/**
 * 
 */
package edu.cmu.sphinx.fst;

import static edu.cmu.sphinx.fst.Convert.importFst;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;

import org.testng.annotations.Test;

import edu.cmu.sphinx.Sphinx4TestCase;
import edu.cmu.sphinx.fst.operations.NShortestPaths;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class NShortestPathsTest extends Sphinx4TestCase {

    @Test
    public void testNShortestPaths() {
        String path = "algorithms/shortestpath/A.fst";
        File parent = getResourceFile(path).getParentFile();
        
        path = new File(parent, "A").getPath();
        Fst fst = importFst(path, new TropicalSemiring());
        path = new File(parent, "nsp").getPath();
        Fst nsp = importFst(path, new TropicalSemiring());

        Fst fstNsp = NShortestPaths.get(fst, 6, true);
        assertThat(nsp, equalTo(fstNsp));
    }
}
