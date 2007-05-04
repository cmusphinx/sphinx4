package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A <code>DataProcessor</code> which wraps incoming <code>DoubleData</code>-objects into equally size blocks of defined
 * length.
 */
public class DataBlocker extends BaseDataProcessor {

    /** The SphinxProperty name for the block size of generated data-blocks in milliseconds. */
    public static final String PROP_BLOCK_SIZE_MS = "blockSizeMs";

    /** The default value for PROP_BLOCK_SIZE_MS. */
    public static final float PROP_BLOCK_SIZE_MS_DEFAULT = 10;

    private double blockSizeMs;
    private int blockSizeSamples = Integer.MAX_VALUE;

    private int curFirstSamplePos;
    private int sampleRate = -1;

    private LinkedList<DoubleData> inBuffer = new LinkedList<DoubleData>();

    private int curInBufferSize = 0;


    public DataBlocker() {
    }


    public DataBlocker(double blockSizeMs) {
        this.blockSizeMs = blockSizeMs;
    }


    public Data getData() throws DataProcessingException {
        // collect enough data to be able to create a new data block
        while (curInBufferSize < blockSizeSamples) {
            Data data = getPredecessor().getData();

            if (data instanceof DataStartSignal) {
                sampleRate = ((DataStartSignal) data).getSampleRate();
                blockSizeSamples = (int) Math.round(sampleRate * blockSizeMs / 1000);
            }

            if (!(data instanceof DoubleData)) {
                inBuffer.clear();
                curInBufferSize = 0;
                return data;
            }

            DoubleData dd = (DoubleData) data;

            inBuffer.add(dd);
            curInBufferSize += dd.getValues().length;
        }

        // now we are ready to merge all data blocks into one
        double[] newSampleBlock = new double[blockSizeSamples];

        int copiedSamples = 0;

        long firstSample = inBuffer.get(0).getFirstSampleNumber() + curFirstSamplePos;
        long collectTime = inBuffer.get(0).getCollectTime();

        while (!inBuffer.isEmpty()) {
            DoubleData dd = inBuffer.remove(0);
            double[] values = dd.getValues();
            int copyLength = Math.min(blockSizeSamples - copiedSamples, values.length - curFirstSamplePos);

            System.arraycopy(values, curFirstSamplePos, newSampleBlock, copiedSamples, copyLength);

            // does the current data-object contains more samples than necessary? -> keep the rest for the next block
            if (copyLength < (values.length - curFirstSamplePos)) {
                assert inBuffer.isEmpty();

                curFirstSamplePos += copyLength;
                inBuffer.add(0, dd);
                break;
            } else {
                copiedSamples += copyLength;
                curFirstSamplePos = 0;
            }
        }

        curInBufferSize = inBuffer.isEmpty() ? 0 : inBuffer.get(0).getValues().length - curFirstSamplePos;
        return new DoubleData(newSampleBlock, sampleRate, collectTime, firstSample);
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#getConfigurationInfo()
    */
    public static Map getConfigurationInfo() {
        Map info = new HashMap();
        info.put(new String("PROP_BLOCK_SIZE_MS_TYPE"), new String("DOUBLE"));
        return info;
    }


    public void register(String string, Registry registry) throws PropertyException {
        super.register(string, registry);
        registry.register(PROP_BLOCK_SIZE_MS, PropertyType.DOUBLE);
    }


    public void newProperties(PropertySheet propertySheet) throws PropertyException {
        super.newProperties(propertySheet);
        blockSizeMs = propertySheet.getDouble(PROP_BLOCK_SIZE_MS, PROP_BLOCK_SIZE_MS_DEFAULT);
    }


    public String getName() {
        return this.getClass().getName();
    }
}
