# An awk script that takes the hub4/eval99 data and
# generates a set of S4 batch files and raw audio
# to allow a supervised mode of testing
#
# Requires 'sox' the sound exchange program to extract the data
# Change 'directory' as appropriate for your system.
#
# Usage:
#       gawk -f unbundleHub4 *.stm
#
#

BEGIN {
    directory = "/lab/speech/sphinx4/data/hub4/eval99/supervised/";
    system("rm -rf " directory );
    system("mkdir -p " directory);
    audioCount = 0;
}


#
# Display info about stm file (for debugging purposes)
#
function displaySummary() {
    print "========================";
    print "Sphere   : "  sphereName;
    print "Audio    : " audioName;
    print "Speaker  : "  speaker;
    print "start    : "  startTime;
    print "end      : "  endTime;
    print "duration : " duration;
    print "class    : " speechClass;
    print "sox cmd  : " soxCommand;
    print "xcrpt    : " transcript;
}


#
# Output the batch file, generate the audio file
#
function outputCommands() {
    printf("%s %s\n", audioName, transcript) >> batchName;
    printf("%s %s\n", audioName, transcript) >> allBatch;
    system(soxCommand);
}

#
# Determines if the current utterance is worth keeping
#
function isGoodUtterance() {
    return speechClass != "";
}

#
# The main processing loop, extract the pertinent info
# and excute the commands
#
$1 != ";;"  {
    audioCount++;
    source = $1;
    sphereName = $1 ".sph";
    speaker = $3;
    startTime = $4;
    endTime = $5
    duration = endTime - startTime;
    info = $6;

    split(info, infoArray, ",");
    speechClass = infoArray[2];
    transcript = "";
    for (i = 7; i <= NF; i++) {
        # markup is in parans, so drop that.
        if (index($i, "(")  == 0) {
            transcript = transcript " " $i;
        }
    }
    transcript = tolower(transcript);


    audioName = directory source "." audioCount "." speechClass ".raw";
    batchName = directory speechClass "_hub4.batch"
    allBatch = directory "all_hub4.batch"

    soxCommand =  "sox " sphereName " -r 16000 -s -w " audioName  \
        " trim " startTime " " duration


    if (isGoodUtterance()) {
        displaySummary();
        outputCommands();
    }
}
