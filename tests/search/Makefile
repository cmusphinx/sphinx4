# [[[copyright]]]

# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/search

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 


##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}

dict: all
	$(JAVA_CMD) tests.search.FullDictionaryTest simple_search.props

grammar: all
	$(JAVA_CMD) tests.search.SimpleLinguistTest simple_search.props

connected-grammar: all
	$(JAVA_CMD) tests.search.SimpleLinguistTest ../decoder/multiDecoder.props

search: all
	$(JAVA_CMD) tests.search.SimpleSearchTest simple_search.props

alpha-dict: all
	$(JAVA_CMD) tests.search.AlphaDictionaryTest alpha_dictionary.props

ArpaGrammarTestTiny: all
	$(JAVA_CMD) tests.search.ArpaGrammarTest ArpaGrammarTest.props

ArpaGrammarTestSmall: all
	$(JAVA_CMD)  \
-Dedu.cmu.sphinx.search.ArpaGrammar.path=/lab/speech/sphinx4/data/fst/digits.fst\
    tests.search.ArpaGrammarTest ArpaGrammarTest.props

#################################################################
# loads a large model. 
ArpaGrammarTestLarge: all
	$(JAVA_CMD)  \
-Dedu.cmu.sphinx.search.ArpaGrammar.path=/lab/speech/sphinx4/data/fst/5721_word_vocab.FST \
	tests.search.ArpaGrammarTest ArpaGrammarTest.props


#################################################################
# loads a huge model. Don't expect this to finish anytime soon
ArpaGrammarTestMega: all
	$(JAVA_CMD)  \
-Dedu.cmu.sphinx.search.ArpaGrammar.path=/lab/speech/sphinx4/data/fst/language_model.arpaformat.FST.ascii\
	tests.search.ArpaGrammarTest ArpaGrammarTest.props

