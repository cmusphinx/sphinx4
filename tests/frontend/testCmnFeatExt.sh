#!/bin/sh


if [ -f features.out ]; then
        rm -f features.out
fi

if [ -f features.diff ]; then
        rm -f features.diff
fi

printf "Running CmnFeatureExtractorTest ...\n";

${JAVA_HOME}/bin/java -cp ../../classes \
 -Dtests.frontend.CmnFeatureExtractorTest.dumpValues=true \
 tests.frontend.CmnFeatureExtractorTest cepstra.ctl > features.out

diff features.s3 features.out > features.diff

wc -l features.diff | awk '
{
  if ($1 == 0) {
    printf("\t%s differences. Test PASSED\n", $1);
  } else {
    printf("\t%s differences. Test FAILED\n", $1);
  }
}
'
