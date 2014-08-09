package edu.cmu.sphinx.demo.mllrTransform;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import edu.cmu.sphinx.decoder.adaptation.DensityFileData;
import edu.cmu.sphinx.decoder.adaptation.MllrTransformer;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

public class MllrTransformerDemo {

	public static void main(String[] args) throws IOException,
			URISyntaxException {
		
		URL x = new URL("file:/home/bogdanpetcu/RSoC/en-us");
		Sphinx3Loader loader = new Sphinx3Loader(x, "en-us", "", null, 0, 0,
				(float) 1e-5, false);

		DensityFileData means = new DensityFileData("means", -Float.MAX_VALUE,
				loader, true);
		means.loadFile();
		
		MllrTransformer mt = new MllrTransformer(means, new float[0][0][0][0], new float[0][0][0], "/home/bogdanpetcu/output");
		mt.transform();
		mt.writeToFile();
	}

}
