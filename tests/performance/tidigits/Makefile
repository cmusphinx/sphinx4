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

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

SKIP=0

all:: 
	$(MAKE) tidigits_wordlist
	$(MAKE) tidigits_flat_unigram
	$(MAKE) tidigits_flat_unigram_fst


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



tidigits_flat_unigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.LMGrammar\
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=./tidigits.flat_unigram.lm\
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch


#
# tidigits with and FST format flat unigram
#
tidigits_flat_unigram_fst:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=./tidigits.flat_unigram.fst \
	edu.cmu.sphinx.decoder.BatchDecoder ./tidigits.props ./tidigits.batch


#
# Quick versions
#

tidigits_wordlist_quick:
	$(MAKE) SKIP=5 tidigits_wordlist

tidigits_flat_unigram_quick:
	$(MAKE) SKIP=5 tidigits_flat_unigram

tidigits_flat_unigram_fst_quick:
	$(MAKE) SKIP=5 tidigits_flat_unigram_fst

