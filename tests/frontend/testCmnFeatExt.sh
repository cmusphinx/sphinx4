#!/bin/sh


if [ -f features.out ]; then
        rm -f features.out
fi

if [ -f features.diff ]; then
        rm -f features.diff
fi

${JAVA_HOME}/bin/java -cp ../../classes \
 -Dtests.frontend.CmnFeatureExtractorTest.dumpValues=true \
 tests.frontend.CmnFeatureExtractorTest cepstra.ctl > features.out

diff features.s3 features.out > features.diff

wc -l features.diff | awk '
{
  if ($1 == 0) {
    printf("%s differences in features.out. Test PASSED\n", $1);
  } else {
    printf("%s differences in features.out. Test FAILED\n", $1);
  }
}
'
