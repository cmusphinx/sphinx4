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
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all::


##########################################################################
#
# RM1 tests.
#
##########################################################################

# Define QSUB as qsub (PBS job submission). Set it to the proper
# location if qsub is not on your path.

QSUB = qsub

#
# The default RM1 test using a looping grammar for all words in RM1.
#

#
# RM1 flat unigram test
#
RM1_FLATUNIGRAM_CMD = $(JAVA_CMD) \
	edu.cmu.sphinx.decoder.BatchDecoder

RM1_FLATUNIGRAM_QUICK_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=8\
	edu.cmu.sphinx.decoder.BatchDecoder

rm1_flatunigram:
	$(RM1_FLATUNIGRAM_CMD) ./rm1.props ./rm1.batch

rm1_flatunigram_quick:
	$(RM1_FLATUNIGRAM_QUICK_CMD) ./rm1.props ./rm1.batch


q_rm1_flatunigram:
	echo "$(RM1_FLATUNIGRAM_CMD) ./rm1.props ./rm1.batch" > rm1.flatunigram.csh
	chmod a+x rm1.flatunigram.csh
	$(QSUB) -j oe -o rm1.flatunigram.log rm1.flatunigram.csh
	rm rm1.flatunigram.csh

rm1onrm1_flatunigram:
	$(RM1_FLATUNIGRAM_CMD) ./rm1onrm1.props ./rm1.batch

rm1onrm1_flatunigram_quick:
	$(RM1_FLATUNIGRAM_QUICK_CMD) ./rm1onrm1.props ./rm1.batch

q_rm1onrm1_flatunigram:
	echo "$(RM1_FLATUNIGRAM_CMD) ./rm1onrm1.props ./rm1.batch" > rm1onrm1.flatunigram.csh
	chmod a+x rm1onrm1.flatunigram.csh
	$(QSUB) -j oe -o rm1onrm1.flatunigram.log rm1onrm1.flatunigram.csh
	rm rm1onrm1.flatunigram.csh

#
# RM1 unigram test
#

RM1_UNIGRAM_CMD = $(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10 \
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=9 \
	-Dedu.cmu.sphinx.model.language.location=RM.2845.unigram.arpa \
	edu.cmu.sphinx.decoder.BatchDecoder

RM1_UNIGRAM_QUICK_CMD = $(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10 \
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=9 \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=8\
	-Dedu.cmu.sphinx.model.language.location=RM.2845.unigram.arpa \
	edu.cmu.sphinx.decoder.BatchDecoder


rm1_unigram:
	$(RM1_UNIGRAM_CMD) ./rm1.props ./rm1.batch

rm1_unigram_quick:
	$(RM1_UNIGRAM_QUICK_CMD) ./rm1.props ./rm1.batch


q_rm1_unigram:
	echo "$(RM1_UNIGRAM_CMD) ./rm1.props ./rm1.batch" > rm1.unigram.csh
	chmod a+x rm1.unigram.csh
	$(QSUB) -j oe -o rm1.unigram.log rm1.unigram.csh
	rm rm1.unigram.csh

rm1onrm1_unigram:
	$(RM1_UNIGRAM_CMD) ./rm1onrm1.props ./rm1.batch

rm1onrm1_unigram_quick:
	$(RM1_UNIGRAM_QUICK_CMD) ./rm1onrm1.props ./rm1.batch

q_rm1onrm1_unigram:
	echo "$(RM1_UNIGRAM_CMD) ./rm1onrm1.props ./rm1.batch" > rm1onrm1.unigram.csh
	chmod a+x rm1onrm1.unigram.csh
	$(QSUB) -j oe -o rm1onrm1.unigram.log rm1onrm1.unigram.csh
	rm rm1onrm1.unigram.csh

#
# RM1 bigram test
#

RM1_BIGRAM_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.location=RM.2845.bigram.arpa \
	edu.cmu.sphinx.decoder.BatchDecoder

rm1_bigram:
	$(RM1_BIGRAM_CMD) ./rm1.props ./rm1.batch

q_rm1_bigram:
	echo "$(RM1_BIGRAM_CMD) ./rm1.props ./rm1.batch" > rm1.bigram.csh
	chmod a+x rm1.bigram.csh
	$(QSUB) -j oe -o rm1.bigram.log rm1.bigram.csh
	rm rm1.bigram.csh

rm1onrm1_bigram:
	$(RM1_BIGRAM_CMD) ./rm1onrm1.props ./rm1.batch

q_rm1onrm1_bigram:
	echo "$(RM1_BIGRAM_CMD) ./rm1onrm1.props ./rm1.batch" > rm1onrm1.bigram.csh
	chmod a+x rm1onrm1.bigram.csh
	$(QSUB) -j oe -o rm1onrm1.bigram.log rm1onrm1.bigram.csh
	rm rm1onrm1.bigram.csh

#
# RM1 trigram test
#

RM1_TRIGRAM_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.location=RM.2845.trigram.arpa \
	edu.cmu.sphinx.decoder.BatchDecoder

rm1_trigram:
	$(RM1_TRIGRAM_CMD) ./rm1.props ./rm1.batch


q_rm1_trigram:
	echo "$(RM1_TRIGRAM_CMD) ./rm1.props ./rm1.batch" > rm1.trigram.csh
	chmod a+x rm1.trigram.csh
	$(QSUB) -j oe -o rm1.trigram.log rm1.trigram.csh
	rm rm1.trigram.csh

rm1onrm1_trigram:
	$(RM1_TRIGRAM_CMD) ./rm1onrm1.props ./rm1.batch


q_rm1onrm1_trigram:
	echo "$(RM1_TRIGRAM_CMD) ./rm1onrm1.props ./rm1.batch" > rm1onrm1.trigram.csh
	chmod a+x rm1onrm1.trigram.csh
	$(QSUB) -j oe -o rm1onrm1.trigram.log rm1onrm1.trigram.csh
	rm rm1onrm1.trigram.csh

#
# RM1 finite state transducer test
#

RM1_FST_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.FSTGrammar \
	-Dedu.cmu.sphinx.search.FSTGrammar.path=RM.2845.trigram.arpa.fst \
	edu.cmu.sphinx.decoder.BatchDecoder

rm1_fst:
	$(RM1_FST_CMD) ./rm1.props ./rm1.batch

q_rm1_fst:
	echo "$(RM1_FST_CMD) ./rm1.props ./rm1.batch" > rm1.trigram.fst.csh
	chmod a+x rm1.trigram.fst.csh
	$(QSUB) -j oe -o rm1.trigram.fst.log rm1.trigram.fst.csh
	rm rm1.trigram.fst.csh

rm1onrm1_fst:
	$(RM1_FST_CMD) ./rm1onrm1.props ./rm1.batch

q_rm1onrm1_fst:
	echo "$(RM1_FST_CMD) ./rm1onrm1.props ./rm1.batch" > rm1onrm1.trigram.fst.csh
	chmod a+x rm1onrm1.trigram.fst.csh
	$(QSUB) -j oe -o rm1onrm1.trigram.fst.log rm1onrm1.trigram.fst.csh
	rm rm1onrm1.trigram.fst.csh

#
# RM1 word pair test
#

RM1_WORDPAIR_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.location=RM.wordpair.arpa \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_wordpair:
	$(RM1_WORDPAIR_CMD) ./rm1.props ./rm1.batch

q_rm1_wordpair:
	echo "$(RM1_WORDPAIR_CMD) ./rm1.props ./rm1.batch" > rm1.wordpair.csh
	chmod a+x rm1.wordpair.csh
	$(QSUB) -j oe -o rm1.wordpair.log rm1.wordpair.csh
	rm rm1.wordpair.csh

rm1onrm1_wordpair:
	$(RM1_WORDPAIR_CMD) ./rm1onrm1.props ./rm1.batch

q_rm1onrm1_wordpair:
	echo "$(RM1_WORDPAIR_CMD) ./rm1onrm1.props ./rm1.batch" > rm1onrm1.wordpair.csh
	chmod a+x rm1onrm1.wordpair.csh
	$(QSUB) -j oe -o rm1onrm1.wordpair.log rm1onrm1.wordpair.csh
	rm rm1onrm1.wordpair.csh

