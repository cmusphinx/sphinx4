package edu.cmu.sphinx.linguist.allphone;

import java.io.IOException;
import java.util.logging.Logger;

import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class AllphoneLinguist implements Linguist {

    /** The property that defines the acoustic model to use when building the search graph */
    @S4Component(type = AcousticModel.class)
    public final static String PROP_ACOUSTIC_MODEL = "acousticModel";
    
    /** The property that defines the unit manager to use when building the search graph */
    @S4Component(type = UnitManager.class, defaultClass = UnitManager.class)
    public final static String PROP_UNIT_MANAGER = "unitManager";
    
    private AcousticModel acousticModel;
    private UnitManager unitManager;
    private Logger logger;
    
    public AllphoneLinguist() {    
        
    }
    
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        acousticModel = (AcousticModel) ps.getComponent(PROP_ACOUSTIC_MODEL);
        unitManager = (UnitManager) ps.getComponent(PROP_UNIT_MANAGER);
    }

    public SearchGraph getSearchGraph() {
        return new AllphoneSearchGraph(acousticModel);
    }

    public void startRecognition() {
    }

    public void stopRecognition() {    }

    public void allocate() throws IOException {
    }

    public void deallocate() throws IOException {
    }

}
