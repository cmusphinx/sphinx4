{
   gsub(/:/, " ");
   gctime += $(NF-1);
   totalTime = $1;
}


END {
    print "GC time: ", gctime, " Total Time ", totalTime, \
        " Percent ", gctime/totalTime * 100;
}
