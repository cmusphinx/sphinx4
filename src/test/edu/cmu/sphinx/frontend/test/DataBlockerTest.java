package edu.cmu.sphinx.frontend.test;

import edu.cmu.sphinx.frontend.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Some small tests which ensure that the <code>DataBlocker</code> works properly. */
public class DataBlockerTest extends BaseDataProcessor {

    private List<Data> input;


    @Before
    public void setUp() {
        input = new ArrayList<Data>();
    }


    @Test
    public void testLongInput() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));

        input.addAll(createDataInput(1000, 1000, sampleRate, 0)); // create one second of data sampled with 1kHz
        input.add(new DataEndSignal(0));

        assertTrue(hasIncreasingOrder(collectOutput(100), 1000));
    }


    @Test
    public void testUsualInput() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));

        input.addAll(createDataInput(600, 120, sampleRate, 0));
        input.add(new DataEndSignal(0));

        List<Data> output = collectOutput(100);

        assertEquals(output.size(), 6);
        assertEquals(201, ((DoubleData) output.get(2)).getFirstSampleNumber());
        assertTrue(hasIncreasingOrder(output, 600));
    }


    @Test
    public void skipLastSamples() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));
        input.addAll(createDataInput(500, 500, sampleRate, 0));
        input.addAll(createDataInput(300, 300, sampleRate, 500));
        input.add(new DataEndSignal(0));

        List<Data> output = collectOutput(250);

        assertEquals(output.size(), 3);
        assertEquals(501, ((DoubleData) output.get(2)).getFirstSampleNumber());
        assertTrue(hasIncreasingOrder(output, 750));
    }


    /**
     * Returns the processed Data output.
     *
     * @return an Data object that has been processed by this DataProcessor
     * @throws edu.cmu.sphinx.frontend.DataProcessingException
     *          if a data processor error occurs
     */
    public Data getData() throws DataProcessingException {
        return input.remove(0);
    }


    public List<Data> collectOutput(double blocSizeMs) throws DataProcessingException {
        DataBlocker dataBlocker = new DataBlocker(blocSizeMs);
        dataBlocker.setPredecessor(this);

        List<Data> output = new ArrayList<Data>();

        while (true) {
            Data d = dataBlocker.getData();
            if (d instanceof DoubleData)
                output.add(d);
            if (d instanceof DataEndSignal)
                return output;
        }
    }


    public static List<DoubleData> createDataInput(int numSamples, int blockSize, int sampleRate, int offSet) {
        List<DoubleData> datas = new ArrayList<DoubleData>();

        double counter = 1;
        for (int i = 0; i < numSamples / blockSize; i++) {
            double[] values = new double[blockSize];
            datas.add(new DoubleData(values, sampleRate, 0, (long) counter + offSet));

            for (int j = 0; j < values.length; j++)
                values[j] = counter++ + offSet;
        }

        return datas;
    }


    /**
     * Tests whether the samples of all <code>Data</code>s in the list are ordered in increasing order with +1
     * increments.
     */
    public static boolean hasIncreasingOrder(List<Data> output, int lastValue) {
        int dataCounter = 0;

        for (Data data : output) {
            if (data instanceof DoubleData) {
                DoubleData dd = (DoubleData) data;

                for (int i = 0; i < dd.getValues().length; i++) {
                    if ((dataCounter + 1) == dd.getValues()[i])
                        dataCounter++;
                    else
                        return false;
                }
            }
        }

        return dataCounter == lastValue;
    }
}
