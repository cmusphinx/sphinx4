# [[[copyright]]]

# Relative path to the top of the source tree
TOP = ../../../..
PACKAGE_PATH = edu/cmu/sphinx/util

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS = 

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS} ${DATA}
