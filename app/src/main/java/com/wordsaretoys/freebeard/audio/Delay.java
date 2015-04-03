package com.wordsaretoys.freebeard.audio;

import java.util.Arrays;


/**
 * implement circular delay buffer with multiple feedback paths
 */
public class Delay {

	static int Length = 65536;
	static int Modulus = Length - 1;
	static float Mixer = 0.95f;

	float[] buffer = new float[Length];
	int tap;
	
	/**
	 * process the next sample
	 */
	public void process(float[] sample) {
		for (int i = 0; i < sample.length; i++) {
			float ms = sample[i] * (1 - Mixer);
			
			int j = (tap + 64) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 128) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 256) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 512) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 1024) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 2048) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 4096) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 8192) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 16384) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;
			j = (tap + 32768) & Modulus;
			buffer[j] = Mixer * buffer[j] + ms;

			j = tap++ & Modulus;
			sample[i] += buffer[j];
		}
	}
	
	/**
	 * flush the buffer
	 */
	public void flush() {
		Arrays.fill(buffer, 0);
	}
	
}
