#!/bin/sh

REF_FILE=$1
TEST_CLASS=$2

if [ -f $REF_FILE.out ]; then
        rm -f $REF_FILE.out
fi

if [ -f $REF_FILE.diff ]; then
        rm -f $REF_FILE.diff
fi

printf "Running $TEST_CLASS ...\n";

# do the test/comparison
${JAVA_HOME}/bin/java -cp ../../classes \
 -Dtests.frontend.$TEST_CLASS.dumpValues=true \
 tests.frontend.$TEST_CLASS \
 $TEST_CLASS \
 frontend.props \
 19990601-114739-00000001.raw > $REF_FILE.out

diff $REF_FILE.s4 $REF_FILE.out > $REF_FILE.diff


# print out the results
wc -l $REF_FILE.diff | awk '
{
  if ($1 == 0) {
    printf("\t%s differences. Test PASSED\n", $1);
  } else {
    printf("\t%s differences. Test FAILED\n", $1);
  }
}
'
