package edu.cmu.sphinx.decoder.adaptation;

import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.util.LogMath;

/**
 * This class is used for estimating a MLLR transform for each cluster of data.
 * The clustering must be previously performed using
 * ClusteredDensityFileData.java
 */
public class Stats {

    // Minimum number of frames to perform estimation

    private static final int MIN_FRAMES = 300;
    private ClusteredDensityFileData means;
    private double[][][][][] regLs;
    private double[][][][] regRs;
    private int nClusters;
    private Sphinx3Loader loader;
    private float varFlor;
    private LogMath logMath = LogMath.getLogMath();
    private int nFrames;

    public Stats(Loader loader, ClusteredDensityFileData means) {
        this.loader = (Sphinx3Loader) loader;
        this.nClusters = means.getNumberOfClusters();
        this.means = means;
        this.varFlor = 1e-5f;
        this.invertVariances();
        this.init();
        this.nFrames = 0;
    }

    private void init() {
        int len = loader.getVectorLength()[0];
        this.regLs = new double[nClusters][][][][];
        this.regRs = new double[nClusters][][][];

        for (int i = 0; i < nClusters; i++) {
            this.regLs[i] = new double[loader.getNumStreams()][][][];
            this.regRs[i] = new double[loader.getNumStreams()][][];

            for (int j = 0; j < loader.getNumStreams(); j++) {
                len = loader.getVectorLength()[j];
                this.regLs[i][j] = new double[len][len + 1][len + 1];
                this.regRs[i][j] = new double[len][len + 1];
            }
        }
    }

    public ClusteredDensityFileData getClusteredData() {
        return this.means;
    }

    public double[][][][][] getRegLs() {
        return regLs;
    }

    public double[][][][] getRegRs() {
        return regRs;
    }

    /**
     * Used for inverting variances.
     */
    private void invertVariances() {

        for (int i = 0; i < loader.getNumStates(); i++) {
            for (int k = 0; k < loader.getNumGaussiansPerState(); k++) {
                for (int l = 0; l < loader.getVectorLength()[0]; l++) {
                    if (loader.getVariancePool().get(i * loader.getNumGaussiansPerState() + k)[l] <= 0.) {
                        this.loader.getVariancePool().get(i * loader.getNumGaussiansPerState() + k)[l] = (float) 0.5;
                    } else if (loader.getVariancePool().get(i * loader.getNumGaussiansPerState() + k)[l] < varFlor) {
                        this.loader.getVariancePool().get(i * loader.getNumGaussiansPerState() + k)[l] = (float) (1. / varFlor);
                    } else {
                        this.loader.getVariancePool().get(i * loader.getNumGaussiansPerState() + k)[l] = (float) (1. / loader
                                .getVariancePool().get(i * loader.getNumGaussiansPerState() + k)[l]);
                    }
                }
            }
        }
    }

    /**
     * Computes posterior values for the each component.
     * 
     * @param componentScores
     *            from which the posterior values are computed.
     * @param numStreams
     *            Number of feature streams
     * @return posterior values for all components.
     */
    private float[] computePosterios(float[] componentScores, int numStreams) {
        float[] posteriors = componentScores;

        int step = componentScores.length / numStreams;
        int startIdx = 0;
        for (int i = 0; i < numStreams; i++) {
            float max = posteriors[startIdx];
            for (int j = startIdx + 1; j < startIdx + step; j++) {
                if (posteriors[j] > max) {
                    max = posteriors[j];
                }
            }

            for (int j = startIdx; j < startIdx + step; j++) {
                posteriors[j] = (float) logMath.logToLinear(posteriors[j] - max);
            }
            startIdx += step;
        }

        return posteriors;
    }

    /**
     * This method is used for directly collect and use counts. The counts are
     * collected and stored separately for each cluster.
     * 
     * @param result
     *            Result object to collect counts from.
     * @throws Exception
     *             if something went wrong
     */
    public void collect(SpeechResult result) throws Exception {
        Token token = result.getResult().getBestToken();
        float[] componentScore, featureVector, posteriors, tmean;
        int[] len;
        float dnom, wtMeanVar, wtDcountVar, wtDcountVarMean, mean;
        int mId, cluster;
        int numStreams, gauPerState;

        if (token == null)
            throw new Exception("Best token not found!");

        do {
            FloatData feature = (FloatData) token.getData();
            SearchState ss = token.getSearchState();

            if (!(ss instanceof HMMSearchState && ss.isEmitting())) {
                token = token.getPredecessor();
                continue;
            }
            nFrames++;

            componentScore = token.calculateComponentScore(feature);
            featureVector = FloatData.toFloatData(feature).getValues();
            mId = (int) ((HMMSearchState) token.getSearchState()).getHMMState().getMixtureId();
            if (loader instanceof Sphinx3Loader && ((Sphinx3Loader) loader).hasTiedMixtures())
                // use CI phone ID for tied mixture model
                mId = ((Sphinx3Loader) loader).getSenone2Ci()[mId];
            len = loader.getVectorLength();
            numStreams = loader.getNumStreams();
            gauPerState = loader.getNumGaussiansPerState();
            posteriors = this.computePosterios(componentScore, numStreams);
            int featVectorStartIdx = 0;

            for (int i = 0; i < numStreams; i++) {
                for (int j = 0; j < gauPerState; j++) {

                    cluster = means.getClassIndex(mId * numStreams * gauPerState + i * gauPerState + j);
                    dnom = posteriors[i * gauPerState + j];
                    if (dnom > 0.) {
                        tmean = loader.getMeansPool().get(mId * numStreams * gauPerState + i * gauPerState + j);

                        for (int k = 0; k < len[i]; k++) {
                            mean = posteriors[i * gauPerState + j] * featureVector[k + featVectorStartIdx];
                            wtMeanVar = mean
                                    * loader.getVariancePool().get(mId * numStreams * gauPerState + i * gauPerState + j)[k];
                            wtDcountVar = dnom
                                    * loader.getVariancePool().get(mId * numStreams * gauPerState + i * gauPerState + j)[k];

                            for (int p = 0; p < len[i]; p++) {
                                wtDcountVarMean = wtDcountVar * tmean[p];

                                for (int q = p; q < len[i]; q++) {
                                    regLs[cluster][i][k][p][q] += wtDcountVarMean * tmean[q];
                                }
                                regLs[cluster][i][k][p][len[i]] += wtDcountVarMean;
                                regRs[cluster][i][k][p] += wtMeanVar * tmean[p];
                            }
                            regLs[cluster][i][k][len[i]][len[i]] += wtDcountVar;
                            regRs[cluster][i][k][len[i]] += wtMeanVar;
                        }
                    }
                }
                featVectorStartIdx += len[i];
            }
            token = token.getPredecessor();
        } while (token != null);
    }

    /**
     * Fill lower part of Legetter's set of G matrices.
     */
    public void fillRegLowerPart() {
        for (int i = 0; i < this.nClusters; i++) {
            for (int j = 0; j < loader.getNumStreams(); j++) {
                for (int l = 0; l < loader.getVectorLength()[j]; l++) {
                    for (int p = 0; p <= loader.getVectorLength()[j]; p++) {
                        for (int q = p + 1; q <= loader.getVectorLength()[j]; q++) {
                            regLs[i][j][l][q][p] = regLs[i][j][l][p][q];
                        }
                    }
                }
            }
        }
    }

    public Transform createTransform() {
        if (nFrames < MIN_FRAMES * nClusters) {
            return null;
        }
        Transform transform = new Transform(loader, nClusters);
        transform.update(this);
        return transform;
    }

    public int getFrames() {
        return nFrames;
    }
}
