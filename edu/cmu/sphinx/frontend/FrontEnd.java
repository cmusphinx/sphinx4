/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Iterator;
import java.util.Vector;

/**
 * FrontEnd is a wrapper class for the chain of front end processors.
 * It provides methods for manipulating and navigating the processors.
 * <p>
 * The front end is modeled as a series of data processors,
 * each of which performs a specific signal processing function.
 * For example, a processor performs Fast-Fourier Transform (FFT)
 * on input data, another processor performs high-pass filtering.
 * Figure 1 below describes how the front end looks like:
 * <p>
 * <center>
 * <img src="doc-files/frontend.jpg">
 * <br><b>Figure 1: The Sphinx4 front end.</b>
 * </center>
 * <p>
 * Each such data processor implements the 
 * {@link edu.cmu.sphinx.frontend.DataProcessor} interface. Objects that 
 * implements the {@link edu.cmu.sphinx.frontend.Data} interface
 * enters and exits the front end, and go between the processors in the
 * front end.
 * The input data to the front end is typically audio data, but this
 * front end allows any input type. Similarly, the output data is typically
 * features, but this front end allows any output type. You can configure
 * the front end to accept any input type and return any output type.
 * We will describe the configuration of the front end in more detail
 * below.
 * <p>
 * <b>The Pull Model of the Front End</b>
 * <p>
 * The front end uses a pull model. To obtain output from the front end,
 * one would call the method:
 * <p>
 * <code>
 * FrontEnd frontend = ... // see how to obtain the front end below
 * <br>Data output = frontend.getData();
 * </code>
 * <p>
 * Calling {@link #getData() getData} on the front end would in turn 
 * call the getData() method on the last DataProcessor, which in turn
 * calls the getData()
 * method on the second last DataProcessor, and so on, until the getData()
 * method on the first DataProcessor is called, which reads Data objects
 * from the input. The input to the front end is actually another
 * DataProcessor. Note that the input DataProcessor is not part of the
 * front end and is not shown in the figure above.
 * One common input DataProcessor is the 
 * {@link edu.cmu.sphinx.frontend.util.Microphone}, which implements
 * the DataProcessor interface. The input DataProcessor (e.g., Microphone)
 * is given to the front end via the 
 * {@link #setDataSource(edu.cmu.sphinx.frontend.DataProcessor) setDataSource}
 * method:
 * <p>
 * <code>
 * DataProcessor microphone = new Microphone();
 * <br>microphone.initialize(...);
 * <br>frontend.setDataSource(microphone);
 * </code>
 * <p>
 * Another common input DataProcessor is the
 * {@link edu.cmu.sphinx.frontend.util.StreamDataSource}.
 * It turns a Java {@link java.io.InputStream} into Data objects. 
 * It is usually used in
 * batch mode decoding.
 * <p>
 * <b>Configuring the front end</b>
 * <p>
 * The front end must be configured through the Sphinx properties file.
 * For details about how about properties files, refer to the 
 * <a href="doc-files/FrontEndProperties.html">
 * Sphinx-4 Front End Properties Guide</a>.
 *
 * Current state-of-the-art front ends generate features that contain
 * Mel-frequency cepstral coefficients (MFCC). To specify such a front end
 * (called a 'pipeline') in Sphinx-4, insert the following lines 
 * in the Sphinx properties file:
 * <p>
 * <code>
 * edu.cmu.sphinx.frontend.FrontEndFactory.pipelines = mfcc
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.nStages = 7
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.1.class = {@link edu.cmu.sphinx.frontend.filter.Preemphasizer edu.cmu.sphinx.frontend.filter.Preemphasizer}
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.2.class = {@link edu.cmu.sphinx.frontend.window.RaisedCosineWindower edu.cmu.sphinx.frontend.window.RaisedCosineWindower}
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.3.class = {@link edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform}
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.4.class = {@link edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank}
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.5.class = {@link edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform}
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.6.class = {@link edu.cmu.sphinx.frontend.feature.BatchCMN edu.cmu.sphinx.frontend.feature.BatchCMN}
 * <br>mfcc;edu.cmu.sphinx.frontend.FrontEndFactory.stage.7.class = {@link edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor}
 * </code>
 * <p>
 * Note: In this example, 'mfcc' becomes the name of the front end.
 * <p>
 * Sphinx-4 also allows you to:
 * <ul>
 * <li>specify multiple front end pipelines</li>
 * <li>specify multiple instance of the same DataProcessor in the same
 *     pipeline</li>
 * </ul>
 * <p>
 * For details on how to do this, refer to the "Front End" section in the
 * <a href="doc-files/FrontEndProperties.html">
 * Sphinx-4 Front End Properties Guide</a>.
 * <p>
 * <b>Obtaining a Front End</b>
 * <p>
 * A front end is obtained through the
 * {@link edu.cmu.sphinx.frontend.FrontEndFactory}. You will call the 
 * {@link edu.cmu.sphinx.frontend.FrontEndFactory#getFrontEnd(edu.cmu.sphinx.util.SphinxProperties,String) getFrontEnd} factory method. Continuing the above example,
 * if the name of the front end as specified in the properties file is
 * "mfcc":
 * <p>
 * <code>
 * SphinxProperties properties = ... // pass in from outside
 * <br>FrontEnd frontend = FrontEndFactory.getFrontEnd(properties, "mfcc");
 * </code>
 * <p>
 * <b>Example code:</b>
 * <p>
 * The code below summarizes the above, and show how one would normally
 * initialize the front end, assuming that it is called "mfcc" in the
 * Sphinx properties file, and that the input data comes from the 
 * microphone:
 * <p>
 * <code>
 * // obtaining the front end and setting its data source
 * <br>FrontEnd frontend = FrontEndFactory.getFrontEnd(sphinxProperties, "mfcc");
 * <br>DataProcessor microphone = new Microphone();
 * <br>microphone.initialize(...);
 * <br>
 * <br>// start getting data from the front end
 * <br>Data output = frontend.getData();
 * </code>
 */
public class FrontEnd extends BaseDataProcessor {

    private DataProcessor first;
    private DataProcessor last;

    private Vector signalListeners = new Vector();


    /**
     * Constructs a FrontEnd with the given first and last DataProcessors,
     * which encloses a chain of DataProcessors.
     *
     * @param firstProcessor first processor in the processor chain
     * @param lastProcessor last processor in the processor chain
     */
    public FrontEnd(DataProcessor firstProcessor,
                    DataProcessor lastProcessor) {
        this.first = firstProcessor;
        this.last = lastProcessor;
    }


    /**
     * Initializes this Front End.
     *
     * @param name         the name of this front end
     * @param frontEndName the name of the front-end pipeline this
     *                     front end is in
     * @param props        the SphinxProperties to use
     * @param predecessor  the predecessor of this Front End
     */
    public void initialize(String name, String frontEndName,
                           SphinxProperties props,
                           DataProcessor predecessor) {
        super.initialize(name, frontEndName, props, predecessor);
    }


    /**
     * Sets the source of data for this front end.
     * It basically sets the predecessor of the first DataProcessor
     * of this front end.
     *
     * @param dataSource the source of data 
     */
    public void setDataSource(DataProcessor dataSource) {
        first.setPredecessor(dataSource);
    }


    /**
     * Returns the processed Data output, basically calls
     * <code>getData()</code> on the last processor.
     *
     * @return an Data object that has been processed by this front end
     *
     * @throws DataProcessingException if a data processor error occurs
     */
    public Data getData() throws DataProcessingException {
        Data data = last.getData();
        
        // fire the signal listeners if its a signal
        if (data instanceof Signal) {
            fireSignalListeners((Signal) data);
        }

        return data;
    }


    /**
     * Sets the source of data for this front end.
     * It basically calls <code>setDataSource(dataSource)</code>.
     *
     * @param dataSource the source of data 
     */
    public void setPredecessor(DataProcessor dataSource) {
        setDataSource(dataSource);
    }

    
    /**
     * Finds the DataProcessor with the given name.
     *
     * @param processorName the name of the DataProcessor to find
     *
     * @return the DataProcessor with the given name, or null if no
     *         DataProcessor with the given name was found
     */
    public DataProcessor findDataProcessor(String processorName) {
        DataProcessor current = last;
        while (current != null) {
            if (current.getName().equals(processorName)) {
                return current;
            } else {
                current = current.getPredecessor();
            }
        }
        return null;
    }


    /**
     * Add a listener to be called when a signal is detected.
     *
     * @param listener the listener to be added
     */
    public void addSignalListener(SignalListener listener) {
        signalListeners.add(listener);
    }


    /**
     * Removes a listener for signals.
     *
     * @param listener the listener to be removed
     */
    public void removeSignalListener(SignalListener listener) {
        signalListeners.remove(listener);
    }


    /**
     * Fire all listeners for signals.
     *
     * @param feature the feature with non-content signal
     */
    protected void fireSignalListeners(Signal signal) {
        Vector copy = (Vector) signalListeners.clone();
        for (Iterator i = copy.iterator(); i.hasNext(); ) {
            SignalListener listener = (SignalListener) i.next();
            listener.signalOccurred(signal);
        }
    }

    
    /**
     * Returns a description of this FrontEnd in the format:
     * <front end name> {<DataProcessor1>, <DataProcessor2> ... 
     * <DataProcessorN>}
     *
     * @return a description of this FrontEnd
     */
    public String toString() {
        String description = "";
        DataProcessor current = last;
        while (current != null) {
            description = (current.getName() + description);
            current = current.getPredecessor();
            if (current != null) {
                description = (", " + description);
            }
        }
        return (getName() + " {" + description + "}");
    }
}
