/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

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
 * DataProcessor, and is usually (though not necessarily) part of
 * the front end and is not shown in the figure above.
 * If you want to maintain some control of the input DataProcessor,
 * you can create it separately, and use the 
 * {@link #setDataSource(edu.cmu.sphinx.frontend.DataProcessor) setDataSource}
 * method to set it as the input DataProcessor. In that case, the input 
 * DataProcessor will be prepended to the existing chain of DataProcessors.
 * One common input DataProcessor is the 
 * {@link edu.cmu.sphinx.frontend.util.Microphone}, which implements
 * the DataProcessor interface.
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
 * For details about configuring the front end, refer to the document 
 * <a href="doc-files/FrontEndConfiguration.html">Configuring the Front End</a>.
 *
 * Current state-of-the-art front ends generate features that contain
 * Mel-frequency cepstral coefficients (MFCC). To specify such a front end
 * (called a 'pipeline') in Sphinx-4, insert the following lines 
 * in the Sphinx-4 configuration file:
 * <p>
 * <pre>
 * &lt;component name="mfcFrontEnd" type="edu.cmu.sphinx.frontend.FrontEnd"&gt;
 *     &lt;propertylist name="pipeline"&gt;
 *        &lt;item&gt;preemphasizer&lt;/item&gt;
 *        &lt;item&gt;windower&lt;/item&gt;
 *        &lt;item&gt;dft&lt;/item&gt;
 *        &lt;item&gt;melFilterBank&lt;/item&gt;
 *        &lt;item&gt;dct&lt;/item&gt;
 *        &lt;item&gt;batchCMN&lt;/item&gt;
 *        &lt;item&gt;featureExtractor&lt;/item&gt;
 *     &lt;/propertylist&gt;
 * &lt;/component&gt;
 *
 * &lt;component name="preemphasizer" type="{@link edu.cmu.sphinx.frontend.filter.Preemphasizer edu.cmu.sphinx.frontend.filter.Preemphasizer}"/&gt;
 * &lt;component name="windower" type="{@link edu.cmu.sphinx.frontend.window.RaisedCosineWindower edu.cmu.sphinx.frontend.window.RaisedCosineWindower}"/&gt;
 * &lt;component name="dft" type="{@link edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform}"/&gt;
 * &lt;component name="melFilterBank" type="{@link edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank}"/&gt;
 * &lt;component name="dct" type="{@link edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform}"/&gt;
 * &lt;component name="batchCMN" type="{@link edu.cmu.sphinx.frontend.feature.BatchCMN edu.cmu.sphinx.frontend.feature.BatchCMN}"/&gt;
 * &lt;component name="featureExtractor" type="{@link edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor}"/&gt;
 * </pre>
 * <p>
 * Note: In this example, 'mfcFrontEnd' becomes the name of the front end.
 * <p>
 * Sphinx-4 also allows you to:
 * <ul>
 * <li>specify multiple front end pipelines</li>
 * <li>specify multiple instance of the same DataProcessor in the same
 *     pipeline</li>
 * </ul>
 * <p>
 * For details on how to do this, refer to the document 
 * <a href="doc-files/FrontEndConfiguration.html">Configuring the Front End</a>.
 * <p>
 * <b>Obtaining a Front End</b>
 * <p>
 * In order to obtain a front end, it must be specified in the configuration
 * file. The Sphinx-4 front end is connected to the rest of the system
 * via the scorer. We will continue with the above example to show how
 * the scorer will obtain the front end. In the configuration file,
 * the scorer should be specified as follows:
 * <p>
 * <pre>
 * &lt;component name="scorer" type="edu.cmu.sphinx.decoder.scorer.SimpleAcousticScorer"&gt;
 *     &lt;property name="frontend" value="mfcFrontEnd"/&gt;
 * &lt;/component&gt;
 * </pre>
 * <p>
 * In the SimpleAcousticScorer, the front end is obtained in the
 * {@link edu.cmu.sphinx.util.props.Configurable#newProperties newProperties}
 * method as follows:
 * <pre>
 * public void newProperties(PropertySheet ps) throws PropertyException {
 *     FrontEnd frontend = (FrontEnd) ps.getComponent("frontend", FrontEnd.class);
 * }
 * </pre>
 */
public class FrontEnd extends BaseDataProcessor  {

    /**
     * the name of the property list of all the components of
     * the frontend pipe line
     */
    public static String PROP_PIPELINE = "pipeline";
    
    
    // ----------------------------
    // Confiugration data
    // -----------------------------
    private List frontEndList;
    
    private DataProcessor first;
    private DataProcessor last;
    private Vector signalListeners = new Vector();

    

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry) throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_PIPELINE, PropertyType.COMPONENT_LIST);
    }


    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        frontEndList = ps.getComponentList(PROP_PIPELINE, DataProcessor.class);
        
        for (Iterator i = frontEndList.iterator(); i.hasNext(); ) {
            DataProcessor dp = (DataProcessor) i.next();
            dp.setPredecessor(last);
            if (first == null) {
                first = dp;
            }
            last = dp;
        }
        initialize();
    }
    

 
    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend.CommonConfig)
     */
    public void initialize() {
        super.initialize();
        for (Iterator i = frontEndList.iterator(); i.hasNext(); ) {
            DataProcessor dp = (DataProcessor) i.next();
            dp.initialize();
        }
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
     * @param signal the signal that occurred
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
