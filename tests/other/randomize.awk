

# reads standard in and outputs the file line-by-line in
# random order

{ lines[count++] = $0 }

END {
   for (i = 0; i < count; i++) {
       idx = int(count * rand());
       tmp = lines[i];
       lines[i] = lines[idx];
       lines[idx] = tmp;
   }
   for (i = 0; i < count; i++) {
       print lines[i];
   }
}
