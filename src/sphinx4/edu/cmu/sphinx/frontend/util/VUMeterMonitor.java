package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.*;

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


    public VUMeterMonitor() {
        vumeter = new VUMeter();

        vuMeterPanel.setVu(vumeter);
        vuMeterPanel.start();
    }


    public Data getData() throws DataProcessingException {
        Data d = getPredecessor().getData();

        // show the panel only if  a microphone is used as data source
        if (d instanceof DataStartSignal)
            vuMeterPanel.setVisible(FrontEndUtils.getDataSource(this, Microphone.class) != null);

        if (d instanceof DoubleData)
            vumeter.calculateVULevels(d);

        return d;
    }
}
