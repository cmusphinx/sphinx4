#!/bin/sh
PUSH_DEST=/home/groups/c/cm/cmusphinx/htdocs/sphinx4/
export PUSH_DEST
cd bld
tar cvf zipcity.tar zipcity
sscp zipcity.tar  
sshh "cd $PUSH_DEST; tar xvf ~/zipcity.tar; rm ~/zipcity.tar;"
rm zipcity.tar
