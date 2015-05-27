package edu.cmu.sphinx.linguist.allphone;

import java.util.ArrayList;

import edu.cmu.sphinx.decoder.scorer.ScoreProvider;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneSequence;

public class PhoneHmmSearchState implements SearchState, SearchStateArc, ScoreProvider {

    private HMMState state;
    private AllphoneLinguist linguist;
    
    private float insertionProb;
    private float languageProb;
    
    public PhoneHmmSearchState(HMMState hmmState, AllphoneLinguist linguist, float insertionProb, float languageProb) {
        this.state = hmmState;
        this.linguist = linguist;
        this.insertionProb = insertionProb;
        this.languageProb = languageProb;
    }

    public SearchState getState() {
        return this;
    }
    
    public int getBaseId() {
        return ((SenoneHMM)state.getHMM()).getBaseUnit().getBaseID();
    }

    public float getProbability() {
        return getLanguageProbability() + getInsertionProbability();
    }

    public float getLanguageProbability() {
        return languageProb;
    }

    public float getInsertionProbability() {
        return insertionProb;
    }

    /* If we are final, transfer to all possible phones, otherwise
     * return all successors of this hmm state.
     * */
    public SearchStateArc[] getSuccessors() {
        if (state.isExitState()) {
            ArrayList<Unit> units = linguist.getUnits(((SenoneHMM)state.getHMM()).getSenoneSequence());
            SearchStateArc[] result = new SearchStateArc[units.size()];
            for (int i = 0; i < result.length; i++)
                result[i] = new PhoneNonEmittingSearchState(units.get(i), linguist, insertionProb, languageProb);
            return result;
        } else {
            HMMStateArc successors[] = state.getSuccessors();
            SearchStateArc[] results = new SearchStateArc[successors.length];
            for (int i = 0; i < successors.length; i++) {
                results[i] = new PhoneHmmSearchState(successors[i].getHMMState(), linguist, insertionProb, languageProb);
            }
            return results;
        }
    }

    public boolean isEmitting() {
        return state.isEmitting();
    }

    public boolean isFinal() {
        return false;
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
        return 2;
    }

    public float getScore(Data data) {
        return state.getScore(data);
    }

    public float[] getComponentScore(Data feature) {
        return state.calculateComponentScore(feature);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PhoneHmmSearchState))
            return false;
        SenoneSequence otherSenoneSeq = ((SenoneHMM)((PhoneHmmSearchState)obj).state.getHMM()).getSenoneSequence();
        SenoneSequence thisSenoneSeq = ((SenoneHMM)state.getHMM()).getSenoneSequence();
        return thisSenoneSeq.equals(otherSenoneSeq);
    }

    @Override
    public int hashCode() {
        return ((SenoneHMM)state.getHMM()).getSenoneSequence().hashCode() + state.getState() * 37;
    }
}
