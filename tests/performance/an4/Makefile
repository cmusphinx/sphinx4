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

# LOGGER_PROPERTIES=tiDigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)



PROFILE_FLAGS= -Xprof 
LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms128m -mx1g 


DEBUG_FLAGS= 
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: 


##########################################################################
#
# The following 14 tests are the Sphinx 4 portion of
# the "apples to rotten apples" tests between Sphinx 3 and Sphinx 4.
#
##########################################################################


# Tests the 79 whole words from AN4, using a looping WordListGrammar
# whose vocabulary is just the spoken words from AN4.

an4_words_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist\
    -Dedu.cmu.sphinx.search.ActiveList.relativeBeamWidth=1E-250 \
    -Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
    -Dedu.cmu.sphinx.search.WordListGrammar.addSilenceWord=false \
    -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch



# Tests the 79 whole words from AN4, using a bigram LM based on
# the spoken words of AN4.

an4_words_bigram:
	echo "Using trigram lm to generate bigram"
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=0.8 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=16.0 \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
        -Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.LMGrammar\
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_words.trigram.lm\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch



# Tests the 79 whole words from AN4, using an FST created from
# the bigram LM based on the spoken words of AN4.

an4_words_bigram_fst:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-25 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./an4_words.bigram.fst.prunedloops\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch



# Tests the 79 whole words from AN4, using a trigram LM based on 
# the spoken words of AN4.
#-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-4\


an4_words_trigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=0.8 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=16.0 \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_words.trigram.lm\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch


an4_words_trigram_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=50\
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=0.8 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=16.0 \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_words.trigram.lm\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch

# Tests the 79 whole words from AN4, using an FST created from
# the trigram LM based on the spoken words of AN4.

an4_words_fst:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-10 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=16.0 \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
        -Dedu.cmu.sphinx.search.FSTGrammar.path=./an4_words.trigram.fst\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch



# Tests the spelled-out words from AN4,
# using a looping WordListGrammar of the 26 letters of the alphabet.

an4_spelling_unigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_spelling.alphalist\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch



# Tests the spelled-out words from AN4,
# using an trigram LM based on spelling of words from AN4.

an4_spelling_trigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-26\
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.filterSuccessors=true\
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_spelling.trigram.lm \
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=1000 \
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_spelling.alphalist \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=11.0 \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch



# Tests the spelled-out words from AN4,
# using an FST created from the trigram LM based on spelling of words from AN4.

an4_spelling_fst:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-10 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=16.0 \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./an4_spelling.trigram.fst \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch




# Tests the spelled-out words from AN4,
# using an trigram LM based on spelling of words from
# "Journey to the Center of the Earth". 

an4_spelling_journey_trigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-26\
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_spelling_journey.trigram.lm \
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_spelling.alphalist\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_spelling.batch



# Tests the spelled-out words from AN4,
# using an FST created from the trigram LM based on spelling of words
# from "Journey to the Center of the Earth". 

an4_spelling_journey_fst:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-30 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=16.0 \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./an4_spelling_journey.trigram.fst \
        edu.cmu.sphinx.decoder.BatchDecoder an4.props ./an4_spelling.batch



# Tests the 105 words and letters from AN4, using a looping WordListGrammar
# whose vocabulary is all words from AN4 and the 26 letters in the alphabet.

an4_full_unigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_full.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch



# Tests the 105 words and letters from AN4, using a trigram LM
# created from the words and spelled-out words of AN4.

an4_full_trigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-5 \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=15.0 \
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=1000 \
	-Dedu.cmu.sphinx.model.language.location=an4_full.trigram.lm \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_full.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch



# Tests the 105 words and letters from AN4, using an FST created from
# a trigram LM using the words and spelled-out words of AN4.


an4_full_fst:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-25 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=20.0 \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./an4_full.trigram.fst \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch


# Tests the 105 words and letters from AN4, using a looping
# WordListGrammar created from full AN4 plus the 100 most common words
# in English.

an4_plus_unigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_plus.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch



# Tests the 105 words and letters from AN4, using a trigram LM
# created from full AN4 plus the 100 most common words
# in English.

an4_plus_trigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-5 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=15.0 \
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=1000 \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=an4_plus.trigram.lm \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_plus.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch

# Tests the 105 words and letters from AN4, using a trigram LM
# created from full AN4 plus the 100 most common words
# in English.

an4_plus_trigram_quick:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=50\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-26\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=500 \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=an4_plus.trigram.lm \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_plus.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch

an4_plus_trigram_quick_bin:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=50\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-36\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=an4_plus.trigram.lm \
        -Dedu.cmu.sphinx.search.ExternalLinguist.path=an4_plus.hmm.bin\
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_plus.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.bin.props ./an4_full.batch

# Tests the 105 words and letters from AN4, using a trigram LM
# created from full AN4 plus the 100 most common words
# in English.

an4_plus_trigram_super_quick:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=500\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=an4_plus.trigram.lm \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_plus.wordlist \
        edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch



# Tests the 105 words and letters from AN4, using an FST created from
# a trigram LM using the full AN4 plus the 100 most common words
# in English.

an4_plus_fst:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.SimpleLinguist \
        -Dedu.cmu.sphinx.search.Recognizer.searchManager=edu.cmu.sphinx.search.SimpleBreadthFirstSearchManager \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-25 \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./an4_plus.trigram.fst \
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_full.batch


##########################################################################
#
# All other miscellaneous AN4 tests go below.
#
##########################################################################



########################################################
# Runs all the tests from a batch file using qsub
########################################################

batch:
	qsub  -o batch.out -e batch.err run_an4_tests

########################################################
# This set of targets are used for low level search testing
########################################################

an4_words_unigram_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=25\
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist\
	-Dedu.cmu.sphinx.search.ActiveList.relativeBeamWidth=1E-250 \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.search.WordListGrammar.addSilenceWord=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch



#################################################
# Bushderby test target with high eta
#################################################
bush_test:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist\
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	-Dedu.cmu.sphinx.search.ActiveList.relativeBeamWidth=1E-250 \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.enableBushderby=true \
	-Dedu.cmu.sphinx.search.ActiveList.strictPruning=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.bushderbyEta=10000 \
	-Dedu.cmu.sphinx.search.WordListGrammar.addSilenceWord=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch


#################################################
# Bushderby test target
#################################################
bush_test2:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist\
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	-Dedu.cmu.sphinx.search.ActiveList.relativeBeamWidth=1E-250 \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.enableBushderby=true \
	-Dedu.cmu.sphinx.search.ActiveList.strictPruning=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.bushderbyEta=.5 \
	-Dedu.cmu.sphinx.search.WordListGrammar.addSilenceWord=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-25\
	edu.cmu.sphinx.decoder.BatchDecoder ./an4.props ./an4_words.batch

