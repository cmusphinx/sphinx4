#!/bin/sh

. ${HOME}/.profile

JAVA_HOME=/lab/speech/java/j2sdk1.4.2
export JAVA_HOME

PUSH_DEST=/home/groups/c/cm/cmusphinx/htdocs/sphinx4
export PUSH_DEST

DOC_DEST=javadoc
export DOC_DEST

JAVADOC_OUT=javadoc.out

LIST=cmusphinx-commits@lists.sourceforge.net

cd /tmp
cvs -d:ext:cvs.sourceforge.net:/cvsroot/cmusphinx co sphinx4
cd sphinx4
ant javadoc
gtar cf /tmp/sphinx4docs.tar -C ${DOC_DEST} .
${HOME}/bin/sscp /tmp/sphinx4docs.tar
${HOME}/bin/sshh tar xf sphinx4docs.tar -C ${PUSH_DEST}
rm /tmp/sphinx4docs.tar
ant -emacs -Daccess=private clean javadoc > ${JAVADOC_OUT}
if grep -w -i warning ${JAVADOC_OUT}; then mailx -s "Javadoc result" ${LIST} < ${JAVADOC_OUT};
elif grep -i error ${JAVADOC_OUT}; then mailx -s "Javadoc result" ${LIST} < ${JAVADOC_OUT};
fi

cd /tmp
rm -rf sphinx4

