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
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh preemphasis PreemphasizerTest

hammingWindow: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh hammingWindow HammingWindowerTest

spectrumAnalyzer: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh spectrum SpectrumAnalyzerTest

alltests: all
	(cd $(TOP); $(MAKE))
	@$(MAKE) -s preemphasizer
	@$(MAKE) -s hammingWindow
	@$(MAKE) -s spectrumAnalyzer
	@$(MAKE) -s cmnFeatExt

clean::
	rm -f *.out *.diff

