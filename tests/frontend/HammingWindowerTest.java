/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.CepstrumProducer;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Preemphasizer;
import edu.cmu.sphinx.util.Timer;

import java.io.FileInputStream;


/**
 * Test program for the HammingWindow
 */
public class HammingWindowerTest {

    public static void main(String[] argv) {
	if (argv.length < 1) {
	    System.out.println("Usage: java FrontEnd <filename>");
	}

        boolean dumpValues = Boolean.getBoolean
            ("tests.frontend.HammingWindowerTest.dumpValues");
        boolean dumpTimes = Boolean.getBoolean
            ("tests.frontend.HammingWindowerTest.dumpTimes");

        Preemphasizer preemphasizer = new Preemphasizer();
        
        CepstrumProducer cepstrumProducer = new CepstrumProducer();
        cepstrumProducer.setDump(dumpValues);

	FrontEnd frontend = new FrontEnd();
      	frontend.addProcessor(preemphasizer);
        frontend.addProcessor(cepstrumProducer);
	frontend.linkProcessors();

	try {
	    frontend.setInputStream(new FileInputStream(argv[0]));
	    frontend.run();
            
            if (dumpTimes) {
                Timer.dumpAll();
            }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
