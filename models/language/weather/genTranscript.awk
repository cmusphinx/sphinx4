{ 
    gsub(/<s>/, "");
    gsub(/<\/s>/, "");
    sentence[$0] = $0;
}


END {
   for (i in sentence) {
       print i;
   }
}
