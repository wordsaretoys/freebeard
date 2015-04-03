package com.wordsaretoys.freebeard.music;

import java.util.ArrayList;
import java.util.Random;

import com.wordsaretoys.freebeard.audio.Audio;
import com.wordsaretoys.freebeard.audio.Audio.AudioListener;
import com.wordsaretoys.freebeard.audio.Voice;

/**
 * maintains lookup tables and state
 */
public class Music {

	static String TAG = "Music";

	// prng
	Random rng;
	
	// list of available instruments
	ArrayList<Voice> voices = new ArrayList<Voice>();
	
	// list of available melody lines
	ArrayList<Score> scores = new ArrayList<Score>();
	
	// tempo for current song
	float tempo;
	
	// time per measure for current song
	float measureTime;
	
	// bitwise composer
	int composer;
	
	// composer endpoint
	int endpoint;
	
	/**
	 * ctor
	 */
	public Music() {

		for (int i = 0; i < 6; i++) {
			voices.add(new Voice());
			scores.add(new Score());
		}
		
		rng = new Random();
		
		Audio.INSTANCE.setListener(new AudioListener() {
			@Override
			public boolean onTimeElapsed(int index) {
				return run(index);
			}
		});
	}

	/**
	 * create a new score
	 */
	void create() {
		
		// pick a key
		int key = rng.nextInt(12);
		// pick a scale
		int[] scale = null;
		switch (rng.nextInt(4)) {
		case 0: scale = Scale.WesternMajor; break;
		case 1: scale = Scale.WesternMinor; break;
		case 2: scale = Scale.WesternMajorPentatonic; break;
		case 3: scale = Scale.WesternMinorPentatonic; break;
		}

		// pick a tempo
		tempo = 80 + 10 * rng.nextInt(6);
		// convert to seconds per measure
		measureTime = 60f * 4f / tempo;
		
		// reset scores
		for (Score score : scores) {
			score.generate(scale, key);
		}
		
		// regenerate the instrumental voices
		for (Voice voice : voices) {
			voice.generate();
		}
		
		// reset composer & endpoint
		composer = 1;
		endpoint = (int) Math.pow(2, scores.size());
	}
	
	/**
	 * run through a cycle
	 */
	protected synchronized boolean run(int index) {

		// actual time
		float time = index * measureTime /  16f;

		// first note? build that shit
		if (index == 0) {
			create();
		}

		// increment composer each measure
		if (index % 16 == 0) {
			composer += rng.nextInt(3);
		}

		// if we've reached the end point, no more notes
		if (composer > endpoint) {
			return false; 
		}
		
		// for each score
		for (int i = 0; i < scores.size(); i++) {
			// if we're composing with this score
			if ((composer & (1 << i)) != 0) {
				// select the score to draw from
				Score score = scores.get(i);
				int type = score.getTypeAt(index);
				// is there a note available?
				if (score.hasNote(index) && type > 0) {
					// select the voice to route the note to
					Voice voice = voices.get(i);
					// get decimal beat time
					float beats = measureTime * score.getBeatAt(index) / 16f;
					// get hold time (0 for staccato)
					float hold = beats * (type - 1);
					// note value
					float pitch = score.getNoteAt(index);
					// adjusted time
					Audio.INSTANCE.schedule(time, voice, pitch, hold); 
				}
			}
		}
		
		// update each score state
		for (int i = 0; i < scores.size(); i++) {
			scores.get(i).process(index);
		}

		return true;
	}
}
