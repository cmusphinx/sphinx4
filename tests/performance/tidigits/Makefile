# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
# "tidigits" refers to the:
#
# "NIST CD-ROM Version of the Texas Instruments-developed Studio Quality
# Speaker-Independent Connected-Digit Corpus".
#
# Makefile for all the TIDIGITS tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/tidigits

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)


PROFILE_FLAGS= 
GC_FLAGS = -ms128m -mx128m
DEBUG_FLAGS= 

EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS) 

##########################################################################

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

ifeq ($(MODE), super_quick)
  SKIP = 100
endif

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}



##########################################################################
#
# The TIDIGITS tests that uses TreeLinguist.
#
##########################################################################

# The full TIDGITS test.  Takes a long long time to run

#
# tidigits with the wordlist grammar-the simplest way to specify a flat unigram
#
tidigits_wordlist:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch


# tidigits with and flat unigram

tidigits_flat_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.flat_unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch


#
# tidigits with and FST format flat unigram
#
tidigits_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar\
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./tidigits.flat_unigram.fst \
    edu.cmu.sphinx.decoder.BatchDecoder ./tidigits.props ./tidigits.batch



##########################################################################
#
# Low-level Test targets - the following targets are used for internal
# decoder testing and are not generally useful, nor are they
# maintained. Use at your risk
#
##########################################################################
# tidigits with and flat unigram and lex linguist

tidigits_flat_unigram_lex:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=1 \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.flat_unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch

# tidigits with and bigram and lex linguist

tidigits_bigram_lex:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.bigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch

# tidigits with flat linguist and bigram

tidigits_bigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.bigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch

# tidigits with flat linguist and bigram

tidigits_bigram_flat:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.bigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch

tidigits_flat_unigram_flat:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.flat.FlatDynamicLinguist \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=2 \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.flat_unigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# tidigits with and trigram and lex linguist

tidigits_trigram_lex:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.maxDepth=3 \
    -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./tidigits.trigram.lm\
    edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch


##########################################################################
#
# Live mode TIDIGITS tests.
#
##########################################################################

tidigits_wordlist_live:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.frontend.endpoint.SpeechMarker.speechTrailer=50 \
	-Dedu.cmu.sphinx.frontend.util.ConcatFileAudioSource.skip=$(SKIP)\
	-Dedu.cmu.sphinx.frontend.util.ConcatFileAudioSource.silenceFile=/lab/speech/sphinx4/data/tidigits/test/raw16k/silence1sec.raw \
	-Dedu.cmu.sphinx.frontend.keepAudioReference=false \
	-Dedu.cmu.sphinx.frontend.bytesPerAudioFrame=320 \
	-Dedu.cmu.sphinx.frontend.cmn=edu.cmu.sphinx.frontend.LiveCMN \
	-Dedu.cmu.sphinx.frontend.featureExtractor=edu.cmu.sphinx.frontend.LiveFeatureExtractor\
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.LevelTracker \
	-Dedu.cmu.sphinx.frontend.endpoint.LevelTracker.threshold=16 \
	-Dedu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer.scoreablesKeepFeature=true \
	-Dedu.cmu.sphinx.decoder.LivePretendDecoder.alignInterval=870 \
	edu.cmu.sphinx.decoder.LivePretendDecoder tidigits.props tidigits.batch


tidigits_an4_words_rejection_live:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.frontend.endpoint.SpeechMarker.speechTrailer=50 \
	-Dedu.cmu.sphinx.frontend.util.ConcatFileAudioSource.skip=$(SKIP)\
	-Dedu.cmu.sphinx.frontend.util.ConcatFileAudioSource.silenceFile=/lab/speech/sphinx4/data/tidigits/test/raw16k/silence1sec.raw \
	-Dedu.cmu.sphinx.frontend.keepAudioReference=false \
	-Dedu.cmu.sphinx.frontend.bytesPerAudioFrame=320 \
	-Dedu.cmu.sphinx.frontend.cmn=edu.cmu.sphinx.frontend.LiveCMN \
	-Dedu.cmu.sphinx.frontend.featureExtractor=edu.cmu.sphinx.frontend.LiveFeatureExtractor\
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.LevelTracker \
	-Dedu.cmu.sphinx.frontend.endpoint.LevelTracker.threshold=16 \
	-Dedu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer.scoreablesKeepFeature=true \
	-Dedu.cmu.sphinx.decoder.LivePretendDecoder.alignInterval=870 \
	edu.cmu.sphinx.decoder.LivePretendDecoder tidigits.props ../an4/an4_words.batch
