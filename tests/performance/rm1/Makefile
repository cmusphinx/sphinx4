# Copyright 1999-2002 Carnegie Mellon University.  
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

PROFILE_FLAGS= -Xprof -server
LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms256m -mx800m

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

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

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}



rm1_flat_unigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=7 \
	-Dedu.cmu.sphinx.model.language.location=rm1.flat_unigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=7 \
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.location=rm1.unigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        -Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.location=rm1.bigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_flat_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
    -Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar\
    -Dedu.cmu.sphinx.search.FSTGrammar.path=./rm1.flat_unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
    -Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar\
    -Dedu.cmu.sphinx.search.FSTGrammar.path=./rm1.unigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
    -Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar\
    -Dedu.cmu.sphinx.search.FSTGrammar.path=./rm1.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_trigram_fst:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10.0 \
    -Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar\
    -Dedu.cmu.sphinx.search.FSTGrammar.path=./rm1.bigram.fst\
    edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch
