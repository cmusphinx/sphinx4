# [[[copyright]]]

# Relative path to the "base" of the source tree
TOP = .

# List any sub directories that need to be built.  Start with generic
# packages going toward specialized.  That is, if one package depends
# on another, put the dependent package later in the list.
SUBDIRS = edu tests mrproject

GTAR=/pkg/gnu/bin/tar

##########################################################################

PUSH_DEST = /home/groups/c/cm/cmusphinx/htdocs/sphinx4
include ${TOP}/build/Makefile.config

# Any extra actions to perform when cleaning
clean::
	rm -rf $(CLASS_DEST_DIR)


# a quick and dirty implementation

javadocs:
	$(JAVADOC) -d $(DOC_DEST) -quiet -subpackages edu -source 1.4


# another quick and dirty implementation, with private methods/fields

javadocs_private:
	$(JAVADOC) -d $(DOC_DEST) -private -quiet -subpackages edu -source 1.4


push_javadocs:
	$(MAKE) javadocs
	$(GTAR) cfC /tmp/sphinx4docs.tar $(DOC_DEST) .
	sscp /tmp/sphinx4docs.tar
	sshh tar xfC sphinx4docs.tar $(PUSH_DEST)
	rm /tmp/sphinx4docs.tar

stest:
	$(GTAR) cfC /tmp/sphinx4docs.tar $(DOC_DEST) .
	$(GTAR) xfC /tmp/sphinx4docs.tar /home/plamere/stest
