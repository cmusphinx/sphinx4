
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

package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.decoder.linguist.simple.*;
import java.util.AbstractMap;
import java.util.HashMap;

/**
 * A list of ActiveLists.  Different token types are placed in different lists..
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 */
public class SimpleActiveListManager implements ActiveListManager  {

    Class listOrder[] = new Class[6];
    AbstractMap listMap = new HashMap();
    SphinxProperties props;
    int listPtr = 0;
    private class HMMStateStateNE {}
    int absoluteWordBeamWidth;
    float relativeWordBeamWidth;

    /**
     * Creates active lists with properties
     *
     * @param props the sphinx properties
     */
    public SimpleActiveListManager(SphinxProperties props) {
        this.props = props;
        absoluteWordBeamWidth = props.getInt(PROP_ABSOLUTE_WORD_BEAM_WIDTH,
                PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT);
        double linearRelativeWordBeamWidth = props.getFloat(PROP_RELATIVE_WORD_BEAM_WIDTH,
                PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT);

        LogMath logMath = LogMath.getLogMath(props.getContext());
        relativeWordBeamWidth = logMath.linearToLog(linearRelativeWordBeamWidth);

        // Todo: Find an alternative to hardcoding the state class order.
        listOrder[0] = HMMStateState.class;
        listOrder[1] = HMMStateStateNE.class;
        listOrder[2] = BranchState.class;
        listOrder[3] = PronunciationState.class;
        listOrder[4] = ExtendedUnitState.class;
        listOrder[5] = GrammarState.class;
    }

    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     *
     * @return the new active list
     */
    public ActiveListManager createNew() {
        return new SimpleActiveListManager(props);
    }

    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public void add(Token token) {
        Class type;
        if (token.getSearchState() instanceof HMMStateState && !token.isEmitting()) {
            type = HMMStateStateNE.class;
        } else {
            type = token.getSearchState().getClass();
        }
        ActiveList activeList = findListFor(type);
        if (activeList == null) {
            SimpleActiveList simpleActiveList = new SimpleActiveList(props);
            if (!token.isEmitting()) {
                simpleActiveList.absoluteBeamWidth = absoluteWordBeamWidth;
                simpleActiveList.relativeBeamWidth = relativeWordBeamWidth;
            }
            activeList = simpleActiveList;
            listMap.put(type, activeList);
        }

/*        if (token.getSearchState() instanceof HMMStateState) {
        } else if (token.getSearchState() instanceof HMMStateState) {
        } else if (token.getSearchState() instanceof HMMStateStateNE) {
        } else if (token.getSearchState() instanceof BranchState) {
        } else if (token.getSearchState() instanceof PronunciationState) {
        } else if (token.getSearchState() instanceof ExtendedUnitState) {
        } else if (token.getSearchState() instanceof GrammarState) {
         } else {
            int i = 3;
            i = 4;
        } */


        activeList.add(token);
    }

    private ActiveList findListFor(Token token) {
        return findListFor(token.getSearchState().getClass());
    }

    private ActiveList findListFor(Class type) {
        return (ActiveList)listMap.get(type);
    }


    /**
     * Replaces an old token with a new token
     *
     * @param oldToken the token to replace (or null in which case,
     * replace works like add).
     *
     * @param newToken the new token to be placed in the list.
     *
     */
    public void replace(Token oldToken, Token newToken) {
        ActiveList activeList = findListFor(oldToken);
        assert activeList != null;
        activeList.replace(oldToken, newToken);
    }

    public ActiveList getNextList() {
        if (listMap.isEmpty()) {
            return null;
        }
        if (listPtr == 6) {
            listPtr = 0;
        }

        ActiveList activeList = findListFor(listOrder[listPtr++]);
        if (activeList == null) {
            return getNextList();
        }
        listMap.remove(listOrder[listPtr-1]);
        return activeList;
    }
}

