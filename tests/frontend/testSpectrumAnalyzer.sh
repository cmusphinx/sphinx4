#!/bin/sh

if [ -f spectrum.out ]; then
        rm -f spectrum.out
fi

if [ -f spectrum.diff ]; then
        rm -f spectrum.diff
fi


${JAVA_HOME}/bin/java -cp ../../classes \
 -Dtests.frontend.SpectrumAnalyzerTest.dumpValues=true \
 tests.frontend.SpectrumAnalyzerTest 19990601-114739-00000001.raw > spectrum.out

diff spectrum.s3 spectrum.out > spectrum.diff

wc -l spectrum.diff | awk '
{
  if ($1 == 0) {
    printf("%s differences in spectrum.out. Test PASSED\n", $1);
  } else {
    printf("%s differences in spectrum.out. Test FAILED\n", $1);
  }
}
'
