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
GC_FLAGS = -ms256m -mx512m -XX:NewSize=100m  -XX:MaxNewSize=100m 

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


########################
# The main test targest
# ###################

rm1_flat_unigram:
	$(MAKE) rm1_flat_unigram_clex

rm1_unigram:
	$(MAKE) rm1_unigram_clex

rm1_bigram:
	$(MAKE) rm1_bigram_clex

rm1_trigram:
	$(MAKE) rm1_trigram_clex

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
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram_simple:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch


rm1_flat_unigram_lex:
	$(JAVA_CMD) \
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.flat_unigram.lm \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=9.5 \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=6000\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_unigram_lex:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_bigram_lex:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160 \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch


rm1_bigram_beamfinder:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
        -Dedu.cmu.sphinx.decoder.Decoder.showBestToken=true \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.util.BeamFinder.enable=true\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=3000\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-200 \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_bigram_little_beam:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=1200\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-60 \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_bigram_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140 \
    -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_trigram_lex:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=18.0 \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.9 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
        -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.maxWordsPerFrame=70 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch


# ========================================
# clextree targets
# ========================================
# clextree targets use the compressed hmm
# lextree to represent the search space

rm1_flat_unigram_clex:
	$(JAVA_CMD) \
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.flat_unigram.lm \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=9.5 \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
   -Dedu.cmu.sphinx.util.BeamFinder.enable=false\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=1600\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-70\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_unigram_clex:
	$(JAVA_CMD) \
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.flat_unigram.lm \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=15 \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
   -Dedu.cmu.sphinx.util.BeamFinder.enable=true\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2600\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-210\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_bigram_clex:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=1000 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_bigram_clex_smear:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=1000 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.wantUnigramSmear=true \
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.fullWordHistories=true \
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.unigramSmearWeight=.150 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch


rm1_trigram_clex:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=20.0 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=1400 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-115 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_trigram_clex_smear:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=20.0 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-115 \
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.wantUnigramSmear=true \
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.unigramSmearWeight=.150 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_trigram_clex_smear_best:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=24.0 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=8000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140 \
   -Dedu.cmu.sphinx.util.BeamFinder.enable=false\
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.wantUnigramSmear=false \
   -Dedu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.unigramSmearWeight=.150 \
  -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_trigram_clex_smear_fast:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=24.0 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-130 \
   -Dedu.cmu.sphinx.util.BeamFinder.enable=false\
  -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

# -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=8000 \

rm1_bigram_clex_beamfinder:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.util.BeamFinder.enable=true\
        -Dedu.cmu.sphinx.decoder.Decoder.showBestToken=false \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=3000\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-200 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch

rm1_trigram_clex_beamfinder:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm\
       -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=27.0 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=4000 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-200 \
   -Dedu.cmu.sphinx.util.BeamFinder.enable=true\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.maxWordsPerFrame=70 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.clex.props ./rm1.batch



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

##########################################################################
#
# Low-level Test targets - the following targets are used for internal
# decoder testing and are not generally useful, nor are they
# maintained. Use at your risk
#
##########################################################################
rm1_flat_unigram_lex_min:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.flat_unigram.lm\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight9.5 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=6000 \
    -Dedu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist.fullWordHistories=false\
    -Dedu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist.maintainSeparateWordRC=false\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
   -Dedu.cmu.sphinx.decoder.Recognizer.linguistProcessors=edu.cmu.sphinx.decoder.linguist.util.LinguistStats \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.absoluteWordBeamWidth=200000 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch


rm1_bigram_old_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
        -Dedu.cmu.sphinx.decoder.Decoder.showBestToken=false \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=17 \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140\
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram_flat_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
        -Dedu.cmu.sphinx.decoder.Decoder.showBestToken=false \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=17 \
        -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist \
       -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140\
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_trigram_flat_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=3 \
        -Dedu.cmu.sphinx.decoder.Decoder.showBestToken=false \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=27 \
        -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist \
       -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=12000\
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-120 \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram_flat_dynamic_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=13 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-120 \
   -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=-1 \
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-60 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
        -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist \
       -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram_lex_dynamic_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=13 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-120 \
   -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-60 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

rm1_unigram_flat_static_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=13 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-120 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
   -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-60 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
       -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch



rm1_bigram_flat_dynamic_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=17 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140 \
   -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=-1 \
    -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.absoluteWordBeamWidth=-1 \
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
        -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist \
       -Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.allowMissingWords=true \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram_flat_static_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=17 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140 \
   -Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=-1 \
   -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm \
        -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch


# rm1 unigram with no word histories (nwh)

rm1_unigram_lex_nwh:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.unigram.lm\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
    -Dedu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist.fullWordHistories=false\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=13 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch



rm1_bigram_lex_test:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=17 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.2\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=-1 \
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch

   # -Dedu.cmu.sphinx.decoder.Recognizer.linguistProcessors=edu.cmu.sphinx.decoder.linguist.util.LinguistStats \
rm1_bigram_lex_min:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=rm1.bigram.lm\
  -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=17 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=2000 \
    -Dedu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist.fullWordHistories=false\
    -Dedu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist.maintainSeparateWordRC=false\
   -Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-140 \
   -Dedu.cmu.sphinx.decoder.Recognizer.linguistProcessors=edu.cmu.sphinx.decoder.linguist.util.LinguistStats \
   -Dedu.cmu.sphinx.knowledge.language.LanguageModel.unigramWeight=.7 \
-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-100 \
-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.absoluteWordBeamWidth=20 \
-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.maxWordsPerFrame=700 \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.lex.props ./rm1.batch



