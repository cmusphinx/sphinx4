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

rm1onwsj_flatunigram:
	$(RM1_FLATUNIGRAM_CMD) ./rm1onwsj.props ./rm1.batch

rm1onwsj_flatunigram_quick:
	$(RM1_FLATUNIGRAM_QUICK_CMD) ./rm1onwsj.props ./rm1.batch

q_rm1onwsj_flatunigram:
	echo "$(RM1_FLATUNIGRAM_CMD) ./rm1onwsj.props ./rm1.batch" > rm1onwsj.flatunigram.csh
	chmod a+x rm1onwsj.flatunigram.csh
	$(QSUB) -j oe -o rm1onwsj.flatunigram.log rm1onwsj.flatunigram.csh
	rm rm1onwsj.flatunigram.csh

#
# RM1 unigram test
#

RM1_UNIGRAM_CMD = $(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10 \
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=7 \
	-Dedu.cmu.sphinx.model.language.location=RM.2845.unigram.arpa \
	edu.cmu.sphinx.decoder.BatchDecoder

RM1_UNIGRAM_QUICK_CMD = $(JAVA_CMD) \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10 \
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=7 \
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

rm1onwsj_unigram:
	$(RM1_UNIGRAM_CMD) ./rm1onwsj.props ./rm1.batch

rm1onwsj_unigram_quick:
	$(RM1_UNIGRAM_QUICK_CMD) ./rm1onwsj.props ./rm1.batch

q_rm1onwsj_unigram:
	echo "$(RM1_UNIGRAM_CMD) ./rm1onwsj.props ./rm1.batch" > rm1onwsj.unigram.csh
	chmod a+x rm1onwsj.unigram.csh
	$(QSUB) -j oe -o rm1onwsj.unigram.log rm1onwsj.unigram.csh
	rm rm1onwsj.unigram.csh

#
# RM1 bigram test
#

RM1_BIGRAM_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.location=RM.2845.bigram.arpa \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10 \
        -Dedu.cmu.sphinx.search.BreadthFirstSearchManager.languageWeight=10 \
	edu.cmu.sphinx.decoder.BatchDecoder

rm1_bigram:
	$(RM1_BIGRAM_CMD) ./rm1.props ./rm1.batch

q_rm1_bigram:
	echo "$(RM1_BIGRAM_CMD) ./rm1.props ./rm1.batch" > rm1.bigram.csh
	chmod a+x rm1.bigram.csh
	$(QSUB) -j oe -o rm1.bigram.log rm1.bigram.csh
	rm rm1.bigram.csh

rm1onwsj_bigram:
	$(RM1_BIGRAM_CMD) ./rm1onwsj.props ./rm1.batch

q_rm1onwsj_bigram:
	echo "$(RM1_BIGRAM_CMD) ./rm1onwsj.props ./rm1.batch" > rm1onwsj.bigram.csh
	chmod a+x rm1onwsj.bigram.csh
	$(QSUB) -j oe -o rm1onwsj.bigram.log rm1onwsj.bigram.csh
	rm rm1onwsj.bigram.csh

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

rm1onwsj_trigram:
	$(RM1_TRIGRAM_CMD) ./rm1onwsj.props ./rm1.batch


q_rm1onwsj_trigram:
	echo "$(RM1_TRIGRAM_CMD) ./rm1onwsj.props ./rm1.batch" > rm1onwsj.trigram.csh
	chmod a+x rm1onwsj.trigram.csh
	$(QSUB) -j oe -o rm1onwsj.trigram.log rm1onwsj.trigram.csh
	rm rm1onwsj.trigram.csh

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

rm1onwsj_fst:
	$(RM1_FST_CMD) ./rm1onwsj.props ./rm1.batch

q_rm1onwsj_fst:
	echo "$(RM1_FST_CMD) ./rm1onwsj.props ./rm1.batch" > rm1onwsj.trigram.fst.csh
	chmod a+x rm1onwsj.trigram.fst.csh
	$(QSUB) -j oe -o rm1onwsj.trigram.fst.log rm1onwsj.trigram.fst.csh
	rm rm1onwsj.trigram.fst.csh

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

rm1onwsj_wordpair:
	$(RM1_WORDPAIR_CMD) ./rm1onwsj.props ./rm1.batch

q_rm1onwsj_wordpair:
	echo "$(RM1_WORDPAIR_CMD) ./rm1onwsj.props ./rm1.batch" > rm1onwsj.wordpair.csh
	chmod a+x rm1onwsj.wordpair.csh
	$(QSUB) -j oe -o rm1onwsj.wordpair.log rm1onwsj.wordpair.csh
	rm rm1onwsj.wordpair.csh

