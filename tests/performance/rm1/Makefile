# Copyrigh 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

# Nothing.

#
# Makefile for all the RM1 tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/rm1

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =


GCSPY_FLAGS= -client -Xport3000 -Xverbose -Xwait
PROFILE_FLAGS= $(GCSPY_FLAGS)
PROFILE_FLAGS= -Xprof -server

LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms256m -mx512m

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

DEBUG_FLAGS= -XX:+UsePerfData
DEBUG_FLAGS= 

EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################
# 
# check the MODE setting. If invoked with MODE=quick, run a quick
# version of the test (5 times quicker)
# 
# example:      make MODE=quick rm1_flat_unigram
#
#

ifeq ($(MODE),quick)
  SKIP = 5
else
  SKIP = 0
endif

ifeq ($(MODE),super_quick)
  SKIP = 200
endif

ifeq ($(HEAP),large)
GC_FLAGS = -ms1024m -mx1600m
endif

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}



rm1_flat_unigram_simple:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=8 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.flat_unigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram_simple:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=7 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram_simple:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch


rm1_flat_unigram:
	$(JAVA_CMD) \
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.flat_unigram.lm \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=9.5 \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=6000\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_unigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_bigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160 \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch


rm1_trigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=18.0 \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
        -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.maxWordsPerFrame=70 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch


rt:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.trigram.props ./rm1.batch

rm1_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./rm1.flat_unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./rm1.unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./rm1.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_trigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=10.0 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./rm1.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

run_batch:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.whichBatch=$(WHICH_BATCH)\
        -Dedu.cmu.sphinx.decoder.BatchDecoder.totalBatches=$(TOTAL_BATCHES)\
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        $(PARSED_EXTRA_OPTIONS) \
        edu.cmu.sphinx.decoder.BatchDecoder $(PROPS) $(BATCH_FILE) 
