package edu.cmu.sphinx.linguist.language.ngram.trie;

import edu.cmu.sphinx.linguist.language.ngram.trie.NgramTrieModel.TrieRange;

/**
 * Trie structure that contains ngrams of order 2+ in reversed order.
 * Ngrams are stored in bit array for space efficiency.
 */

public class NgramTrie {

    private MiddleNgramSet[] middles;
    private LongestNgramSet longest;
    private NgramTrieBitarr bitArr;
    private int ordersNum;
    private int quantProbBoLen;
    private int quantProbLen;

    public NgramTrie(int[] counts, int quantProbBoLen, int quantProbLen) {
        int memLen = 0;
        int[] ngramMemSize = new int[counts.length - 1];
        for (int i = 1; i <= counts.length - 1; i++) {
            int entryLen = requiredBits(counts[0]);
            if (i == counts.length - 1) {
                //longest ngram
                entryLen += quantProbLen;
            } else {
                //middle ngram
                entryLen += requiredBits(counts[i + 1]);
                entryLen += quantProbBoLen;
            }
            // Extra entry for next pointer at the end.  
            // +7 then / 8 to round up bits and convert to bytes
            // +8 (or +sizeof(uint64))so that reading bit array doesn't exceed bounds 
            // Note that this waste is O(order), not O(number of ngrams).
            int tmpLen = ((1 + counts[i]) * entryLen + 7) / 8 + 8; 
            ngramMemSize[i - 1] = tmpLen;
            memLen += tmpLen;
        }
        bitArr = new NgramTrieBitarr(memLen);
        this.quantProbLen = quantProbLen;
        this.quantProbBoLen = quantProbBoLen;
        middles = new MiddleNgramSet[counts.length - 2];
        int[] startPtrs = new int[counts.length - 2];
        int startPtr = 0;
        for (int i = 0; i < counts.length - 2; i++) {
            startPtrs[i] = startPtr;
            startPtr += ngramMemSize[i];
        }
        // Crazy backwards thing so we initialize using pointers to ones that have already been initialized
        for (int i = counts.length - 1; i >= 2; --i) {
            middles[i - 2] = new MiddleNgramSet(startPtrs[i - 2], quantProbBoLen, counts[i-1], counts[0], counts[i]);
        }
        longest = new LongestNgramSet(startPtr, quantProbLen, counts[0]);
        ordersNum = middles.length + 1;
    }

    /**
     * Getter for allocated byte array to which trie is mapped
     * @return byte[] with ngram trie 
     */
    public byte[] getMem() {
        return bitArr.getArr();
    }

    /**
     * Finds ngram index which corresponds to ngram with specified wordId.
     * Search is performed in specified range. 
     * Fills range with ngram successors if ngram was found, makes range invalid otherwise.
     * @param ngramSet - set of ngrams of certain order to look in
     * @param wordId - word id to look for
     * @param range - range to look in. range contains ngram successors or is invalid after method usage.
     * @return ngram index that can be converted into byte offset if ngram was found, -1 otherwise
     */
    private int findNgram(NgramSet ngramSet, int wordId, TrieRange range) {
        int ptr;
        range.begin--;
        if ((ptr = uniformFind(ngramSet, range, wordId)) < 0) {
            range.setFound(false);
            return -1;
        }
        //read next order ngrams for future searches
        if (ngramSet instanceof MiddleNgramSet)
            ((MiddleNgramSet)ngramSet).readNextRange(ptr, range);
        return ptr;
    }

    /**
     * Finds ngram of cerain order in specified range and reads it's backoff.
     * Range contains ngram successors after function execution.
     * If ngram is not found, range will be invalid.
     * @param wordId - word id to look for
     * @param orderMinusTwo - order of ngram minus two
     * @param range - range to look in, contains ngram successors after function execution
     * @param quant - quantation object to decode compressed backoff stored in trie
     * @return backoff of ngram
     */
    public float readNgramBackoff(int wordId, int orderMinusTwo, TrieRange range, NgramTrieQuant quant) {
        int ptr;
        NgramSet ngram = getNgram(orderMinusTwo);
        if ((ptr = findNgram(ngram, wordId, range)) < 0)
            return 0.0f;
        return quant.readBackoff(bitArr, ngram.memPtr, ngram.getNgramWeightsOffset(ptr), orderMinusTwo);
    }

    /**
     * Finds ngram of cerain order in specified range and reads it's probability.
     * Range contains ngram successors after function execution.
     * If ngram is not found, range will be invalid.
     * @param wordId - word id to look for
     * @param orderMinusTwo - order of ngram minus two
     * @param range - range to look in, contains ngram successors after function execution
     * @param quant - quantation object to decode compressed probability stored in trie
     * @return probability of ngram
     */
    public float readNgramProb(int wordId, int orderMinusTwo, TrieRange range, NgramTrieQuant quant) {
        int ptr;
        NgramSet ngram = getNgram(orderMinusTwo);
        if ((ptr = findNgram(ngram, wordId, range)) < 0)
            return 0.0f;
        return quant.readProb(bitArr, ngram.memPtr, ngram.getNgramWeightsOffset(ptr), orderMinusTwo);
    }

    /**
     * Calculates pivot for binary search
     */
    private int calculatePivot(int offset, int range, int width) {
    	return (int)(((long)offset * width) / (range + 1));
    }

    /**
     * Searches ngram index for given wordId in provided range 
     */
    private int uniformFind(NgramSet ngram, TrieRange range, int wordId) {
    	TrieRange vocabRange = new TrieRange(0, ngram.maxVocab);
        while (range.getWidth() > 1) {
            int pivot = range.begin + 1 + calculatePivot(wordId - vocabRange.begin, vocabRange.getWidth(), range.getWidth() - 1);
            int mid = ngram.readNgramWord(pivot);
            if (mid < wordId) {
                range.begin = pivot;
                vocabRange.begin = mid;
            } else if (mid > wordId){
                range.end = pivot;
                vocabRange.end = mid;
            } else {
                return pivot;
            }
        }
        return -1;
    }

    /**
     * Getter for ngram set by ngram order
     */
    private NgramSet getNgram(int orderMinusTwo) {
        if (orderMinusTwo == ordersNum - 1)
            return longest;
        return middles[orderMinusTwo];
    }

    /**
     * Calculates minimum amount of bits to store provided int
     */
    private int requiredBits(int maxValue) {
        if (maxValue == 0) return 0;
        int res = 1;
        while ((maxValue >>= 1) != 0) res++;
        return res;
    }

    /**
     * Gives access to set of ngram of certain order (trie layer)
     */
    abstract class NgramSet {
        int memPtr;
        int wordBits;
        int wordMask;
        int totalBits;
        int insertIdx;
        int maxVocab;
        NgramSet(int memPtr, int maxVocab, int remainingBits) {
            this.maxVocab = maxVocab;
            this.memPtr = memPtr;
            wordBits = requiredBits(maxVocab);
            if (wordBits > 25)
                throw new Error("Sorry, word indices more than" + (1 << 25) + " are not implemented");
            totalBits = wordBits + remainingBits;
            wordMask = (1 << wordBits) - 1;
            insertIdx = 0;
        }

        int readNgramWord(int ngramIdx) {
            int offset = ngramIdx * totalBits;
            return bitArr.readInt(memPtr, offset, wordMask);
        }

        int getNgramWeightsOffset(int ngramIdx) {
            return ngramIdx * totalBits + wordBits;
        }

        abstract int getQuantBits();

    }

    /**
     * Implementation of NgramSet for ngrams of order [2...Max Ngram Order - 1]
     */
    class MiddleNgramSet extends NgramSet {
        int nextMask;
        int nextOrderMemPtr;
        MiddleNgramSet(int memPtr, int quantBits, int entries, int maxVocab, int maxNext) {
            super(memPtr, maxVocab, quantBits + requiredBits(maxNext));
            nextMask = (1 << requiredBits(maxNext)) - 1;
            if (entries + 1 >= (1 << 25) || (maxNext >= (1 << 25)))
                throw new Error("Sorry, current implementation doesn't support more than " + (1 << 25) + " n-grams of particular order");
        }

        void readNextRange(int ngramIdx, TrieRange range) {
            int offset = ngramIdx * totalBits;
            offset += wordBits;
            offset += getQuantBits();
            range.begin = bitArr.readInt(memPtr, offset, nextMask);
            offset += totalBits;
            range.end = bitArr.readInt(memPtr, offset, nextMask);
        }

        @Override
        int getQuantBits() {
            return quantProbBoLen;
        }
    }

    /**
     * Implementation of NgramSet for ngrams of maximum order
     */
    class LongestNgramSet extends NgramSet {
        LongestNgramSet(int memPtr, int quantBits, int maxVocab) {
            super(memPtr, maxVocab, quantBits);
        }

        @Override
        int getQuantBits() {
            return quantProbLen;
        }
    }

}
