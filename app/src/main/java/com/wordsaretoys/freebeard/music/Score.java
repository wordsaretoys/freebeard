package com.wordsaretoys.freebeard.music;

import java.util.Random;

/**
 * generates a melody line
 */
public class Score {

	// pitches to play
	float[] pitch;
	
	// note function
	Sumer note;
	
	// note type function
	Sumer type;
	
	// note count function
	Sumer beat;
	
	// accumulated counts/measure
	int measure;
	
	// next note index
	int nextAt;
	
	// RNG
	Random rng;
	
	/**
	 * ctor, create fixed objects
	 */
	public Score() {
		rng = new Random();
	}
	
	/**
	 * generate new melody line
	 */
	public void generate(int[] scale, int key) {
		pitch = Scale.get(scale, key);
		int base = (int) Math.pow(2, 2 + rng.nextInt(3));
		note = new Sumer(base, pitch.length);
		type = new Sumer(base, 3);
		beat = new Sumer(base, 1 + rng.nextInt(4));
		nextAt = 0;
		measure = 0;
	}
	
	/**
	 * process score state
	 */
	public void process(int index) {
		// if a new note can be processed
		if (index >= nextAt) {
			// how many beats to count
			int beats = getBeatAt(index);
			int total = measure + beats;
	
			// if it would overrun the measure
			if (total > 16) {
				beats -= (total - 16);
				total = 0;
			}
			measure = total;
			
			// set up for next note
			nextAt = index + beats;
		}
	}

	public boolean hasNote(int index) {
		return index == nextAt;
	}
	
	public int getBeatAt(int index) {
		return (int) Math.pow(2, beat.get(index));
	}
	
	public int getTypeAt(int index) {
		return type.get(index);
	}
	
	public float getNoteAt(int index) {
		return pitch[note.get(index)];
	}
}
