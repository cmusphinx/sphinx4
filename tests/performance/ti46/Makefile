# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
# In following "ti46" refers to:
#
# "NIST CD-ROM Version of the Texas Instruments-developed 46-Word
# Speaker-Dependent Isolated Word Speech Database"
#
# "tidigits" refers to the:
#
# "NIST CD-ROM Version of the Texas Instruments-developed Studio Quality
# Speaker-Independent Connected-Digit Corpus".
#
# Makefile for all the TI46 tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/ti46

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

PROFILE_FLAGS= 
LOG_GC_FLAGS=  
GC_FLAGS = -ms16m -mx16m

DEBUG_FLAGS=

EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config

SKIP=0

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: 


#
# ti46 with the wordlist grammar, the simplest way to specify a flat unigram
#

ti46_wordlist:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

#
# a quick form of ti46_wordlist
#

ti46_wordlist_quick:
	$(MAKE) SKIP=5 ti46_wordlist

#
# ti46 with an ARPA format flat unigram
#
ti46_flat_unigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
        -Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.LMGrammar\
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./ti46.flat_unigram.lm\
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

#
# a quick form 
#
ti46_flat_unigram_quick:
	$(MAKE) SKIP=5 ti46_flat_unigram

#
# ti46 with and FST format flat unigram
#
ti46_flat_unigram_fst:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./ti46.flat_unigram.fst \
	edu.cmu.sphinx.decoder.BatchDecoder ./ti46.props ./ti46.batch


#
# a quick form 
#

ti46_flat_unigram_fst_quick:
	$(MAKE) SKIP=5 ti46_flat_unigram_fst

##########################################################################
#
#  Older tests, (of historical interest)
#
##########################################################################


#
# The full TI46 (isolated digits) test.
#
ti46:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=false \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# The full TI46 (isolated digits) test using the TreeLinguist.
#
ti46_tree:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=false \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-20\
	-Dedu.cmu.sphinx.search.StaticLinguist.isFlatSentenceHMM=false \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test.
#
ti46_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=false \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# The full TI46 (isolated digits) test, using a looping grammar.
#
ti46_multi:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=true \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test that uses a
# looping grammar.
#
ti46_multi_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=true \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

##########################################################################
#
# The TI46 tests that DO NOT use unit context and composite senones.
#
##########################################################################


#
# The full TI46 (isolated digits) test.
#
ti46_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=false \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test.
#
ti46_quick_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=false \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# The full TI46 (isolated digits) test, using a looping grammar.
#
ti46_multi_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=true \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test that uses a
# looping grammar.
#
ti46_multi_quick_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.SimpleWordListGrammar.isLooping=true \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch
