package edu.cmu.sphinx.tools.endpoint;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;

public class Segmenter {

	public static void main(String[] args) throws MalformedURLException {
	       URL audioURL;

	        if (args.length == 1 || args.length == 2) {
	            audioURL = new File(args[0]).toURI().toURL();
	        } else {
	            System.out.println ("Usage: java -cp lib/batch.jar:lib/sphinx4.jar edu.cmu.sphinx.tools.endpoint.Segmenter <filename> [--no-split]");
	            return;
	        }
	        

	        URL configURL = Segmenter.class.getResource("frontend.config.xml");

	        ConfigurationManager cm = new ConfigurationManager(configURL);
	        FrontEnd frontend = (FrontEnd) cm.lookup("endpointer");

	        if (args.length >= 2 && args[1].equals("-no-split")) {
	            ConfigurationManagerUtils.setProperty(cm, "wavWriter", "captureUtterances", "false");
            }
	        
	        frontend.initialize();

	        // configure the audio input for the recognizer
	        AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
	        dataSource.setAudioFile(audioURL, null);
	        
	        Data data = null;
	        do {
	        	data = frontend.getData();
	        } while (data != null);
	        }
}
