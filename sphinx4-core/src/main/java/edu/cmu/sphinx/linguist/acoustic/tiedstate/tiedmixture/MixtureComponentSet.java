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
import edu.cmu.sphinx.util.LogMath;

/**
 * MixtureComponentsSet - phonetically tied set of gaussians
 */
public class MixtureComponentSet {
    
    //TODO make configurable
    private static final int TOP_GAUSSIAN_NUM = 4;
    
    private ArrayList<PrunableMixtureComponent[]> components;
    private ArrayList<PrunableMixtureComponent[]> topComponents;
    private int numStreams;
    private int gaussianPerStream;
    private LogMath logMath;
    private long curFirstSampleNumber;
    
    public MixtureComponentSet(ArrayList<PrunableMixtureComponent[]> components) {
        this.components = components;
        this.numStreams = components.size();
        topComponents = new ArrayList<PrunableMixtureComponent[]>();
        for (int i = 0; i < numStreams; i++) {
            PrunableMixtureComponent[] featTopComponents = new PrunableMixtureComponent[TOP_GAUSSIAN_NUM];
            for (int j = 0; j < TOP_GAUSSIAN_NUM; j++)
                featTopComponents[j] = components.get(i)[j];
            topComponents.add(featTopComponents);
        }
        gaussianPerStream = components.get(0).length;
        logMath = LogMath.getLogMath();
        curFirstSampleNumber = -1;
    }
    
    private float applyWeights(float[] logMixtureWeights) {
        float ascore = 0;
        for (int i = 0; i < numStreams; i++) {
            float logTotal = LogMath.LOG_ZERO;
            PrunableMixtureComponent[] featTopComponents = topComponents.get(i);
            for (PrunableMixtureComponent component : featTopComponents) {
                logTotal = logMath.addAsLinear(logTotal,
                        component.getStoredScore() + logMixtureWeights[gaussianPerStream * i + component.getId()]);
            }
            ascore += logTotal;
        }
        return ascore;
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
    
    private void updateScores(float[] featureVector) {
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
    
    public float calculateScore(Data feature, float[] logMixtureWeights) {
        
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");
        
        long firstSampleNumber = FloatData.toFloatData(feature).getFirstSampleNumber();
        if (firstSampleNumber != curFirstSampleNumber) {
            float[] featureVector = FloatData.toFloatData(feature).getValues();
            updateScores(featureVector);
            curFirstSampleNumber = firstSampleNumber;
        }
        
        return applyWeights(logMixtureWeights);
    }
    
    public float[] calculateComponentScore(float[] featureVector, float[] logMixtureWeights) {
        float[] scores = new float[size()];
        int step = featureVector.length / numStreams;
        int scoreIdx = 0;
        float[] streamVector = new float[step];
        for (int i = 0; i < numStreams; i++) {
            System.arraycopy(featureVector, i * step, streamVector, 0, step);
            for (int j = 0; j < components.get(i).length; j++) {
                scores[scoreIdx] = components.get(i)[j].getScore(streamVector) + logMixtureWeights[scoreIdx];
                scoreIdx++;
            }
        }
        return scores;
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
