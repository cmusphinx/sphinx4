/*
 * Copyright 2014 Carnegie Mellon University.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist.acoustic.tiedstate.tiedmixture;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;

/**
 * MixtureComponentsSet - phonetically tied set of gaussians
 */
public class MixtureComponentSet {
    
    private int scoresQueueLen;
    private boolean toStoreScore;
    private LinkedList<MixtureComponentSetScores> storedScores;
    MixtureComponentSetScores curScores;

    private ArrayList<PrunableMixtureComponent[]> components;
    private ArrayList<PrunableMixtureComponent[]> topComponents;
    private int numStreams;
    private int topGauNum;
    private int gauNum;
    private long gauCalcSampleNumber;
    
    public MixtureComponentSet(ArrayList<PrunableMixtureComponent[]> components, int topGauNum) {
        this.components = components;
        this.numStreams = components.size();
        this.topGauNum = topGauNum;
        this.gauNum = components.get(0).length;
        topComponents = new ArrayList<PrunableMixtureComponent[]>();
        for (int i = 0; i < numStreams; i++) {
            PrunableMixtureComponent[] featTopComponents = new PrunableMixtureComponent[topGauNum];
            for (int j = 0; j < topGauNum; j++)
                featTopComponents[j] = components.get(i)[j];
            topComponents.add(featTopComponents);
        }
        gauCalcSampleNumber = -1;
        toStoreScore = false;
        storedScores = new LinkedList<MixtureComponentSetScores>();
        curScores = null;
    }
    
    private void storeScores(MixtureComponentSetScores scores) {
        storedScores.add(scores);
        while(storedScores.size() > scoresQueueLen)
            storedScores.poll();
    }
    
    private MixtureComponentSetScores getStoredScores(long frameFirstSample) {
        if (storedScores.isEmpty())
            return null;
        if (storedScores.peekLast().getFrameStartSample() < frameFirstSample)
            //new frame
            return null;
        for (MixtureComponentSetScores scores : storedScores) {
            if (scores.getFrameStartSample() == frameFirstSample)
                return scores;
        }
        //Failed to find score. Seems it wasn't calculated yet
        return null;
    }
    
    private MixtureComponentSetScores createFromTopGau(long firstFrameSample) {
        MixtureComponentSetScores scores = new MixtureComponentSetScores(numStreams, topGauNum, firstFrameSample);
        for (int i = 0; i < numStreams; i++) {
            for (int j = 0; j < topGauNum; j++) {
                scores.setScore(i, j, topComponents.get(i)[j].getStoredScore());
                scores.setGauId(i, j, topComponents.get(i)[j].getId());
            }
        }
        return scores;
    }
    
    private void insertTopComponent(PrunableMixtureComponent[] topComponents, PrunableMixtureComponent component) {
        int i;
        for (i = 0; i < topComponents.length - 1; i++) {
            if (component.getPartialScore() < topComponents[i].getPartialScore()) {
                topComponents[i - 1] = component;
                return;
            }
            topComponents[i] = topComponents[i + 1];
        }
        if (component.getPartialScore() < topComponents[topComponents.length - 1].getPartialScore())
            topComponents[topComponents.length - 2] = component;
        else
            topComponents[topComponents.length - 1] = component;
    }
    
    private boolean isInTopComponents(PrunableMixtureComponent[] topComponents, PrunableMixtureComponent component) {
        for (PrunableMixtureComponent topComponent : topComponents)
            if (topComponent.getId() == component.getId())
                return true;
        return false;
    }
    
    private void updateTopScores(float[] featureVector) {
        int step = featureVector.length / numStreams;        
        
        float[] streamVector = new float[step];
        for (int i = 0; i < numStreams; i++) {
            System.arraycopy(featureVector, i * step, streamVector, 0, step);
            PrunableMixtureComponent[] featTopComponents = topComponents.get(i);
            PrunableMixtureComponent[] featComponents = components.get(i);
            
            //update scores in top gaussians from previous frame
            for (PrunableMixtureComponent topComponent : featTopComponents)
                topComponent.updateScore(streamVector);
            Arrays.sort(featTopComponents, componentComparator);
            
            //Check if there is any gaussians that should float into top
            float threshold = featTopComponents[0].getPartialScore();    
            for (PrunableMixtureComponent component : featComponents) {
                if (isInTopComponents(featTopComponents, component))
                    continue;
                if (component.isTopComponent(streamVector, threshold)) {
                    insertTopComponent(featTopComponents, component);
                    threshold = featTopComponents[0].getPartialScore();
                }
            }
        }
    }
    
    public void updateTopScores(Data feature) {
        
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");
        
        long firstSampleNumber = FloatData.toFloatData(feature).getFirstSampleNumber();
        if (toStoreScore) {
            curScores = getStoredScores(firstSampleNumber);
        } else {
            if (curScores != null && curScores.getFrameStartSample() != firstSampleNumber)
                curScores = null;
        }
        if (curScores != null)
            //component scores for this frame was already calculated
            return;
        float[] featureVector = FloatData.toFloatData(feature).getValues();
        updateTopScores(featureVector);
        //store just calculated score in list
        curScores = createFromTopGau(firstSampleNumber);
        if (toStoreScore)
            storeScores(curScores);
    }
    
    private void updateScores(float[] featureVector) {
        int step = featureVector.length / numStreams;
        float[] streamVector = new float[step];
        for (int i = 0; i < numStreams; i++) {
            System.arraycopy(featureVector, i * step, streamVector, 0, step);
            for (PrunableMixtureComponent component : components.get(i)) {
                component.updateScore(streamVector);
            }
        }
    }
    
    public void updateScores(Data feature) {
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");
        
        long firstSampleNumber = FloatData.toFloatData(feature).getFirstSampleNumber();
        if (gauCalcSampleNumber != firstSampleNumber) {
            float[] featureVector = FloatData.toFloatData(feature).getValues();
            updateScores(featureVector);
            gauCalcSampleNumber = firstSampleNumber;
        }
    }
    
    /**
     * Should be called on each new utterance to scores for old frames
     */
    public void clearStoredScores() {
        storedScores.clear();
    }
    
    /**
     * How long scores for previous frames should be stored.
     * For fast match this value is lookahead_window_length + 1)
     * @param scoresQueueLen queue length
     */
    public void setScoreQueueLength(int scoresQueueLen) {
        toStoreScore = scoresQueueLen > 0;
        this.scoresQueueLen = scoresQueueLen;
    }
    
    public int getTopGauNum() {
        return topGauNum;
    }
    
    public int getGauNum() {
        return gauNum;
    }
    
    public float getTopGauScore(int streamId, int topGauId) {
        return curScores.getScore(streamId, topGauId);
    }
    
    public int getTopGauId(int streamId, int topGauId) {
        return curScores.getGauId(streamId, topGauId);
    }
    
    public float getGauScore(int streamId, int topGauId) {
        return components.get(streamId)[topGauId].getStoredScore();
    }
    
    public int getGauId(int streamId, int topGauId) {
        return components.get(streamId)[topGauId].getId();
    }
    
    private <T> T[] concatenate (T[] A, T[] B) {
        int aLen = A.length;
        int bLen = B.length;

        @SuppressWarnings("unchecked")
        T[] C = (T[]) Array.newInstance(A.getClass().getComponentType(), aLen+bLen);
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);

        return C;
    }
    
    protected MixtureComponent[] toArray() {
        PrunableMixtureComponent[] allComponents = new PrunableMixtureComponent[0];
        for (int i = 0; i < numStreams; i++)
            concatenate(allComponents, components.get(i));
        return allComponents;
    }
    
    protected int dimension() {
        int dimension = 0;
        for (int i = 0; i < numStreams; i++) {
            dimension+= components.get(i)[0].getMean().length;
        }
        return dimension;
    }

    protected int size() {
        int size = 0;
        for (int i = 0; i < numStreams; i++) {
            size += components.get(0).length;
        }
        return size;
    }
    
    private Comparator<PrunableMixtureComponent> componentComparator = new Comparator<PrunableMixtureComponent>() {

        public int compare(PrunableMixtureComponent a, PrunableMixtureComponent b) {
            return (int)(a.getStoredScore() - b.getStoredScore());
        }
    };
    
}
