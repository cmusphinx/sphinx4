package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.RawPropertyData;

import javax.swing.*;
import java.awt.*;

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
    VUMeterPanel vuMeterPanel = new VUMeterPanel();
    JDialog vuMeterDialog;


    public VUMeterMonitor() {
        vumeter = new VUMeter();

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


    /** A little test-function which plugs a microphone directly into the vu-meter. */
    public static void main(String[] args) throws DataProcessingException {
        Microphone mic = new Microphone();

        PropertySheet propSheet = new PropertySheet(mic, null, new RawPropertyData("tt", mic.getClass().getName()), new ConfigurationManager());
        try {
            propSheet.setInt(Microphone.PROP_MSEC_PER_READ, Microphone.PROP_MSEC_PER_READ_DEFAULT);
            propSheet.setInt(Microphone.PROP_SAMPLE_RATE, Microphone.PROP_SAMPLE_RATE_DEFAULT);
            propSheet.setString(Microphone.PROP_STEREO_TO_MONO, "selectChannel");
            propSheet.setInt(Microphone.PROP_SELECT_CHANNEL, 2);
            propSheet.setBoolean(Microphone.PROP_BIG_ENDIAN, true);
//            propSheet.setLogger(getLogger());

            mic.newProperties(propSheet);
            mic.initialize();
        } catch (PropertyException e) {
            e.printStackTrace();
        }

        mic.startRecording();

        VUMeterMonitor monitor = new VUMeterMonitor();
        monitor.getVuMeterDialog().setModal(true);
        monitor.setPredecessor(mic);

        while (true) {
            monitor.getData();
        }
    }
}
