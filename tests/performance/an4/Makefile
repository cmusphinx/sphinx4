# [[[copyright]]] 
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



PROFILE_FLAGS= -Xprof -server
LOG_GC_FLAGS=  -Xloggc:gc.txt
 GC_FLAGS=  -XX:+PrintGCDetails
# GC_FLAGS=  -XX:+UseConcMarkSweepGC
#GC_FLAGS=  -XX:+UseParallelGC
#GC_FLAGS= -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails
HEAP_DUMP =   -Xrunhprof:heap=dump,cpu=samples
#GC_FLAGS= -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails -XX:+TraceGen1Time

# GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

#GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails
#GC_FLAGS= -ms1g -XX:+UseConcMarkSweepGC -XX:-UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

GC_FLAGS = -ms1g -mx1g

#PROFILE2_FLAGS= -Xrunhprof:file=log.txt
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt

DEBUG_FLAGS=
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}


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
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist \
        tests.decoder.BatchDecoder ./wsjDigits.props ./an4_words.batch



# Tests the 79 whole words from AN4, using a trigram LM based on 
# the spoken words of AN4.

an4_words_trigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_words.arpa \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_words.wordlist \
        tests.decoder.BatchDecoder ./wsjDigits.props ./an4_words.batch



# Tests the 79 whole words from AN4, using an FST created from
# the trigram LM based on the spoken words of AN4.

an4_words_fst:
        $(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=400 \
	-Dedu.cmu.sphinx.search.Dictionary.allowMissingWords=true \
        -Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
        -Dedu.cmu.sphinx.search.Linguist.joinPronunciations=true \
        -Dedu.cmu.sphinx.search.Linguist.showCompilationProgress=true \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.ArpaGrammar \
	-Dedu.cmu.sphinx.search.ArpaGrammar.path=./an4_words.arpa \
        tests.decoder.BatchDecoder wsjDigits.props ./an4_words.batch



# Tests the spelled-out words from AN4,
# using a looping WordListGrammar of the 26 letters of the alphabet.

an4_spelling_unigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_spelling.alphalist \
        tests.decoder.BatchDecoder ./wsjDigits.props ./an4_spelling.batch



# Tests the spelled-out words from AN4,
# using an trigram LM based on spelling of words from AN4.

an4_spelling_trigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./an4_spelling.arpa \
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./an4_spelling.alphalist \
        tests.decoder.BatchDecoder ./wsjDigits.props ./an4_spelling.batch



# Tests the spelled-out words from AN4,
# using an FST created from the trigram LM based on spelling of words from AN4.

an4_spelling_fst:
        $(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=400 \
	-Dedu.cmu.sphinx.search.Dictionary.allowMissingWords=true \
        -Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
        -Dedu.cmu.sphinx.search.Linguist.joinPronunciations=true \
        -Dedu.cmu.sphinx.search.Linguist.showCompilationProgress=true \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.ArpaGrammar \
	-Dedu.cmu.sphinx.search.ArpaGrammar.path=./an4_spelling.arpa \
        tests.decoder.BatchDecoder wsjDigits.props ./an4_spelling.batch



# Tests the spelled-out words from AN4,
# using an trigram LM based on spelling of words from
# "Journey to the Center of the Earth". 

an4_spelling_journey_trigram:


# Tests the spelled-out words from AN4,
# using an FST created from the trigram LM based on spelling of words
# from "Journey to the Center of the Earth". 

an4_spelling_journey_fst:



# Tests the 105 words and letters from AN4, using a looping WordListGrammar
# whose vocabulary is all words from AN4 and the 26 letters in the alphabet.

an4_full_unigram:



# Tests the 105 words and letters from AN4, using a trigram LM
# created from the words and spelled-out words of AN4.

an4_full_trigram:



# Tests the 105 words and letters from AN4, using an FST created from
# a trigram LM using the words and spelled-out words of AN4.

an4_full_fst:



# Tests the 105 words and letters from AN4, using a looping
# WordListGrammar created from full AN4 plus the 100 most common words
# in English.

an4_plus_unigram:



# Tests the 105 words and letters from AN4, using a trigram LM
# created from full AN4 plus the 100 most common words
# in English.

an4_plus_trigram:



# Tests the 105 words and letters from AN4, using an FST created from
# a trigram LM using the full AN4 plus the 100 most common words
# in English.

an4_plus_fst:



##########################################################################
#
# All other miscellaneous AN4 tests go below.
#
##########################################################################

