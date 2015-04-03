package com.wordsaretoys.freebeard.music;



/**
 * maintains enums and lookup tables for chords et al
 */
public class Scale {

	static int[] WesternMajor = {
		2, 2, 1, 2, 2, 2, 1
	};
	
	static int[] WesternMinor = {
		2, 1, 2, 2, 1, 2, 2
	};

	static int[] WesternMajorPentatonic = {
		2, 2, 3, 2, 3
	};

	static int[] WesternMinorPentatonic = {
		3, 2, 2, 3, 2
	};

	public static float[] get(int[] scale, int key) {
		// interval sum gives us number of tones
		int sum = 0;
		for (int i = 0; i< scale.length; i++) {
			sum += scale[i];
		}
		
		// generate scale root
		float root = (float) Math.pow(2f, 1f / sum);
		// allocate space for two octaves
		float[] t = new float[scale.length * 2];
		// for each pitch in the table
		for (int i = 0, deg = 0; i < t.length; i++) {
			// pitch number: offset by key, mod two octaves
			int n = (key + deg) % (2 * sum);
			// pitch: offset by 9/12 of interval sum 
			// to get "A-equivalent" pitch in this scale
			t[i] = (float)(440f * Math.pow(root, n - 0.75f * sum));
			// sum the next interval to get scale degree
			deg += scale[i % scale.length];
		}
		return t;
	}
	
}
