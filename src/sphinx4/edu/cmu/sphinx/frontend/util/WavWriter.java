package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.Map;
import java.util.HashMap;


/**
 * Stores audio data into numbered (MS-)wav files.
 *
 * @author Holger Brandl
 */
//todo currently the WavWriter is only able to write data in bigEndian; support for littleEndian would be nice
//todo currently the WavWriter buffers all audio data until a DataEndSignal occurs.
public class WavWriter extends BaseDataProcessor {

    /**
     * The pathname which must obey the pattern: pattern + i + .wav. Only the pattern is required here (e.g.
     * wavdump/file). After each DataEndSignal the smalles unused 'i' is determined.
     */
    public static final String PROP_OUT_FILE_NAME_PATTERN = "outFilePattern";


    /** The default value for PROP_RAND_STREAM_START */
    private String outPattern = null;

    /** SphinxProperty for the number of bits per value. */
    public static final String PROP_BITS_PER_SAMPLE = "bitsPerSample";
    /** Default value for PROP_BITS_PER_SAMPLE. */
    private int bitsPerSample = 16;

    /** The SphinxProperty specifying whether the input data is big-endian. */
    public static final String PROP_BIG_ENDIAN_DATA = "bigEndianData";
    /** The default value for PROP_IS_DATA_BIG_ENDIAN. */
    private boolean isBigEndian = true;

    /** The SphinxProperty specifying whether the input data is signed. */
    public static final String PROP_SIGNED_DATA = "signedData";
    /** The default value of PROP_SIGNED_DATA. */
    private boolean isSigned = true;

    /** The SphinxProperty specifying whether the input data is signed. */
    public static final String PROP_CAPTURE_UTTERANCES = "captureUtterances";
    /** The default value of PROP_SIGNED_DATA. */
    private boolean captureUtts = false;


    private ByteArrayOutputStream baos;
    private DataOutputStream dos;

    private int sampleRate;
    private boolean isInSpeech;


    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();

        if (data instanceof DataStartSignal)
            sampleRate = ((DataStartSignal) data).getSampleRate();

        if (data instanceof DataStartSignal || (data instanceof SpeechStartSignal && captureUtts)) {
            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
        }


        if ((data instanceof DataEndSignal && !captureUtts) || (data instanceof SpeechEndSignal && captureUtts)) {
            AudioFormat wavFormat = new AudioFormat(sampleRate, bitsPerSample, 1, isSigned, true);
            AudioFileFormat.Type outputType = getTargetType("wav");
            String wavName = outPattern + getNextFreeIndex(outPattern) + ".wav";

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

            isInSpeech = false;
        }

        if (data instanceof SpeechStartSignal)
            isInSpeech = true;

        if ((data instanceof DoubleData || data instanceof FloatData) && (isInSpeech || !captureUtts)) {
            DoubleData dd = data instanceof DoubleData ? (DoubleData) data : DataUtil.FloatData2DoubleData((FloatData) data);
            double[] values = dd.getValues();

//            if (isBigEndian) {
//                doubleData = DataUtil.bytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
//            } else {
//                doubleData = DataUtil.littleEndianBytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
//            }

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

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getConfigurationInfo()
     */
    public static Map getConfigurationInfo(){
        Map info = new HashMap();
    
        info.put(new String("PROP_BIG_ENDIAN_DATA_TYPE"),new String("BOOLEAN"));
        info.put(new String("PROP_SIGNED_DATA_TYPE"),new String("BOOLEAN"));
        info.put(new String("PROP_CAPTURE_UTTERANCES_TYPE"),new String("BOOLEAN"));
        info.put(new String("PROP_BITS_PER_SAMPLE_TYPE"),new String("INTEGER"));        
        info.put(new String("PROP_OUT_FILE_NAME_PATTERN_TYPE"),new String("STRING"));
        return info;
    }
    
    /*
    * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
    */
    public void register(String name, Registry registry) throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_OUT_FILE_NAME_PATTERN, PropertyType.STRING);
        registry.register(PROP_BITS_PER_SAMPLE, PropertyType.INT);
        registry.register(PROP_BIG_ENDIAN_DATA, PropertyType.BOOLEAN);
        registry.register(PROP_SIGNED_DATA, PropertyType.BOOLEAN);
        registry.register(PROP_CAPTURE_UTTERANCES, PropertyType.BOOLEAN);

    }


    /** Initializes this DataProcessor. This is typically called after the DataProcessor has been configured. */
    public void initialize() {
        super.initialize();

        assert outPattern != null;
        baos = new ByteArrayOutputStream();
    }


    /*
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        outPattern = ps.getString(WavWriter.PROP_OUT_FILE_NAME_PATTERN, null);

        bitsPerSample = ps.getInt(PROP_BITS_PER_SAMPLE, bitsPerSample);
        if (bitsPerSample % 8 != 0) {
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        }

        isBigEndian = ps.getBoolean(PROP_BIG_ENDIAN_DATA, isBigEndian);
        isSigned = ps.getBoolean(PROP_SIGNED_DATA, isSigned);
        captureUtts = ps.getBoolean(PROP_CAPTURE_UTTERANCES, captureUtts);

        initialize();
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

//        try {
//            AudioSystem.write(ais, getTargetType("wav"), new File("segment.wav"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());
    }

}
