package edu.cmu.sphinx.linguist.allphone;

import java.util.ArrayList;
import java.util.Iterator;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.util.LogMath;

public class PhoneHmmSearchState implements SearchState, SearchStateArc {

    private Unit unit;
    private HMMState state;
    private AcousticModel acousticModel;
    
    public PhoneHmmSearchState(Unit unit, HMMState hmmState, AcousticModel model) {
        this.unit = unit;
        this.state = hmmState;
        this.acousticModel = model;
    }

    public SearchState getState() {
        return null;
    }

    public float getProbability() {
        return LogMath.LOG_ONE;
    }

    public float getLanguageProbability() {
        return LogMath.LOG_ONE;
    }

    public float getInsertionProbability() {
        return LogMath.LOG_ONE;
    }

    /* If we are final, transfer to all possible phones, otherwise
     * return all successors of this hmm state.
     * */
    public SearchStateArc[] getSuccessors() {
        if (state.isExitState()) {
            ArrayList<SearchStateArc> result = new ArrayList<SearchStateArc>();
            Iterator<Unit> iter = acousticModel.getContextIndependentUnitIterator();
            while( iter.hasNext()) {
                result.add(new PhoneSearchState(iter.next(), acousticModel));
            }
            return result.toArray(new SearchStateArc[result.size()]);
        } else {
            HMMStateArc successors[] = state.getSuccessors();
            SearchStateArc[] results = new SearchStateArc[successors.length];
            for (int i = 0; i < successors.length; i++) {
                results[i] = new PhoneHmmSearchState(unit, successors[i].getHMMState(), acousticModel);
            }
            return results;
        }
    }

    public boolean isEmitting() {
        return state.isEmitting();
    }

    public boolean isFinal() {
        return true;
    }

    public String toPrettyString() {
        return "HMM " + state.toString();
    }

    public String getSignature() {
        return null;
    }

    public WordSequence getWordHistory() {
        return null;
    }

    public Object getLexState() {
        return null;
    }

    public int getOrder() {
        return 1;
    }
}
