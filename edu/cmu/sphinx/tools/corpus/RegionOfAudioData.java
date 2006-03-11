package edu.cmu.sphinx.tools.corpus;

import edu.cmu.sphinx.tools.audio.AudioData;

import java.util.Collection;

/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf
 * Date: Mar 3, 2006
 * Time: 9:48:04 PM
 */

/**
 * RegionOfAudioData is a base class of behavior common to all ojects that describe a region of audio data such as
 * a recording of a Word or an Utterance.  All RegionOfAudioData objects may be decorated with mulitple Notes that associate
 * user text with a region of time. Finally, all RegionOfAudioData objects may be marked at "excluded" which means that
 * refer to flawed data and should not be used in the testing and training.
 */
public class RegionOfAudioData {

    protected AudioDatabase audioDatabase;
    protected int beginTime;
    protected int endTime;
    protected Collection<Note> notes;
    protected boolean isExcluded;

    public boolean isExcluded() {
        return isExcluded;
    }

    public void setNotes(Collection<Note> notes) {
        this.notes = notes;
    }

    public void setExcluded(boolean excluded) {
        isExcluded = excluded;
    }

    public void addNote(Note note) {
        this.notes.add(note);
    }

    public Collection<Note> getNotes() {
        return notes;
    }

    public AudioDatabase getAudioDatabase() {
        return audioDatabase;
    }

    public void setAudioDatabase(AudioDatabase audioDatabase) {
        this.audioDatabase = audioDatabase;
    }

    public int getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(int beginTime) {
        this.beginTime = beginTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public AudioData getAudioData() {
        return new AudioData(audioDatabase.readPcmAsShorts(beginTime, endTime), audioDatabase.getSamplesPerSecond());
    }

    public double[] getPitchData() {
        return null;
    }

    public double[] getEnergyData() {
        return null;
    }
}
