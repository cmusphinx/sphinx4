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

preemphasizer: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh preemphasis PreemphasizerTest

hammingWindow: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh hammingWindow HammingWindowerTest

spectrumAnalyzer: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh spectrum SpectrumAnalyzerTest

melspectrum: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh melspectrum MelFilterbankTest

melcepstrum: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh melcepstrum MelCepstrumTest

cmn: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh cmn CMNTest

featureExtractor: all
	@chmod +x testFrontEnd.sh
	sh ./testFrontEnd.sh features FeatureExtractorTest

alltests: all
	(cd $(TOP); $(MAKE))
	@$(MAKE) -s preemphasizer
	@$(MAKE) -s hammingWindow
	@$(MAKE) -s spectrumAnalyzer
	@$(MAKE) -s melspectrum
	@$(MAKE) -s melcepstrum
	@$(MAKE) -s cmn
	@$(MAKE) -s featureExtractor

live: all
	(cd $(TOP); $(MAKE))
	$(JAVA_CMD) -cp ../../classes \
	tests.frontend.MicrophoneTest

# Endpointing for the tidigits data set, current configured such that
# you can only see the speech regions. If you want to see the non-speech
# regions as well, set the system property "removeNonSpeech" to "false".
ep-tidigits: all
	$(JAVA_CMD) \
	-Dtests.frontend.energyEndpointer.batchMode=true \
	-DremoveNonSpeech=false \
	tests.frontend.EnergyEndpointerTest \
	energyEndpointer ../decoder/tiDigits.props ../decoder/tidigits.batch

ep-tidigits-quick: all
	$(JAVA_CMD) \
	-Dtests.frontend.energyEndpointer.batchMode=true \
	-DremoveNonSpeech=false \
	tests.frontend.EnergyEndpointerTest \
	energyEndpointer ../decoder/tiDigits.props ../decoder/tidigits.batch

ep-one: all
	$(JAVA_CMD) \
	-Dtests.frontend.energyEndpointer.batchMode=true \
	-Dtests.frontend.energyEndpointer.dumpTimes=true \
	-DremoveNonSpeech=false \
	tests.frontend.EnergyEndpointerTest \
	energyEndpointer ../decoder/tiDigits.props ./one.batch

clean::
	rm -f *.out *.diff

