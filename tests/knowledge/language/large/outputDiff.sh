#!/bin/sh

#
# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.
#

#
# If the given diff file has more than 0 lines, print out that
# the number of lines and that the test has failed. Otherwise,
# print that the test has passed.
#


wc $1 | gawk -v filename=$1 '
{
  if ($1 == 0) {
    printf("%s differences in %s. Test PASSED\n", $1, filename);
  } else {
    printf("%s differences in %s. Test FAILED\n", $1, filename);
  }
}
'  
