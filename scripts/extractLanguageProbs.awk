# An awk/gawk script that extracts the language probabilities from an
# S4 results output file.  Output is 3 columns:
# 1 - frame number
# 2 - total lang probability applied
# 3 - this frames lang prob
# 4 - this frames insertion prob
$1 >= 1 && $1 < 100000 { sum += $4;  sum += $5; }
$1 >= 1 && $1 < 100000 { print $1, sum, $4, $5 }
