# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
# Makefile for all the RM1 tests.
#

# test commit notification changes.

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/rm1

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=ti46.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)



PROFILE_FLAGS= -Xprof -server
LOG_GC_FLAGS=  -Xloggc:gc.txt
# GC_FLAGS=  -XX:+PrintGCDetails
GC_FLAGS = -ms256m -mx512m
# GC_FLAGS=  -XX:+UseConcMarkSweepGC
#GC_FLAGS=  -XX:+UseParallelGC
#GC_FLAGS= -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails
HEAP_DUMP =   -Xrunhprof:heap=dump,cpu=samples
#GC_FLAGS= -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails -XX:+TraceGen1Time

#GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=50m -XX:MaxNewSize=50m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

#GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails
#GC_FLAGS= -ms1g -XX:+UseConcMarkSweepGC -XX:-UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails


#PROFILE2_FLAGS= -Xrunhprof:file=log.txt
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt
DEBUG_FLAGS=
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all::

ngram: rm1_unigram rm1_bigram rm1_trigram

q_ngram: q_rm1_unigram q_rm1_bigram q_rm1_trigram



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
# RM1 unigram test
#

RM1_UNIGRAM_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=RM.unigram.arpa \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Dictionary.dictionaryPath=dict/cmudict.0.6d+rm1.txt \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10\
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./rm1.wordlist \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_unigram:
	$(RM1_UNIGRAM_CMD)

q_rm1_unigram:
	echo "$(RM1_UNIGRAM_CMD)" > rm1.unigram.csh
	chmod a+x rm1.unigram.csh
	$(QSUB) -j oe -o rm1.unigram.log rm1.unigram.csh
	rm rm1.unigram.csh

#
# RM1 bigram test
#

RM1_BIGRAM_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=RM.2880.bigram.arpa \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Dictionary.dictionaryPath=dict/cmudict.0.6d+rm1.txt \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10\
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./rm1.wordlist \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_bigram:
	$(RM1_BIGRAM_CMD)

q_rm1_bigram:
	echo "$(RM1_BIGRAM_CMD)" > rm1.bigram.csh
	chmod a+x rm1.bigram.csh
	$(QSUB) -j oe -o rm1.bigram.log rm1.bigram.csh
	rm rm1.bigram.csh

#
# RM1 trigram test
#

RM1_TRIGRAM_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=RM.2880.trigram.arpa \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Dictionary.dictionaryPath=dict/cmudict.0.6d+rm1.txt \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10\
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./rm1.wordlist \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_trigram:
	$(RM1_TRIGRAM_CMD)

q_rm1_trigram:
	echo "$(RM1_TRIGRAM_CMD)" > rm1.trigram.csh
	chmod a+x rm1.trigram.csh
	$(QSUB) -j oe -o rm1.trigram.log rm1.trigram.csh
	rm rm1.trigram.csh

#
# RM1 finite state transducer test
#

RM1_FST_CMD = $(JAVA_CMD) \
        -Dedu.cmu.sphinx.model.acoustic.useComposites=false \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Dictionary.dictionaryPath=dict/cmudict.0.6d+rm1.txt \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10\
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.ArpaGrammar \
	-Dedu.cmu.sphinx.search.ArpaGrammar.path=RM.2880.trigram.arpa.fst \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_fst:
	$(RM1_FST_CMD)

q_rm1_fst:
	echo "$(RM1_FST_CMD)" > rm1.trigram.fst.csh
	chmod a+x rm1.trigram.fst.csh
	$(QSUB) -j oe -o rm1.trigram.fst.log rm1.trigram.fst.csh
	rm rm1.trigram.fst.csh

#
# RM1 word pair test
#

RM1_WORDPAIR_CMD = $(JAVA_CMD) \
	-Dedu.cmu.sphinx.model.language.LanguageModelFactory.languageClass=edu.cmu.sphinx.model.language.SimpleNGramModel \
	-Dedu.cmu.sphinx.model.language.format=arpa \
	-Dedu.cmu.sphinx.model.language.location=RM.wordpair.arpa \
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Dictionary.dictionaryPath=dict/cmudict.0.6d+rm1.txt \
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-10\
        -Dedu.cmu.sphinx.search.WordListGrammar.path=./rm1.wordlist \
	edu.cmu.sphinx.decoder.BatchDecoder ./rm1.props ./rm1.batch

rm1_wordpair:
	$(RM1_WORDPAIR_CMD)

q_rm1_wordpair:
	echo "$(RM1_WORDPAIR_CMD)" > rm1.wordpair.csh
	chmod a+x rm1.wordpair.csh
	$(QSUB) -j oe -o rm1.wordpair.log rm1.wordpair.csh
	rm rm1.wordpair.csh

