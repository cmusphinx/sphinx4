/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;


/**
 * A BatchFileAudioSource takes a file (called batch file onwards)
 * that contains a list of audio files,
 * and converts the audio data in each of the audio files into
 * DoubleAudioFrame(s). One would obtain the DoubleAudioFrames using
 * the <code>read()</code> method. This class uses the StreamAudioSource
 * class. In fact, it converts each audio file in the batch file into
 * an InputStream, and sets it to the InputStream of StreamAudioSource.
 * Its read() method then calls StreamAudioSource.read().
 * The only difference is that BatchFileAudiosource
 * takes a batch file, whereas StreamAudioSource takes an InputStream.
 *
 * The format of the batch file would look like: <pre>
 * /home/user1/data/music.au
 * /home/user1/data/talking.au
 * ...
 * </pre>
 */
public class BatchFileAudioSource implements DataSource {

    private String batchFile;
    private BufferedReader reader;
    private StreamAudioSource streamAudioSource = null;


    /**
     * Constructs a BatchFileAudioSource with the given InputStream.
     *
     * @param audioStream the InputStream where audio data comes from
     */
    public BatchFileAudioSource(String batchFile) throws IOException {
        this.batchFile = batchFile;
        reader = new BufferedReader(new FileReader(batchFile));
        streamAudioSource = new StreamAudioSource(null);

        String firstFile = reader.readLine();
        if (firstFile != null) {
            fileSetStream(firstFile);
        }
    }


    /**
     * Construct an InputStream with the given audio file, and set it as
     * the InputStream of the streamAudioSource.
     *
     * @param audioFile the file containing audio data
     */
    private void fileSetStream(String audioFile) throws IOException {
        streamAudioSource.setInputStream
            (new FileInputStream(audioFile + ".raw"));
        streamAudioSource.reset();
    }

    
    /**
     * Reads and returns the next DoubleAudioFrame. 
     * Returns null if all the data in all the files have been read.
     *
     * @return the next DoubleAudioFrame or <code>null</code> if no more is
     *     available
     *
     * @throws java.io.IOException
     */
    public Data read() throws IOException {
        if (streamAudioSource == null) {
            return null;
        }
        
        Data frame = streamAudioSource.read();
        if (frame != null) {
            return frame;
        }

        // if we reached the end of the current file, go to the next one
        String nextFile = reader.readLine();
        if (nextFile != null) {
            fileSetStream(nextFile);
            return streamAudioSource.read();
        }

        return null;
    }
}



