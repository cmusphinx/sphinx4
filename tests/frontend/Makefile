# [[[copyright]]]

# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/frontend

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 


##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}

cmnFeatExt: all
	@chmod +x testCmnFeatExt.sh
	sh ./testCmnFeatExt.sh

preemphasizer: all
	@chmod +x testPreemphasizer.sh
	sh ./testPreemphasizer.sh

hammingWindow: all
	@chmod +x testHammingWindow.sh
	sh ./testHammingWindow.sh

alltests: all
	(cd $(TOP); $(MAKE))
	@$(MAKE) preemphasizer
	@$(MAKE) hammingWindow
	@$(MAKE) cmnFeatExt

clean::
	rm -f *.out *.diff

