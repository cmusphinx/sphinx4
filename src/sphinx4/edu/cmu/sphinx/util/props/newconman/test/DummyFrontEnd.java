package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.newconman.PropSheet;
import edu.cmu.sphinx.util.props.newconman.S4Boolean;
import edu.cmu.sphinx.util.props.newconman.S4ComponentList;
import edu.cmu.sphinx.util.props.newconman.SimpleConfigurable;

import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class DummyFrontEnd implements SimpleConfigurable {

    @S4Boolean(defaultValue = true)
    public static final String PROP_USE_MFFCS = "useMfccs";

    @S4ComponentList
    public static final String DATA_PROCS = "dataProcs";

    List<SimpleConfigurable> dataProcs;

    boolean useMfccs;


    public void newProperties(PropSheet ps) throws PropertyException {
        useMfccs = ps.getBoolean(PROP_USE_MFFCS);
        dataProcs = ps.getComponentList(DATA_PROCS);
    }


    public boolean isUseMfccs() {
        return useMfccs;
    }


    public List<SimpleConfigurable> getDataProcs() {
        return dataProcs;
    }


    public String getName() {
        return this.getClass().getName();
    }
}
