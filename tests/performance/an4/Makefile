# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
# Makefile for all the AN4 tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/an4

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=an4.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)



PROFILE_FLAGS= -Xprof 
LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms128m -mx512m


DEBUG_FLAGS= 
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}


# 
# check the MODE setting. If invoked with MODE=quick, run a quick
# version of the test (5 times quicker)
# 
# example:      make MODE=quick an4_words_wordlist
#
#

ifeq ($(MODE),quick)
  SKIP = 5
else
  SKIP = 0
endif

ifeq ($(MODE),super_quick)
  SKIP = 100
endif

all:: 


##########################################################################
#
# The following 14 tests are the Sphinx 4 portion of
# the "apples to rotten apples" tests between Sphinx 3 and Sphinx 4.
#
##########################################################################


# Tests the 79 whole words from AN4, using a looping WordListGrammar
# whose vocabulary is just the spoken words from AN4.

an4_words_wordlist:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.SimpleWordListGrammar.path=./an4_words.wordlist\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=1.0E-25\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-250 \
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch


# Tests the 79 whole words from AN4, using a flat unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_words_flat_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.flat_unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch

# Tests the 79 whole words from AN4, using a unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_words_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch

# Tests the 79 whole words from AN4, using a unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_words_bigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.bigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch

# Tests the 79 whole words from AN4, using a trigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_words_trigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.NullGrammar\
    \
    -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=false \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.trigram.lm\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=1.0E-20\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch

an4_words_trigram_flat:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.NullGrammar\
    \
    -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.trigram.lm\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=-1 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-40 \
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2 \
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch

an4_full_trigram_flat:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.NullGrammar\
    \
    -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_full.trigram.lm\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=-1 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-40 \
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2 \
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_full.batch


# Tests the 79 whole words from AN4, using an FST created from
# the flat unigram LM based on the spoken words of AN4.

an4_words_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_words.flat_unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch

# Tests the 79 whole words from AN4, using an FST created from
# the unigram LM based on the spoken words of AN4.

an4_words_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_words.unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch

# Tests the 79 whole words from AN4, using an FST created from
# the bigram LM based on the spoken words of AN4.

an4_words_bigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_words.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch

# Tests the 79 whole words from AN4, using an FST created from
# the trigram LM based on the spoken words of AN4.

an4_words_trigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_words.trigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch


################# Spelling tests #################

# Tests the spelling  words from AN4, using a looping WordListGrammar
# whose vocabulary is just the spoken words from AN4.

an4_spelling_wordlist:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.SimpleWordListGrammar.path=./an4_spelling.wordlist\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-250 \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch


# Tests the spelling  words from AN4, using a flat unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_spelling_flat_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_spelling.flat_unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_spelling.batch

# Tests the spelling  words from AN4, using a unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_spelling_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_spelling.unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_spelling.batch

# Tests the spelling  words from AN4, using a unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_spelling_bigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=0\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_spelling.bigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_spelling.batch


# Tests the spelling  words from AN4, using an FST created from
# the flat unigram LM based on the spoken words of AN4.

an4_spelling_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_spelling.flat_unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch

# Tests the spelling  words from AN4, using an FST created from
# the unigram LM based on the spoken words of AN4.

an4_spelling_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_spelling.unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch

# Tests the spelling  words from AN4, using an FST created from
# the bigram LM based on the spoken words of AN4.

an4_spelling_bigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=0\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_spelling.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch

# Tests the spelling  words from AN4, using an FST created from
# the trigram LM based on the spoken words of AN4.

an4_spelling_trigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=0\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_spelling.trigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch

################# full tests #################

# Tests the 105 words and letters from AN4, using a looping WordListGrammar
# whose vocabulary is just the spoken words from AN4.

an4_full_wordlist:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.SimpleWordListGrammar.path=./an4_full.wordlist\
    -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-250 \
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch


# Tests the 105 words and letters from AN4, using a flat unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_full_flat_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_full.flat_unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_full.batch

# Tests the 105 words and letters from AN4, using a unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_full_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_full.unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_full.batch

# Tests the 105 words and letters from AN4, using a unigram arpa-format
# grammar whose vocabulary is just the spoken words from AN4.
an4_full_bigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_full.bigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_full.batch


# Tests the 105 words and letters from AN4, using an FST created from
# the flat unigram LM based on the spoken words of AN4.

an4_full_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_full.flat_unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch

# Tests the 105 words and letters from AN4, using an FST created from
# the unigram LM based on the spoken words of AN4.

an4_full_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_full.unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch

# Tests the 105 words and letters from AN4, using an FST created from
# the bigram LM based on the spoken words of AN4.

an4_full_bigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_full.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch

# Tests the 105 words and letters from AN4, using an FST created from
# the trigram LM based on the spoken words of AN4.

an4_full_trigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./an4_full.trigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch



##############################################################################
# old stuff

#################################################
# Bushderby test target with high eta
#################################################
bush_test:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.linguist.WordListGrammar.path=./an4_words.wordlist\
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-250 \
	-Dedu.cmu.sphinx.decoder.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.enableBushderby=true \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.strictPruning=true \
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.bushderbyEta=10000 \
	-Dedu.cmu.sphinx.decoder.linguist.WordListGrammar.addSilenceWord=false \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch


#################################################
# Bushderby test target
#################################################
bush_test2:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.linguist.WordListGrammar.path=./an4_words.wordlist\
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-250 \
	-Dedu.cmu.sphinx.decoder.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.enableBushderby=true \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.strictPruning=true \
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.bushderbyEta=.5 \
	-Dedu.cmu.sphinx.decoder.linguist.WordListGrammar.addSilenceWord=false \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch

#################################################
# Binary loader test target
#################################################
binary:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.acoustic.location=file:/lab/speech/sphinx4/data/wsj_8gau_13dCep_16k_40mel_130Hz_6800Hz.bin.zip \
    -Dedu.cmu.sphinx.knowledge.acoustic.format=sphinx3.binary \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch

ascii:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.acoustic.location=file:/lab/speech/sphinx4/data/wsj_8gau_13dCep_16k_40mel_130Hz_6800Hz.ascii.zip \
    -Dedu.cmu.sphinx.knowledge.acoustic.format=sphinx3.ascii \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder an4.props an4_words.batch


#########################################################################
#
# Live mode AN4 tests.
#
#########################################################################

an4_words_bigram_live:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.frontend.endpoint.SpeechMarker.speechTrailer=50 \
	-Dedu.cmu.sphinx.frontend.util.ConcatFileAudioSource.skip=$(SKIP) \
	-Dedu.cmu.sphinx.frontend.util.ConcatFileAudioSource.silenceFile=/lab/speech/sphinx4/data/tidigits/test/raw16k/silence1sec.raw \
	-Dedu.cmu.sphinx.frontend.keepAudioReference=false \
	-Dedu.cmu.sphinx.frontend.bytesPerAudioFrame=320 \
	-Dedu.cmu.sphinx.frontend.cmn=edu.cmu.sphinx.frontend.LiveCMN \
	-Dedu.cmu.sphinx.frontend.featureExtractor=edu.cmu.sphinx.frontend.LiveFeatureExtractor \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.LevelTracker \
	-Dedu.cmu.sphinx.frontend.endpoint.LevelTracker.threshold=16 \
	-Dedu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer.scoreablesKeepFeature=true \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./an4_words.bigram.lm\
	edu.cmu.sphinx.decoder.LivePretendDecoder an4.props an4_words.batch
