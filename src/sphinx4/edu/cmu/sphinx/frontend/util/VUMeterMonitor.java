package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.RawPropertyData;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * A VU meter to be plugged into a front-end. Preferably this component should be plugged directly behind the
 * <code>DataBlocker</code> in order to ensure that only equally sized blockes of meaningful length are used for RMS
 * computation.
 * <p/>
 * Because vu-monitoring makes sense only for online speech processing the vu-meter will be visible only if data source
 * which preceeds it is a <code>Microphone</code>.
 *
 * @author Holger Brandl
 */

public class VUMeterMonitor extends BaseDataProcessor {

    VUMeter vumeter;
    VUMeterPanel vuMeterPanel;
    JDialog vuMeterDialog;


    public VUMeterMonitor() {
        vumeter = new VUMeter();

        vuMeterPanel = new VUMeterPanel();
        vuMeterPanel.setVu(vumeter);
        vuMeterPanel.start();

        vuMeterDialog = new JDialog();
        vuMeterDialog.setBounds(100, 100, 100, 400);

        vuMeterDialog.getContentPane().setLayout(new BorderLayout());
        vuMeterDialog.getContentPane().add(vuMeterPanel);

        vuMeterDialog.setVisible(true);
    }


    public Data getData() throws DataProcessingException {
        Data d = getPredecessor().getData();

        // show the panel only if  a microphone is used as data source
        if (d instanceof DataStartSignal)
            vuMeterPanel.setVisible(FrontEndUtils.getFrontEndProcessor(this, Microphone.class) != null);

        if (d instanceof DoubleData)
            vumeter.calculateVULevels(d);

        return d;
    }


    public JDialog getVuMeterDialog() {
        return vuMeterDialog;
    }


    /** A little test-function which plugs a microphone directly into the vu-meter.
     * @param args
     * @throws edu.cmu.sphinx.frontend.DataProcessingException*/
    public static void main(String[] args) throws DataProcessingException {
        Microphone mic = new Microphone( 16000, 16, 1,
                          true, true, true, 10, false,
                          "selectChannel", 2, "default");

        mic.initialize();
        mic.startRecording();

        VUMeterMonitor monitor = new VUMeterMonitor();
        monitor.getVuMeterDialog().setModal(true);
        monitor.setPredecessor(mic);

        while (true) {
            monitor.getData();
        }
    }
}
