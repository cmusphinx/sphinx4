/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

#include <stdio.h>
#include <sys/time.h>

#define MAX_ELEMENTS 39

double d1Values[MAX_ELEMENTS];
double d2Values[MAX_ELEMENTS];
double d3Values[MAX_ELEMENTS];

float f1Values[MAX_ELEMENTS];
float f2Values[MAX_ELEMENTS];
float f3Values[MAX_ELEMENTS];

int i1Values[MAX_ELEMENTS];
int i2Values[MAX_ELEMENTS];
int i3Values[MAX_ELEMENTS];

double score;
int maxIterations =  1000000;

double doDoubleScore() {
    int i;
    double dval = 0;
    for (i = 0; i < MAX_ELEMENTS; i++) {
	double diff = d1Values[i] - d2Values[i];
	dval -= diff * diff * d3Values[i];
    }
    return dval * d1Values[0];
}

int doIntScore() {
    int i;
    int dval = 0;
    for (i = 0; i < MAX_ELEMENTS; i++) {
	int diff = i1Values[i] - i2Values[i];
	dval -= diff * diff * i3Values[i];
    }
    return dval * i1Values[0];
}

float doFloatScore() {
    int i;
    float dval = 0;
    for (i = 0; i < MAX_ELEMENTS; i++) {
	float diff = f1Values[i] - f2Values[i];
	dval -= diff * diff * f3Values[i];
    }
    return dval * f1Values[0];
}

void doScores() {
    int i;

    struct timeval tv;
    double time_start, time_end;
    gettimeofday(&tv,NULL);
    time_start = (double)(tv.tv_sec)+(((double)tv.tv_usec)/1000000.0);

    for (i = 0; i < maxIterations; i++) {
	score = doDoubleScore();
    }

    gettimeofday(&tv,NULL);
    time_end = ((double)(tv.tv_sec))+((double)tv.tv_usec/1000000.0);
    printf("double time %f\n", time_end - time_start);

    gettimeofday(&tv,NULL);
    time_start = (double)(tv.tv_sec)+(((double)tv.tv_usec)/1000000.0);

    for (i = 0; i < maxIterations; i++) {
	score = doIntScore();
    }

    gettimeofday(&tv,NULL);
    time_end = ((double)(tv.tv_sec))+((double)tv.tv_usec/1000000.0);
    printf("int time %f\n", time_end - time_start);

    gettimeofday(&tv,NULL);
    time_start = (double)(tv.tv_sec)+(((double)tv.tv_usec)/1000000.0);

    for (i = 0; i < maxIterations; i++) {
	score = doFloatScore();
    }

    gettimeofday(&tv,NULL);
    time_end = ((double)(tv.tv_sec))+((double)tv.tv_usec/1000000.0);
    printf("float time %f\n", time_end - time_start);
}


int main(int argc, char **argv) {
    int i;

    for (i = 0; i < 10; i++) {
	doScores();
    }
    return 0;
}
