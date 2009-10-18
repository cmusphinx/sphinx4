#!/bin/sh

. ${HOME}/.profile

PUSH_DEST=htdocs/sphinx4

JAVADOC_OUT=javadoc.out

LIST=cmusphinx-commits@lists.sourceforge.net

cd /tmp
${HOME}/bin/ssvn sphinx4
cd sphinx4
COPY_LIST=`pwd`/scripts/doclist.txt
ant javadoc
srsync $PUSH_DEST javadoc
srsync $PUSH_DEST `cat $COPY_LIST` `find src/apps -name '*html'`
ant -emacs -Daccess=private clean javadoc > ${JAVADOC_OUT}
if grep -w -i warning ${JAVADOC_OUT}; then mailx -s "Javadoc result" ${LIST} < ${JAVADOC_OUT};
elif grep -i error ${JAVADOC_OUT}; then mailx -s "Javadoc result" ${LIST} < ${JAVADOC_OUT};
fi

cd /tmp
rm -rf sphinx4

