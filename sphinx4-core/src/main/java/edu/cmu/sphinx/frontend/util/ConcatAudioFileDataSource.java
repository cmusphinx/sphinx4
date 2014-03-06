package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.util.ReferenceSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;


/**
 * Concatenates a list of audio files as one continuous audio stream.
 * <p/>
 * A {@link edu.cmu.sphinx.frontend.DataStartSignal DataStartSignal} will be placed before the start of the first file,
 * and a {@link edu.cmu.sphinx.frontend.DataEndSignal DataEndSignal} after the last file. No DataStartSignal or
 * DataEndSignal will be placed between them.
 *
 * @author Holger Brandl
 */
public class ConcatAudioFileDataSource extends AudioFileDataSource implements ReferenceSource {

    private URL nextFile;
    private List<String> referenceList;
    private boolean isInitialized;

    List<URL> batchFiles;

    public ConcatAudioFileDataSource(int bytesPerRead, List<AudioFileProcessListener> listeners) {
        super(bytesPerRead,listeners);
    }

    public ConcatAudioFileDataSource() {

    }

    /** Initializes a ConcatFileDataSource. */
    @Override
    public void initialize() {
        super.initialize();

        if (batchFiles == null)
            return;

        try {
            referenceList = new ArrayList<String>();
            dataStream = new SequenceInputStream(new InputStreamEnumeration(batchFiles));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setBatchFile(File batchFile) {
        assert batchFile.isFile() : "given batch file is not a file";

        setBatchUrls(readDriver(batchFile.getAbsolutePath()));
    }


    public void setBatchFiles(List<File> files) {
        assert files != null;
        List<URL> urls = new ArrayList<URL>();

        try {
            for (File file : files)
                urls.add(file.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        setBatchUrls(urls);
    }


    public void setBatchUrls(List<URL> urls) {
        batchFiles = new ArrayList<URL>(urls);
        initialize();
    }


    /** Reads and verifies a driver file.
     *  @param fileName
     */
    private static List<URL> readDriver(String fileName) {
        File inputFile = new File(fileName);
        List<URL> driverFiles = null;

        try {
            if (!inputFile.isFile() || !inputFile.canRead())
                throw new IllegalArgumentException("file to read is not valid");

            BufferedReader bf = new BufferedReader(new FileReader(inputFile));

            driverFiles = new ArrayList<URL>();

            String line;
            while ((line = bf.readLine()) != null && line.trim().length() != 0) {
                File file = new File(line);

                assert file.isFile() : "file " + file + " does not exist!";
                driverFiles.add(file.toURI().toURL());
            }

            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert driverFiles != null;
        return driverFiles;
    }


    @Override
    public void setAudioFile(URL audioFileURL, String streamName) {
        throw new RuntimeException("Not supported for ConcatAudioFileDataSource");
    }


    /**
     * Returns a list of all reference text. Implements the getReferences() method of ReferenceSource.
     *
     * @return a list of all reference text
     */
    @Override
    public List<String> getReferences() {
        return referenceList;
    }


    /**
     * The work of the concatenating of the audio files are done here. The idea here is to turn the list of audio files
     * into an Enumeration, and then fed it to a SequenceInputStream, giving the illusion that the audio files are
     * concatenated, but only logically.
     */
    class InputStreamEnumeration implements Enumeration<AudioInputStream> {

        private URL lastFile;
        final Iterator<URL> fileIt;


        InputStreamEnumeration(List<URL> files) throws IOException {
            fileIt = new ArrayList<URL>(files).iterator();
        }


        /**
         * Tests if this enumeration contains more elements.
         *
         * @return true if and only if this enumeration object contains at least one more element to provide; false
         *         otherwise.
         */
        @Override
        public boolean hasMoreElements() {
            if (nextFile == null) {
                nextFile = readNext();
            }
            return (nextFile != null);
        }


        /**
         * Returns the next element of this enumeration if this enumeration object has at least one more element to
         * provide.
         *
         * @return the next element of this enumeration.
         */
        @Override
        public AudioInputStream nextElement() {
            AudioInputStream stream = null;
            if (lastFile == null) {
                nextFile = readNext();
            }

            if (nextFile != null) {
                try {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(nextFile);

                    // test whether all files in the stream have the same format
                    AudioFormat format = ais.getFormat();
                    if (!isInitialized) {
                        isInitialized = true;

                        bigEndian = format.isBigEndian();
                        sampleRate = (int) format.getSampleRate();
                        signedData = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
                        bytesPerValue = format.getSampleSizeInBits() / 8;
                    }

                    if (format.getSampleRate() != sampleRate || format.getChannels() != 1 || format.isBigEndian() != bigEndian) {
                        throw new RuntimeException("format mismatch for subsequent files");
                    }

                    stream = ais;
                    // System.out.println(nextFile);

                    logger.finer("Strating processing of '" + lastFile.getFile() + '\'');
                    for (AudioFileProcessListener fl : fileListeners)
                        fl.audioFileProcStarted(new File(nextFile.getFile()));

                    lastFile = nextFile;
                    nextFile = null;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw new Error("Cannot convert " + nextFile + " to a FileInputStream");
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }
            }

            return stream;
        }


        /**
         * Returns the name of next audio file
         *
         * @return the name of the appropriate audio file
         */
        public URL readNext() {
            if (lastFile != null) {
                logger.finest("Finished processing of '" + lastFile.getFile() + '\'');
                for (AudioFileProcessListener fl : fileListeners)
                    fl.audioFileProcFinished(new File(lastFile.getFile()));

                lastFile = null;
            }

            if (fileIt.hasNext())
                lastFile = fileIt.next();

            return lastFile;
        }
    }
}
