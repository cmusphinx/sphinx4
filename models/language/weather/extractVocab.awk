
$1 == "WORD"  { vocab[$2] = $2; }
NF == 1  { vocab[$1] = $1; }
NF > 1 && $1 == "<s>" {

    ok = 1;
    for (i = 1; i <= NF; i++) {
        if (!($i in vocab)) {
            # print " ok: " $1
            ok = 0;
        } 
    }

    if (ok) {
        print $0;
    }
}
