#
#
# An [gn]awk script that allows a comparison between two runs of S4
# This script will annotate the output of S4 with the corresponding
# WER from a given previously collected S4 output.  This is useful
# when tuning the recognizer.
#
# Typical usage:
#
# make an4_words_trigram > results.out &
# tail -f results.out | gawk -f compareResults.awk previousResults.out
#
#
BEGIN {
        other = ARGV[1];
        ARGV[1] = "";
        print ARGV[1];
}

$1 != "Words:" { print $0; }

$1 == "Words:" {
   words = $2;
   while (getline oRes < other > 0) {
       split(oRes, arr);
       if (arr[1] == "Words:" && arr[2] == words) {
           otherWER = arr[6];
           break;
       }
   }
   printf("%s  Previous WER: %s\n", $0, otherWER);
}
