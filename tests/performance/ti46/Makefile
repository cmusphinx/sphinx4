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
# Makefile for all the TI46 tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/ti46

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

PROFILE_FLAGS= 
LOG_GC_FLAGS=  
GC_FLAGS = -ms16m -mx16m

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

include ${TOP}/build/Makefile.config


CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}


#
# ti46 with the wordlist grammar, the simplest way to specify a flat unigram
#

ti46_wordlist:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

#
# ti46 with an ARPA format flat unigram
#
ti46_flat_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.knowledge.language.SimpleNGramModel \
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.LMGrammar\
    -Dedu.cmu.sphinx.knowledge.language.location=./ti46.flat_unigram.lm \
    edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

#
# ti46 with and FST format flat unigram
#
ti46_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.decoder.Recognizer.grammar=edu.cmu.sphinx.decoder.linguist.FSTGrammar \
    -Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=./ti46.flat_unigram.fst \
    edu.cmu.sphinx.decoder.BatchDecoder ./ti46.props ./ti46.batch

