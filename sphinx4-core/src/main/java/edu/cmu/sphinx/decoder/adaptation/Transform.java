package edu.cmu.sphinx.decoder.adaptation;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

public class Transform {

    private float[][][][] As;
    private float[][][] Bs;
    private Sphinx3Loader loader;
    private int nrOfClusters;

    public Transform(Sphinx3Loader loader, int nrOfClusters) {
        this.loader = loader;
        this.nrOfClusters = nrOfClusters;
    }

    /**
     * Used for access to A matrix.
     * 
     * @return A matrix (representing A from A*x + B = C)
     */
    public float[][][][] getAs() {
        return As;
    }

    /**
     * Used for access to B matrix.
     * 
     * @return B matrix (representing B from A*x + B = C)
     */
    public float[][][] getBs() {
        return Bs;
    }

    /**
     * Writes the transformation to file in a format that could further be used
     * in Sphinx3 and Sphinx4.
     * 
     * @param filePath
     *            path to store transform matrix
     * @param index
     *            index of transform to store
     * @throws Exception
     *             if something went wrong
     */
    public void store(String filePath, int index) throws Exception {
        PrintWriter writer = new PrintWriter(filePath, "UTF-8");

        // nMllrClass
        writer.println("1");
        writer.println(loader.getNumStreams());

        for (int i = 0; i < loader.getNumStreams(); i++) {
            writer.println(loader.getVectorLength()[i]);

            for (int j = 0; j < loader.getVectorLength()[i]; j++) {
                for (int k = 0; k < loader.getVectorLength()[i]; ++k) {
                    writer.print(As[index][i][j][k]);
                    writer.print(" ");
                }
                writer.println();
            }

            for (int j = 0; j < loader.getVectorLength()[i]; j++) {
                writer.print(Bs[index][i][j]);
                writer.print(" ");

            }
            writer.println();

            for (int j = 0; j < loader.getVectorLength()[i]; j++) {
                writer.print("1.0 ");

            }
            writer.println();
        }
        writer.close();
    }

    /**
     * Used for computing the actual transformations (A and B matrices). These
     * are stored in As and Bs.
     */
    private void computeMllrTransforms(double[][][][][] regLs, double[][][][] regRs) {
        int len;
        DecompositionSolver solver;
        RealMatrix coef;
        RealVector vect, ABloc;

        for (int c = 0; c < nrOfClusters; c++) {
            this.As[c] = new float[loader.getNumStreams()][][];
            this.Bs[c] = new float[loader.getNumStreams()][];

            for (int i = 0; i < loader.getNumStreams(); i++) {
                len = loader.getVectorLength()[i];
                this.As[c][i] = new float[len][len];
                this.Bs[c][i] = new float[len];

                for (int j = 0; j < len; ++j) {
                    coef = new Array2DRowRealMatrix(regLs[c][i][j], false);
                    solver = new LUDecomposition(coef).getSolver();
                    vect = new ArrayRealVector(regRs[c][i][j], false);
                    ABloc = solver.solve(vect);

                    for (int k = 0; k < len; ++k) {
                        this.As[c][i][j][k] = (float) ABloc.getEntry(k);
                    }

                    this.Bs[c][i][j] = (float) ABloc.getEntry(len);
                }
            }
        }
    }

    /**
     * Read the transformation from a file
     * 
     * @param filePath
     *            file path to load transform
     * @throws Exception
     *             if something went wrong
     */
    public void load(String filePath) throws Exception {

        Scanner input = new Scanner(new File(filePath));
        int numStreams, nMllrClass;

        nMllrClass = input.nextInt();

        assert nMllrClass == 1;

        numStreams = input.nextInt();

        this.As = new float[nMllrClass][numStreams][][];
        this.Bs = new float[nMllrClass][numStreams][];

        for (int i = 0; i < numStreams; i++) {
            int length = input.nextInt();

            this.As[0][i] = new float[length][length];
            this.Bs[0][i] = new float[length];

            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    As[0][i][j][k] = input.nextFloat();
                }
            }
            for (int j = 0; j < length; j++) {
                Bs[0][i][j] = input.nextFloat();
            }
            for (int j = 0; j < length; j++) {
                // Skip MLLR variance scale
                input.nextFloat();
            }
        }
        input.close();
    }

    /**
     * Stores in current object a transform generated on the provided stats.
     * 
     * @param stats
     *            provided stats that were previously collected from Result
     *            objects.
     */
    public void update(Stats stats) {
        stats.fillRegLowerPart();
        As = new float[nrOfClusters][][][];
        Bs = new float[nrOfClusters][][];
        this.computeMllrTransforms(stats.getRegLs(), stats.getRegRs());
    }
}
