
makeLM - makes a language model for the weather forecasting domain.

Usage:

    % makeLM

Outputs:
    weather.lm - arpa format of the language model
    weather.DMP - CMU binary format of the language model
    weather.transcript - transcript

Requires:
    CMU language model toolkit:
        http://www.speech.cs.cmu.edu/SLM_info.html
    lm3g2dmp - utility to generate DMP format models:
       http://cmusphinx.sourceforge.net/webpage/html/download.php#utilities#
    unix commands: gawk uniq mv rmdir rm

    All commmads should be in your path




--------------------------------------------------------
Detailed notes on how to create your own language model
--------------------------------------------------------
This directory provides an example of how to create a language model
for Sphinx-4 from a reference text. The process for creating a
language model is as follows:

1) Prepare a reference text that will be used to generate the language
model.  This should consist of a set of sentences that are bounded by
the start and end sentence markers: <s> and </s>.  here's an example:


<s> generally cloudy today with scattered outbreaks of rain 
    and drizzle persistent and heavy at times </s>

<s> some dry intervals also with hazy sunshine especially 
    in eastern parts in the morning </s>

<s> highest temperatures nine to thirteen celsius in a light 
    or moderate mainly east south east breeze </s>

<s> cloudy damp and misty today with spells of rain and drizzle in 
    most places   much of this rain will be light and patchy but 
    heavier rain may develop in the west later </s>


More data will generate better language models.  The 'weather.txt'
file (used to generate the weather language model) contains nearly
100,000 sentences.


2) Generate the vocabulary file. This is a list of all the words in
the file:

    text2wfreq < weather.txt | wfreq2vocab > weather.tmp.vocab


3) You may want to edit the vocabulary file to remove words (numbers,
mispelling, names).  If you find mispellings, it is a good idea to fix
them in the input transcript.


4) If you want a closed vocabulary language model (a language model
that has no provisions for unknown words), then you should remove
sentences from your input transcript that contain words that are not
in your vocabulary file. The 'extractVocab.awk' script will do this:

gawk -f extractVocab.awk weather.vocab  weather.txt  > weather.closed.txt


5) Generate the arpa format language model with the commands:

% text2idngram -vocab weather.vocab < weather.closed.txt > weather.idngram
% idngram2lm -vocab_type 0 -idngram weather.idngram -vocab \
     weather.vocab -arpa weather.arpa


6) Generate the CMU binary form (DMP)

    % mkdir dmp
    % lm3g2dmp weather.arpa dmp
    % mv dmp/weather.arpa.DMP weather.DMP

--------------------------------------------------------
Files
--------------------------------------------------------
README.txt              - this file
extractVocab.awk        - extracts in-vocabulary sentences 
genTranscript.awk       - generates a transcript file
makeLM                  - makes the language model
weather.DMP             - binary form of the language model
weather.lm              - arpa form of the language model
weather.transcript      - the transcript
weather.txt             - the input corpus
weather.vocab           - the vocabulary
