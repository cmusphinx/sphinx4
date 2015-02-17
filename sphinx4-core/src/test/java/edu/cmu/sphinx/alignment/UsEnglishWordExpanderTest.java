package edu.cmu.sphinx.alignment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import edu.cmu.sphinx.util.Utilities;

public class UsEnglishWordExpanderTest {

    private static final Object[][] TEST_DATA =
            {
                    {"# . no, $ convertion.", ". no $ convertion"},
                    {"1, 2 3", "one two three"},
                    {"the answer is 42,", "the answer is forty two"},
                    {"587", "five hundred eighty seven"},
                    {"1903", "one thousand nine hundred three"},
                    {"12011", "twelve thousand eleven"},
                    {"126166",
                            "one hundred twenty six thousand one hundred sixty six"},
                    {"9 3/4", "nine and three fourth 's"},
                    {"October 1st", "october first"},
                    {"May the 4th be with you", "may the fourth be with you"},
                    {"7-11", "seven to eleven"},
                    {"12, 35", "twelve thirty five"},
                    {"146%", "one hundred forty six percent"},
                    {"320'000", "three hundred twenty thousand"},
                    {"120,000", "one hundred twenty thousand"},
                    {"$35,000", "thirty five thousand dollars"},
                    {"$1000000", "one million dollars"},
                    {"U.S. economy", "u s economy"},
                    {"sweet home Greenbow, AL.", "sweet home greenbow alabama"},
                    {"Henry I", "henry the first"},
                    {"Chapter XVII", "chapter seventeen"},
                    {"don't, doesn't, won't, can't", "don't doesn't won't can't"},
                    {"I've we've", "i've we've"},
                    {"I've we've it's", "i've we've it's"},
                    {"Classics of 80s", "classics of eighties"},
                    {"In 1880s", "in eighteen eighties"},
                    {"Mulholland Dr.", "mulholland drive"},
                    {"dr. Jekyll and Mr. Hyde.",
                            "doctor jekyll and mister hyde"},
                    {"Mr. & Mrs. smith", "mister and missus smith"},
                    {"St. Louis Cardinals", "saint louis cardinals"},
                    {"St. Elmo's fire", "saint elmo's fire"},
                    {"elm st.", "elm street"},};

    private TextTokenizer expander;

    @BeforeMethod
    public void setupMethod() {
        expander = new USEnglishTokenizer();
    }

    @DataProvider(name = "data")
    public Object[][] getData() {
        return TEST_DATA;
    }

    @Test(dataProvider = "data")
    public void textToWords(String text, String expanded) {
        List<String> tokens = expander.expand(text);
        assertThat(Utilities.join(tokens), equalTo(expanded));
    }
}
