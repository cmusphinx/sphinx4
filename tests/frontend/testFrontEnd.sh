#!/bin/sh

REF_FILE=$1
NUM_STAGES=$2

if [ -f $REF_FILE.out ]; then
        rm -f $REF_FILE.out
fi

if [ -f $REF_FILE.diff ]; then
        rm -f $REF_FILE.diff
fi

printf "Running $TEST_CLASS ...\n";

# do the test/comparison
${JAVA_HOME}/bin/java -cp ../../bld/classes \
    -Dedu.cmu.sphinx.frontend.FrontEndFactory.nStages=$NUM_STAGES \
    tests.frontend.FrontEndTest \
    $REF_FILE \
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
