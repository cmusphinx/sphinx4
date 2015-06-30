package edu.cmu.sphinx.linguist.language.ngram.trie;

/**
 * Class for ngram weights quantation.
 * Stores quantation tables for each ngram order for probabilities and backoffs,
 * while ngrams store pointers to this tables. 
 * Size of table is specified by quantation parameter, for default QUANT_16 each 
 * weight has 65k entries table.
 */

public class NgramTrieQuant {

    public static enum QuantType {NO_QUANT, QUANT_16};

    private int probBits;
    private int backoffBits;

    private int probMask;
    private int backoffMask;
    private float[][] tables;

    private QuantType quantType;

    public NgramTrieQuant(int order, QuantType quantType) {
        switch (quantType) {
        case NO_QUANT:
            return; //nothing to do here
        case QUANT_16:
            probBits = 16;
            backoffBits = 16;
            probMask = (1 << probBits) - 1;
            backoffMask = (1 << backoffBits) - 1;
            break;
        default:
            throw new Error("Unsupported quantization type: " + quantType);
        }
        tables = new float[(order - 1) * 2 - 1][];
        this.quantType = quantType;
    }

    /**
     * Setter that is used during quantation reading
     * @param table - array of weights to be used as quantation table
     * @param order - ngrams order which quantation table corresponds to
     * @param isProb - specifies if provided table is for probability (backoffs otherwise)
     */
    public void setTable(float[] table, int order, boolean isProb) {
        int index = (order - 2) * 2;
        if (!isProb) index++;
        tables[index] = table;
    }

    /**
     * Getter for length of probability quantation table.
     * @return length of quantation table.
     */
    public int getProbTableLen() {
        return 1 << probBits;
    }

    /**
     * Getter for length of backoffs quantation table.
     * @return length of quantation table
     */
    public int getBackoffTableLen() {
        return 1 << backoffBits;
    }

    /**
     * Getter for size of quantaized weights in ngram trie.
     * @return amount of bits required to store quantized probability and backoff
     */
    public int getProbBoSize() {
        switch (quantType) {
        case NO_QUANT:
            return 63;
        case QUANT_16:
            return 32; //16 bits for prob + 16 bits for bo
        //TODO implement different quantization stages
        default:
            throw new Error("Unsupported quantization type: " + quantType);
        }
    }

    /**
     * Getter for size of quantaized weight in ngram trie
     * @return amount of bits required to store quantized probability
     */
    public int getProbSize() {
        switch (quantType) {
        case NO_QUANT:
            return 31;
        case QUANT_16:
            return 16; //16 bits for probs
        //TODO implement different quantization stages
        default:
            throw new Error("Unsupported quantization type: " + quantType);
        }
    }

    /**
     * Returns actual weight value for specified table and encoded weight from trie
     * @param tableIdx - index of table to look in. Is calculated
     * @param encodedVal - encoded weight from trie
     * @return actual weight
     */
    private float binsDecode(int tableIdx, int encodedVal) {
        return tables[tableIdx][encodedVal];
    }

    /**
     * Reads encoded probability from provided trie bit array and decodes it into actual value
     * for specific ngram
     * @param bitArr - trie bit array 
     * @param memPtr - memory pointer for specific ngram order
     * @param bitOffset - offset from memPtr that is calculated according to ngram index
     * @param orderMinusTwo - order of ngram minus two
     * @return probability of ngram
     */
    public float readProb(NgramTrieBitarr bitArr, int memPtr, int bitOffset, int orderMinusTwo) {
        switch (quantType) {
        case NO_QUANT:
            return bitArr.readNegativeFloat(memPtr, bitOffset);
        case QUANT_16:
            int tableIdx = orderMinusTwo * 2;
            if (tableIdx < tables.length - 1)
                bitOffset += backoffBits;
            return binsDecode(tableIdx, bitArr.readInt(memPtr, bitOffset, backoffMask));
        //TODO implement different quantization stages
        default:
            throw new Error("Unsupported quantization type: " + quantType);
        }
    }

    /**
     * Reads encoded backoff from provided trie bit array and decodes it into actual value
     * for specific ngram
     * @param bitArr - trie bit array 
     * @param memPtr - memory pointer for specific ngram order
     * @param bitOffset - offset from memPtr that is calculated according to ngram index
     * @param orderMinusTwo - order of ngram minus two
     * @return backoffs of ngram
     */
    public float readBackoff(NgramTrieBitarr bitArr, int memPtr, int bitOffset, int orderMinusTwo) {
        switch (quantType) {
        case NO_QUANT:
            bitOffset += 31;
            return bitArr.readFloat(memPtr, bitOffset);
        case QUANT_16:
            int tableIdx = orderMinusTwo * 2 + 1;
            return binsDecode(tableIdx, bitArr.readInt(memPtr, bitOffset, probMask));
        //TODO implement different quantization stages
        default:
            throw new Error("Unsupported quantization type: " + quantType);
        }
    }

}
