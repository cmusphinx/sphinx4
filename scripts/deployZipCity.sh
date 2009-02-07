#!/bin/sh
# Define the destination
PUSH_DEST=/home/groups/c/cm/cmusphinx/htdocs/sphinx4/src/apps/edu/cmu/sphinx/demo/
export PUSH_DEST
# Add the scripts directory to the path, just in case
PATH=`pwd`/scripts:$PATH
export PATH
ant -f demo.xml release_zipcity
pushd bld
srsync $PUSH_DEST/ zipcity
popd

