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
	$(JAVA_CMD) tests.search.SimpleDictionaryTest simple_search.props

grammar: all
	$(JAVA_CMD) tests.search.SimpleLinguistTest simple_search.props

connected-grammar: all
	$(JAVA_CMD) tests.search.SimpleLinguistTest ../decoder/multiDecoder.props

search: all
	$(JAVA_CMD) tests.search.SimpleSearchTest simple_search.props
