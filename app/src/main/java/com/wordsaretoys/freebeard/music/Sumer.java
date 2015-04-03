package com.wordsaretoys.freebeard.music;

public class Sumer {

	float[] source;
	int range, index, modu;
	
	/**
	 * create source array
	 * note that srclen MUST be a power of 2!
	 */
	public Sumer(int srclen, int range) {
		source = new float[srclen];
		for (int i = 0; i < srclen; i++) {
			source[i] = (float) Math.random();
		}
		this.range = range;
		modu = srclen - 1;
	}
	
	/**
	 * return next value
	 */
	public int next() {
		float p = source[index & modu] + 
				source[(index >> 1) & modu] + 
				source[(index >> 2) & modu] + 
				source[(index >> 3) & modu];
		index++;
		return (int) Math.floor(range * p) % range; 
	}
	
	/**
	 * return value at specified position
	 */
	public int get(int i) {
		float p = source[i & modu] + 
				source[(i >> 1) & modu] + 
				source[(i >> 2) & modu] + 
				source[(i >> 3) & modu];
//		float p = source[i & modu] + source[(i >> 1) & modu];
		return (int) Math.floor(range * p) % range; 
	}

}
