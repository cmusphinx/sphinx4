package edu.cmu.sphinx.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;
import edu.cmu.sphinx.util.props.PropertyException;

public class GrammarAligner implements Aligner {

	private Recognizer recognizer;
	private AlignerGrammar grammar;
	private AudioFileDataSource dataSource;

	public GrammarAligner(URL acousticModel, URL dictionary, URL g2p) throws PropertyException, MalformedURLException {
		ConfigurationManager cm = new ConfigurationManager(ConfigurationManagerUtils.resourceToURL("resource:/edu/cmu/sphinx/config/aligner.xml"));

		cm.setGlobalProperty("acousticModel", acousticModel.toString());
		cm.setGlobalProperty("dictionary", dictionary.toString());
		cm.setGlobalProperty("filler", acousticModel.toString() + "/noisedict");
		cm.setGlobalProperty("g2p", "");
		
		recognizer = (Recognizer)cm.lookup("recognizer");
		grammar = (AlignerGrammar)cm.lookup("alignerGrammar");
		dataSource = (AudioFileDataSource)cm.lookup("audioFileDataSource");
		recognizer.allocate();
	}

	@Override
	public ArrayList<WordResult> align(AudioInputStream stream, String text) {
		dataSource.setInputStream(stream, "input");
		grammar.setText(text);
		Result result = recognizer.recognize();
		return result.getWords();
	}
}
