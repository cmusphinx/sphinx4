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
#GC_FLAGS = -ms128m -mx512m
GC_FLAGS = -ms128m -mx2048m


DEBUG_FLAGS= 
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)


HUB4_DIR=/lab/speech/sphinx4/data/hub4/eval99/supervised

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


# hub4_f0 with wsj models

#-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./hub4.flat_unigram.lm\

hub4_flat_unigram:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	edu.cmu.sphinx.decoder.BatchDecoder ./hub4.props \
        $(HUB4_DIR)/f0_hub4.batch

test:
	$(JAVA_CMD) \
    -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
    -Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./hub4.flat_unigram.lm\
	edu.cmu.sphinx.decoder.BatchDecoder ./hub4.props \
        test.batch

run_batch:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.whichBatch=$(WHICH_BATCH)\
        -Dedu.cmu.sphinx.decoder.BatchDecoder.totalBatches=$(TOTAL_BATCHES)\
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        $(PARSED_EXTRA_OPTIONS) \
        edu.cmu.sphinx.decoder.BatchDecoder $(PROPS) $(BATCH_FILE) 
