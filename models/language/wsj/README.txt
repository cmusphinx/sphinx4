---------------------------------------------
wsj language model
---------------------------------------------

A set of scripts and instructions for generating a 5000 word language model.


Outputs:
    wsj.lm - arpa format of the language model
    wsj.DMP - CMU binary format of the language model

Requires:
    CSR-I (WSJ0) Complete - from LDC
        http://wave.ldc.upenn.edu/Catalog/CatalogEntry.jsp?catalogId=LDC93S6A

    CMU language model toolkit:
        http://www.speech.cs.cmu.edu/SLM_info.html

    lm3g2dmp - utility to generate DMP format models:
       http://cmusphinx.sourceforge.net/webpage/html/download.php#utilities#

    unix commands: gawk mv rmdir rm tr

    All commmands should be in your path

---------------------
Detailed Instructions
---------------------
Steps to prepare the large WSJ lm:

1) Start with LDC WSJ transcripts disk

  % ls
  np_data  readme.txt  vp_data


2) uncompress the np_data and collect into a single transcript file:

   find /ldc/disk/np_data -name "*.z" | xargs zcat >> ./wsj.txt


3) generate the language models:

  % makeLM

   This will create two files:
        wsj.DMP - CMU binary format language model
        wsj.LM - ARPA format language model

---------------------
Files:
---------------------
5k_words.vocab  - custom vocabulary file
README.txt      - this document
makeLM          - script to build the language model
prep.awk        - script to prepare the transcript
