package edu.cmu.sphinx.frontend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.io.File;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import edu.cmu.sphinx.Sphinx4TestCase;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.AudioFileProcessListener;
import edu.cmu.sphinx.frontend.util.ConcatAudioFileDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;


/**
 * Some small unit tests to check whether the AudioFileDataSource and the
 * ConcatAudioFileDataSource are working properly.
 */
public class AudioDataSourcesTest extends Sphinx4TestCase {

    public static final String BASE_DIR = "/edu/cmu/sphinx/frontend/test/data";

    private int numFileStarts; // used to test the AudioFileProcessListener
                               // implementation
    private int numFileEnds;

    @BeforeMethod
    public void setUp() {
        numFileStarts = 0;
        numFileEnds = 0;
    }

    @Test
    public void testSimpleFileSources() throws DataProcessingException {
        // TODO: use paremeterized tests
        runAssert("test.wav");
        runAssert("test.aiff");
        runAssert("test.au");
        // TODO: check if OGG is supported and skip if it's not
        // runAssert("test.ogg"); // only works if the appropriate JavaSound
        // extension is installed
    }

    @Test
    public void test8KhzSource() throws DataProcessingException {
        AudioFileDataSource dataSource = ConfigurationManager
                .getInstance(AudioFileDataSource.class);

        // Test simple WAV.
        File file = getResourceFile("test8k.wav");

        dataSource.setAudioFile(file, null);
        assertThat(dataSource.getData(), instanceOf(DataStartSignal.class));
        Data d = dataSource.getData();
        assertThat(dataSource.getData(), instanceOf(DoubleData.class));
        assertThat(((DoubleData) d).getSampleRate(), equalTo(8000));

        while ((d = dataSource.getData()) instanceof DoubleData);
        assertThat(d, instanceOf(DataEndSignal.class));
    }

    @Test
    public void testConcatDataSource() throws DataProcessingException {
        ConcatAudioFileDataSource dataSource = ConfigurationManager
                .getInstance(ConcatAudioFileDataSource.class);

        dataSource.addNewFileListener(new AudioFileProcessListener() {

            public void audioFileProcStarted(File audioFile) {
                numFileStarts++;
            }

            public void audioFileProcFinished(File audioFile) {
                numFileEnds++;
            }

            public void newProperties(PropertySheet ps)
                    throws PropertyException {
            }
        });

        File file = getResourceFile("test.drv");
        dataSource.setBatchFile(file);
        assertThat(dataSource.getData(), instanceOf(DataStartSignal.class));
        assertThat(dataSource.getData(), instanceOf(DoubleData.class));

        Data d;
        while ((d = dataSource.getData()) instanceof DoubleData);
        assertThat(d, instanceOf(DataEndSignal.class));

        assertThat(numFileStarts, equalTo(3));
        assertThat(numFileEnds, equalTo(3));
    }

    private void runAssert(String fileName) throws DataProcessingException {
        AudioFileDataSource dataSource = ConfigurationManager
                .getInstance(AudioFileDataSource.class);

        File file = getResourceFile(fileName);
        dataSource.setAudioFile(file, null);
        assertThat(dataSource.getData(), instanceOf(DataStartSignal.class));
        assertThat(dataSource.getData(), instanceOf(DoubleData.class));

        Data d;
        while ((d = dataSource.getData()) instanceof DoubleData);
        assertThat(d, instanceOf(DataEndSignal.class));
    }
}
