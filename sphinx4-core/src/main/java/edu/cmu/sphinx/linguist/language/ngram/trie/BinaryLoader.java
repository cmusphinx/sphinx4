package edu.cmu.sphinx.linguist.language.ngram.trie;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import edu.cmu.sphinx.linguist.language.ngram.trie.NgramTrieModel.TrieUnigram;
import edu.cmu.sphinx.util.Utilities;

/**
 * Class that provides utils to load NgramTrieModel
 * from binary file created with sphinx_lm_convert.
 * Routines should be called in certain order according to format:
 * <ul>
 *     <li>verifyHeader</li>
 *     <li>readCounts</li>
 *     <li>readQuant</li>
 *     <li>readUnigrams</li>
 *     <li>readTrieByteArr</li>
 *     <li>readWords</li>
 * </ul>
 */

public class BinaryLoader {

    private static final String TRIE_HEADER = "Trie Language Model";

    private DataInputStream inStream;

    public BinaryLoader(File location) throws IOException {
        inStream = new DataInputStream(new FileInputStream(location));
    }

    private void loadModelData(InputStream stream) throws IOException {
	DataInputStream dataStream = new DataInputStream(new BufferedInputStream(stream));
	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	byte[] buffer = new byte[4096];
	while (true) {
	    if (dataStream.read(buffer) < 0)
		break;
	    bytes.write(buffer);
	}
	inStream = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    public BinaryLoader(URL location) throws IOException {
        loadModelData(location.openStream());
    }

    /**
     * Reads header from stream and checks if it matches trie header
     * @throws IOException if reading from stream failed
     */
    public void verifyHeader() throws IOException {
        String readHeader = readString(inStream, TRIE_HEADER.length());
        if (!readHeader.equals(TRIE_HEADER)) {
            throw new Error("Bad binary LM file header: " + readHeader);
        }
    }

    /**
     * Reads language model order and ngram counts
     * @return array of counts where ordinal number is ngram order
     * @throws IOException if reading from stream failed
     */
    public int[] readCounts() throws IOException {
        int order = readOrder();
        int[] counts = new int[order];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = Utilities.readLittleEndianInt(inStream);
        }
        return counts;
    }

    /**
     * Reads weights quantation object from stream
     * @param order - max order of ngrams for this model
     * @return quantation object, see {@link NgramTrieQuant} 
     * @throws IOException if reading from stream failed
     */
    public NgramTrieQuant readQuant(int order) throws IOException {
        int quantTypeInt = Utilities.readLittleEndianInt(inStream);
        if (quantTypeInt < 0 || quantTypeInt >= NgramTrieQuant.QuantType.values().length)
            throw new Error("Unknown quantatization type: " + quantTypeInt);
        NgramTrieQuant.QuantType quantType = NgramTrieQuant.QuantType.values()[quantTypeInt];
        NgramTrieQuant quant = new NgramTrieQuant(order, quantType);
        //reading tables
        for (int i = 2; i <= order; i++) {
            quant.setTable(readFloatArr(quant.getProbTableLen()), i, true);
            if (i < order)
                quant.setTable(readFloatArr(quant.getBackoffTableLen()), i, false);
        }
        return quant;
    }

    /**
     * Reads array of language model unigrams 
     * @param count - amount of unigrams according to counts previously read
     * @return array of language model unigrams, see {@link NgramTrieModel.TrieUnigram}
     * @throws IOException if reading from stream failed
     */
    public TrieUnigram[] readUnigrams(int count) throws IOException {
        TrieUnigram[] unigrams = new TrieUnigram[count + 1];
        for (int i = 0; i < count + 1; i++) {
            unigrams[i] = new TrieUnigram();
            unigrams[i].prob = Utilities.readLittleEndianFloat(inStream);
            unigrams[i].backoff = Utilities.readLittleEndianFloat(inStream);
            unigrams[i].next = Utilities.readLittleEndianInt(inStream);
        }
        return unigrams;
    }

    /**
     * Reads trie in form of byte array into provided array. 
     * Size of byte array is computed from previously read language model specifications,
     * see {@link NgramTrie}
     * @param arr - byte array to read trie to
     * @throws IOException if reading from stream failed
     */
    public void readTrieByteArr(byte[] arr) throws IOException {
        inStream.read(arr);
    }

    /**
     * Reads vocabulary of language model. Ordinal number of word stays for wordId.
     * @param unigramNum - amount of unigrams
     * @return array of strings - vocabulary of language model
     * @throws IOException of reading from stream failed
     */
    public String[] readWords(int unigramNum) throws IOException {
        int len = Utilities.readLittleEndianInt(inStream);
        if (len <= 0) {
            throw new Error("Bad word string size: " + len);
        }
        String[] words = new String[unigramNum];
        byte[] bytes = new byte[len];
        inStream.read(bytes);

        int s = 0;
        int wordStart = 0;
        for (int i = 0; i < len; i++) {
            char c = (char) (bytes[i] & 0xFF);
            if (c == '\0') {
                // if its the end of a string, add it to the 'words' array
                words[s] = new String(bytes, wordStart, i - wordStart);
                wordStart = i + 1;
                s++;
            }
        }
        assert (s == unigramNum);
        return words;
    }

    /**
     * Should be called when model reading finished
     * @throws IOException if stream was corrupted
     */
    public void close() throws IOException {
        inStream.close();
    }

    /**
     * Reads language model max depth. 
     * Order is stored in uint8 or in byte
     * @return order of language model
     * @throws IOException if reading from stream failed
     */
    private int readOrder() throws IOException {
        return (int)inStream.readByte();
    }

    /**
     * Reads float array of specified length. 
     * Quantation tables are stored in form of float arrays,
     * see {@link readQuant}
     * @param len - length of array to read
     * @return array of floats that was read from stream
     * @throws IOException if reading from stream failed
     */
    private float[] readFloatArr(int len) throws IOException {
        float[] arr = new float[len];
        for (int i = 0; i < len; i++)
            arr[i] = Utilities.readLittleEndianFloat(inStream);
        return arr;
    }

    /**
     * Reads a string of the given length from the given DataInputStream. It is assumed that the DataInputStream
     * contains 8-bit chars.
     *
     * @param stream the DataInputStream to read from
     * @param length the number of characters in the returned string
     * @return a string of the given length from the given DataInputStream
     * @throws java.io.IOException
     */
    private String readString(DataInputStream stream, int length)
            throws IOException {
        StringBuilder builder = new StringBuilder();
        byte[] bytes = new byte[length];
        stream.read(bytes);
        for (int i = 0; i < length; i++) {
            builder.append((char) bytes[i]);
        }
        return builder.toString();
    }

}
