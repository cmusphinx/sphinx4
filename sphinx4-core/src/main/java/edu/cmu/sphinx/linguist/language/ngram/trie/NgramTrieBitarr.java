package edu.cmu.sphinx.linguist.language.ngram.trie;

/**
 * Byte array that keeps ngram trie
 */

public class NgramTrieBitarr {

    private byte[] mem;
    
    public NgramTrieBitarr(int memLen) {
        mem = new byte[memLen];
    }

    /**
     * Getter for byte array, so it can be filled with actual data
     * @return data array
     */
    public byte[] getArr() {
        return mem;
    }

    /**
     * Reads integer from byte array for specified memory pointer, 
     * offset from this pointer and mask of value that is read.
     * @param memPtr - memory pointer for specific ngram order
     * @param bitOffset - offset from memPtr that is calculated 
     *                    according to ngram index and type of value that is read.
     * @param mask - bit mask of value that is read
     * @return requested integer
     */
    public int readInt(int memPtr, int bitOffset, int mask) {
        int idx = memPtr + (bitOffset >> 3);
        int value = mem[idx++] & 0xFF;
        value |= (mem[idx++] << 8) & 0xFFFF;
        value |= (mem[idx++] << 16) & 0xFFFFFF;
        value |= (mem[idx++] << 24) & 0xFFFFFFFF;
        value >>= (bitOffset & 7);
        value &= mask;
        return value;
    }

    /**
     * Reads negative float from byte array for specified memory pointer
     * and offset from this pointer. Used to read uncompressed float, i.e.
     * in case quantation was not applied
     * @param memPtr - memory pointer for specific ngram order
     * @param bitOffset - offset from memPtr
     * @return float that was read from specified position
     */
    public float readNegativeFloat(int memPtr, int bitOffset) {
        //TODO cap
        return 0.0f;
    }

    /**
     * Reads float from byte array for specified memory pointer
     * and offset from this pointer. Used to read uncompressed float, i.e.
     * in case quantation was not applied
     * @param memPtr - memory pointer for specific ngram order
     * @param bitOffset - offset from memPtr
     * @return float that was read from specified position
     */
    public float readFloat(int memPtr, int bitOffset) {
        //TODO cap
        return 0.0f;
    }

}
