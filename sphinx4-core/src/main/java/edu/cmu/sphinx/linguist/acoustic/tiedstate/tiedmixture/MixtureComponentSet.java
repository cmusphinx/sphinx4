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

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;

/**
 * MixtureComponentsSet - phonetically tied set of gaussians
 */
public class MixtureComponentSet {
    
    private ArrayList<PrunableMixtureComponent[]> components;
    private ArrayList<PrunableMixtureComponent[]> topComponents;
    private int numStreams;
    private int topGauNum;
    private int gauNum;
    private long topGauCalcSampleNumber;
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
        topGauCalcSampleNumber = -1;
        gauCalcSampleNumber = -1;
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
        if (firstSampleNumber != topGauCalcSampleNumber) {
            float[] featureVector = FloatData.toFloatData(feature).getValues();
            updateTopScores(featureVector);
            topGauCalcSampleNumber = firstSampleNumber;
        }
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
    
    public int getTopGauNum() {
        return topGauNum;
    }
    
    public int getGauNum() {
        return gauNum;
    }
    
    public float getTopGauScore(int streamId, int topGauId) {
        return topComponents.get(streamId)[topGauId].getStoredScore();
    }
    
    public int getTopGauId(int streamId, int topGauId) {
        return topComponents.get(streamId)[topGauId].getId();
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

        @Override
        public int compare(PrunableMixtureComponent a, PrunableMixtureComponent b) {
            return (int)(a.getStoredScore() - b.getStoredScore());
        }
    };
    
}
