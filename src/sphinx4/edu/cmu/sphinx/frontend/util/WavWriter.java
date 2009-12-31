package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.util.props.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;


/**
 * Stores audio data into numbered (MS-)wav files.
 * TODO: currently the WavWriter is only able to write data in bigEndian, 
 * support for littleEndian would be nice
 * TODO: currently the WavWriter buffers all audio data until a DataEndSignal occurs.
 *
 * @author Holger Brandl
 */
public class WavWriter extends BaseDataProcessor {

    /**
     * The pathname which must obey the pattern: pattern + i + .wav. Only the pattern is required here (e.g.
     * wavdump/file). After each DataEndSignal the smalles unused 'i' is determined.
     */
    @S4String
    public static final String PROP_OUT_FILE_NAME_PATTERN = "outFilePattern";

    @S4Boolean(defaultValue = false)

    public static final String PROP_IS_COMPLETE_PATH = "isCompletePath";
    private boolean isCompletePath;

    /** The default value for PROP_RAND_STREAM_START */
    private String dumpFilePath;

    /** The property for the number of bits per value. */
    @S4Integer(defaultValue = 16)
    public static final String PROP_BITS_PER_SAMPLE = "bitsPerSample";
    /** Default value for PROP_BITS_PER_SAMPLE. */
    private int bitsPerSample = 16;


    /** The property specifying whether the input data is signed. */
    @S4Boolean(defaultValue = true)
    public static final String PROP_SIGNED_DATA = "signedData";
    /** The default value of PROP_SIGNED_DATA. */
    private boolean isSigned = true;

    /** The property specifying whether the input data is signed. */
    @S4Boolean(defaultValue = false)
    public static final String PROP_CAPTURE_UTTERANCES = "captureUtterances";
    /** The default value of PROP_SIGNED_DATA. */
    protected boolean captureUtts;

    private ByteArrayOutputStream baos;
    private DataOutputStream dos;

    private int sampleRate;
    private boolean isInSpeech;

    public WavWriter(String dumpFilePath, boolean isCompletePath, int bitsPerSample, boolean isSigned, boolean captureUtts) {
	    initLogger();

        this.dumpFilePath = dumpFilePath;
        this.isCompletePath = isCompletePath;

        this.bitsPerSample = bitsPerSample;
        if (bitsPerSample % 8 != 0) {
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        }

        this.isSigned = isSigned;
        this.captureUtts = captureUtts;

        initialize();
    }

    public WavWriter() {
    }

    /*
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        dumpFilePath = ps.getString(WavWriter.PROP_OUT_FILE_NAME_PATTERN);
        isCompletePath = ps.getBoolean(PROP_IS_COMPLETE_PATH);

        bitsPerSample = ps.getInt(PROP_BITS_PER_SAMPLE);
        if (bitsPerSample % 8 != 0) {
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        }

        isSigned = ps.getBoolean(PROP_SIGNED_DATA);
        captureUtts = ps.getBoolean(PROP_CAPTURE_UTTERANCES);

        initialize();
    }
    
    @Override
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();

        if (data instanceof DataStartSignal)
            sampleRate = ((DataStartSignal) data).getSampleRate();

        if (data instanceof DataStartSignal || (data instanceof SpeechStartSignal && captureUtts)) {
            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
        }


        if ((data instanceof DataEndSignal && !captureUtts) || (data instanceof SpeechEndSignal && captureUtts)) {
        	
            String wavName;
            if (isCompletePath)
                wavName = dumpFilePath;
            else
                wavName = dumpFilePath + getNextFreeIndex(dumpFilePath) + ".wav";

            writeFile(wavName);

            isInSpeech = false;
        }

        if (data instanceof SpeechStartSignal)
            isInSpeech = true;

        if ((data instanceof DoubleData || data instanceof FloatData) && (isInSpeech || !captureUtts)) {
            DoubleData dd = data instanceof DoubleData ? (DoubleData) data : DataUtil.FloatData2DoubleData((FloatData) data);
            double[] values = dd.getValues();

            for (double value : values) {
                try {
                    dos.writeShort(new Short((short) value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return data;
    }


    private static int getNextFreeIndex(String outPattern) {
        int fileIndex = 0;
        while (new File(outPattern + fileIndex + ".wav").isFile())
            fileIndex++;

        return fileIndex;
    }


    /** Initializes this DataProcessor. This is typically called after the DataProcessor has been configured. */
    @Override
    public void initialize() {
        super.initialize();

        assert dumpFilePath != null;
        baos = new ByteArrayOutputStream();
    }


    private static AudioFileFormat.Type getTargetType(String extension) {
        AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();

        for (AudioFileFormat.Type aTypesSupported : typesSupported) {
            if (aTypesSupported.getExtension().equals(extension)) {
                return aTypesSupported;
            }
        }

        return null;
    }


    /**
     * Converts a big-endian byte array into an array of doubles. Each consecutive bytes in the byte array are converted
     * into a double, and becomes the next element in the double array. The size of the returned array is
     * (length/bytesPerValue). Currently, only 1 byte (8-bit) or 2 bytes (16-bit) samples are supported.
     *
     * @param values
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws ArrayIndexOutOfBoundsException
     */
    public static byte[] valuesToBytes(double[] values, int bytesPerValue, boolean signedData)
            throws ArrayIndexOutOfBoundsException {

        byte[] byteArray = new byte[bytesPerValue * values.length];

        int byteArInd = 0;

        for (double value : values) {
            int val = (int) value;


            for (int j = bytesPerValue - 1; j >= 0; j++) {
                byteArray[byteArInd + j] = (byte) (val & 0xff);
                val = val >> 8;
            }

            byteArInd += bytesPerValue;
        }

        return byteArray;
    }


    public static AudioInputStream convertDoublesToAudioStream(double[] values, int sampleRate) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (double value : values) {
            try {
                dos.writeShort(new Short((short) value));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AudioFormat wavFormat = new AudioFormat(sampleRate, 16, 1, true, true);
        byte[] abAudioData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(abAudioData);

        return new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());
    }


    /** Writes a given double array into a wav file (given the sample rate of the signal).
     * @param signal
     * @param sampleRate
     * @param targetFile*/
    public static void writeWavFile(double[] signal, int sampleRate, File targetFile) {
        AudioInputStream ais = WavWriter.convertDoublesToAudioStream(signal, sampleRate);
        AudioFileFormat.Type outputType = getTargetType("wav");

        try {
            AudioSystem.write(ais, outputType, targetFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Writes the current stream to disc; override this method if you want to take 
    * additional action on file writes
    *
    * @param wavName name of the file to be written
    */
    protected void writeFile(String wavName) {
        AudioFormat wavFormat = new AudioFormat(sampleRate, bitsPerSample, 1, isSigned, true);
        AudioFileFormat.Type outputType = getTargetType("wav");

        byte[] abAudioData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(abAudioData);
        AudioInputStream ais = new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());

        File outWavFile = new File(wavName);

        if (AudioSystem.isFileTypeSupported(outputType, ais)) {
            try {
                AudioSystem.write(ais, outputType, outWavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   	
    }

}
