/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.CepstralMeanNormalizer;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.FeatureExtractor;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Microphone;
import edu.cmu.sphinx.frontend.MelCepstrumProducer;
import edu.cmu.sphinx.frontend.MelFilterbank;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.Windower;
import edu.cmu.sphinx.frontend.Preemphasizer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * Test program for the Microphone. Listens for audio data
 * on the system's audio capturing device, and does frontend
 * processing on it.
 * Running this class shows a little swing application that contains
 * a "Listen" and "Stop" button. 
 */
public class MicrophoneTest extends JFrame {

    private static String testName = "MicrophoneTest";
    private Microphone microphone;
    private FrontEnd frontend;
    private JPanel panel;
    private JTextArea featureTextArea;


    /**
     * Main method to run this test.
     */
    public static void main(String[] argv) {
        MicrophoneTest microphoneTest = new MicrophoneTest();
        microphoneTest.show();
    }


    /**
     * Constructs a default MicrophoneTest.
     */
    public MicrophoneTest() {
        super(testName);
        setSize(new Dimension(500, 300));

        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });

        createModel();           // create the frontend components
        createPanel();           // create the GUI components
        getContentPane().add(panel, BorderLayout.CENTER);
    }


    /**
     * Creates the frontend components.
     */
    private void createModel() {

        frontend = new FrontEnd("FrontEnd", testName);        
        microphone = new Microphone("Microphone", testName);

        new Thread(microphone).start();
        
        frontend.setAudioSource(microphone);
        
        Preemphasizer preemphasizer = new Preemphasizer
            ("Preemphasizer", testName, frontend.getAudioSource());
        Windower windower = new Windower
            ("HammingWindow", testName, preemphasizer);
        SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
            ("FFT", testName, windower);
        MelFilterbank melFilterbank = new MelFilterbank
            ("MelFilter", testName, spectrumAnalyzer);
        MelCepstrumProducer melCepstrum = new MelCepstrumProducer
            ("MelCepstrum", testName, melFilterbank);
        CepstralMeanNormalizer cmn = new CepstralMeanNormalizer
            ("CMN", testName, melCepstrum);
        FeatureExtractor extractor = new FeatureExtractor
            ("FeatureExtractor", testName, cmn);
        
        frontend.setFeatureSource(extractor);
    }


    /**
     * Creates the main panel where all the swing GUI components are
     * placed.
     */
    private void createPanel() {

        panel = new JPanel(new BorderLayout());
        
        featureTextArea = new JTextArea();
        JScrollPane textScrollPane = new JScrollPane(featureTextArea);

        panel.add(createButtonPanel(), BorderLayout.NORTH);
        panel.add(textScrollPane, BorderLayout.CENTER);
    }


    /**
     * Creates the top sub-Panel where the "Listen" and "Stop" buttons
     * are placed.
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton startButton = new JButton("Listen");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                microphone.startRecording();
                (new FeatureConsumer()).start();
            }
        });

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                microphone.stopRecording();
            }
        });
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        return buttonPanel;
    }
    

    /**
     * A FeatureConsumer thread that gets FeatureFrames from the
     * Frontend and simply prints them out at textArea swing GUI.
     */
    class FeatureConsumer extends Thread {
        
        public void run() {
            int featureBlockSize = 25;
            int featureCount = 0;
            FeatureFrame featureFrame = null;
            do {
                try {
                    featureFrame = frontend.getFeatureFrame(featureBlockSize);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                if (featureFrame != null) {
                    outputFeatureFrame(featureFrame.getFeatures());
                } else {
                    break;
                }
            } while (featureFrame.getFeatures().length == featureBlockSize);
        }


        private void outputFeatureFrame(Feature[] features) {
            for (int i = 0; i < features.length; i++) {
                featureTextArea.append("FEATURE: " + features[i].toString() + 
                                       "\n");
            }
        }
    }
}


