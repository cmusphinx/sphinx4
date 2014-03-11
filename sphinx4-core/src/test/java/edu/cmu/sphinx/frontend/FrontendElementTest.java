package edu.cmu.sphinx.frontend;

import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.io.*;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.cmu.sphinx.Sphinx4TestCase;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;


public class FrontendElementTest extends Sphinx4TestCase {

    public void runTest(String frontendName, String name) throws IOException {
        URL url = getResourceUrl("frontend.xml");
        ConfigurationManager cm = new ConfigurationManager(url);

        AudioFileDataSource dataSource = cm.lookup("audioFileDataSource");
        dataSource.setAudioFile(getResourceFile("test-feat.wav"), null);

        FrontEnd frontend = cm.lookup(frontendName);
        compareDump(frontend, name);
    }

    private void compareDump(FrontEnd frontend, String name)
            throws NumberFormatException, DataProcessingException, IOException {
        FileInputStream stream = new FileInputStream(getResourceFile(name));
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = br.readLine()) != null) {
            Data data = frontend.getData();

            if (line.startsWith("DataStartSignal"))
                assertThat(data, instanceOf(DataStartSignal.class));
            if (line.startsWith("DataEndSignal"))
                assertThat(data, instanceOf(DataEndSignal.class));
            if (line.startsWith("SpeechStartSignal"))
                assertThat(data, instanceOf(SpeechStartSignal.class));
            if (line.startsWith("SpeechEndSignal"))
                assertThat(data, instanceOf(SpeechEndSignal.class));

            if (line.startsWith("Frame")) {
                assertThat(data, instanceOf(DoubleData.class));

                double[] values = ((DoubleData) data).getValues();
                String[] tokens = line.split(" ");

                assertThat(values.length, equalTo(parseInt(tokens[1])));

                for (int i = 0; i < values.length; i++)
                    assertThat(values[i],
                               closeTo(parseDouble(tokens[2 + i]),
                                       abs(0.001 * values[i])));
            }

            if (line.startsWith("FloatFrame")) {
                String[] tokens = line.split(" ");
                Assert.assertTrue(data instanceof FloatData);
                float[] values = ((FloatData) data).getValues();
                Assert.assertEquals(values.length,
                                    (int) Integer.valueOf(tokens[1]));
                for (int i = 0; i < values.length; i++)
                    assertThat(Double.valueOf(values[i]),
                               closeTo(parseFloat(tokens[2 + i]),
                                       abs(0.001 * values[i])));
            }
        }
    }

    @Test
    public void testPreemp() throws IOException {
        runTest("preempTest", "after-preemp.dump");
    }

    @Test
    public void testWindow() throws IOException {
        runTest("windowTest", "after-window.dump");
    }

    @Test
    public void testFFT() throws IOException {
        runTest("fftTest", "after-fft.dump");
    }

    @Test
    public void testMel() throws IOException {
        runTest("melTest", "after-mel.dump");
    }

    @Test
    public void testDct() throws IOException {
        runTest("dctTest", "after-dct.dump");
    }

    @Test
    public void testCMN() throws IOException {
        runTest("cmnTest", "after-cmn.dump");
    }

    @Test
    public void testFeature() throws IOException {
        runTest("feTest", "after-feature.dump");
    }
}
