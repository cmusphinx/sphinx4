#!/bin/sh

if [ -f preemphasis.out ]; then
        rm -f preemphasis.out
fi

if [ -f preemphasis.diff ]; then
        rm -f preemphasis.diff
fi


${JAVA_HOME}/bin/java -cp ../../classes tests.frontend.PreemphasizerTest 19990601-114739-00000001.raw > preemphasis.out

diff preemphasis.s3 preemphasis.out > preemphasis.diff

wc -l preemphasis.diff | awk '
{
  if ($1 == 0) {
    printf("%s differences in preemphasis.out. Test PASSED\n", $1);
  } else {
    printf("%s differences in preemphasis.out. Test FAILED\n", $1);
  }
}
'
