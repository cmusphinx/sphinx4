/*
 * Copyright 2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import edu.cmu.sphinx.decoder.adaptation.DensityFileData;
import edu.cmu.sphinx.decoder.adaptation.MllrEstimation;
import edu.cmu.sphinx.decoder.adaptation.MllrTransformer;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.recognizer.Recognizer;


/**
 * Base class for high-level speech recognizers.
 */
public class AbstractSpeechRecognizer {

    protected final Context context;
    protected final Recognizer recognizer;

    protected final SpeechSourceProvider speechSourceProvider;
    
    protected boolean collectStatsForAdaptation;
    private MllrEstimation estimation;

    /**
     * Constructs recognizer object using provided configuration.
     */
    public AbstractSpeechRecognizer(Configuration configuration)
        throws IOException
    {
        this(new Context(configuration));
    }

    protected AbstractSpeechRecognizer(Context context) throws IOException {
        this.context = context;
        recognizer = context.getInstance(Recognizer.class);
        speechSourceProvider = new SpeechSourceProvider();
    }
    
    protected void initAdaptation() throws Exception{
    	this.collectStatsForAdaptation = true;
    	Loader loader = context.getLoader();
    	this.estimation = new MllrEstimation("", 1, "", false, "", false, loader);
    }
    
    private MllrTransformer getTransformer() throws IOException, URISyntaxException{
    	Sphinx3Loader loader = (Sphinx3Loader) context.getLoader();
    	DensityFileData means = new DensityFileData("", -Float.MAX_VALUE, loader, false);
    	MllrTransformer transformer;
    	
    	if (!this.estimation.isComplete()){
    		this.estimation.estimateMatrices();
    	}
    	
    	means.getMeansFromLoader();
    	transformer = new MllrTransformer(means, estimation.getA(), estimation.getB(), "");
    	transformer.transform();
    	
    	return transformer;
    }
    
    public void adaptModelOnDisk() throws IOException, URISyntaxException{
    	Sphinx3Loader loader = (Sphinx3Loader) context.getLoader();
    	String path = loader.getLocation() + "/means";
    	MllrTransformer transformer = this.getTransformer();
    	
    	transformer.setOutputMeanFile(path);
    	transformer.writeToFile();
    }
    
    public void writeTransformationToFile(String filepath){
    	if (!this.estimation.isComplete()){
    		this.estimation.estimateMatrices();
    	}
    	
    	this.estimation.setOutputFilePath(filepath);
    	
    	try {
			estimation.createMllrFile();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    }
    
    public void adaptCurrentModel() throws IOException, URISyntaxException{
    	MllrTransformer transformer = this.getTransformer();

    	//TODO : adapt current model
    }
    

    /**
     * Returns result of the recognition.
     */
    public SpeechResult getResult() {
        Result result = recognizer.recognize();
        SpeechResult speechRes = null == result ? null : new SpeechResult(result);
        
        if(this.collectStatsForAdaptation && speechRes != null){
        	try {
				estimation.addCounts(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        return speechRes;
    }
    
    /**
     * Returns the Loader object used for loading the acoustic model.
     */
    public Loader getLoader() {
    	return (Loader) context.getLoader();
    }
}
