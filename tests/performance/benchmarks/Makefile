# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.



HTML_FILE=benchmarks.html
FILE=results.txt
DEST=/lab/speech/web/sphinx4/$(HTML_FILE)

UPDATE_DEST=/home/groups/c/cm/cmusphinx/htdocs/


results:
	showResults any any any $(FILE) > $(DEST)

old_results:
	showResults argus sphinx4 ti46 $(FILE) > $(DEST)
	echo "<br>" >> $(DEST)
	showResults mangueira sphinx4 ti46 $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults sunlabs sphinx4 ti46 $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults sunlabs sphinx4 ti46 $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults pharynx sphinx4 ti46 $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults boteco sphinx4 ti46 $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults mute sphinx4 ti46 $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults argus sphinx4 tidigits $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults mangueira sphinx4 tidigits $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults sunlabs sphinx4 tidigits $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults pharynx sphinx4 tidigits $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults boteco sphinx4 tidigits $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults mute sphinx4 tidigits $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults argus sphinx4 tidigits_quick $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults mangueira sphinx4 tidigits_quick $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults sunlabs sphinx4 tidigits_quick $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults pharynx sphinx4 tidigits_quick $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults boteco sphinx4 tidigits_quick $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults mute sphinx4 tidigits_quick $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)
	showResults any sphinx3 any $(FILE) >> $(DEST)
	echo "<br>" >> $(DEST)

update:
	$(MAKE) results
	sscp $(DEST) 
	sshh  cp $(HTML_FILE)  $(UPDATE_DEST)
