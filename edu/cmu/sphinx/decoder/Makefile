# [[[copyright]]]
# Relative path to the "base" of the source tree
TOP = ../../../..
PACKAGE_PATH = edu/cmu/sphinx/decoder

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built.  Start with generic
# packages going toward specialized.  That is, if one package depends
# on another, put the dependent package later in the list.
SUBDIRS = 

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}
