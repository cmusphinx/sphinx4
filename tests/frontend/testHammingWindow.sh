#!/bin/sh

if [ -f hammingWindow.out ]; then
        rm -f hammingWindow.out
fi

if [ -f hammingWindow.diff ]; then
        rm -f hammingWindow.diff
fi


${JAVA_HOME}/bin/java -cp ../../classes \
 -Dtests.frontend.HammingWindowerTest.dumpValues=true \
 tests.frontend.HammingWindowerTest 19990601-114739-00000001.raw > hammingWindow.out

diff hammingWindow.s3 hammingWindow.out > hammingWindow.diff

wc -l hammingWindow.diff | awk '
{
  if ($1 == 0) {
    printf("%s differences in hammingWindow.out. Test PASSED\n", $1);
  } else {
    printf("%s differences in hammingWindow.out. Test FAILED\n", $1);
  }
}
'
