package com.wordsaretoys.freebeard.audio;

import java.util.Arrays;
import java.util.Random;

import android.util.SparseArray;

/**
 * represents a single instrument
 */
public class Voice {

	static int SinLen = 65536;
	static int SinMod = SinLen - 1;
	static float[] sine;
	
	static int SrcLen = 8192;
	static int SrcMod = SrcLen - 1;

	static int sampleRate;
	static float samplePeriod;
	
	// source buffers
	float[] source0;
	float[] source1;
	float[] source2;
	
	// octave multiplier
	float octave;
	
	// attack and release times
	float attack, release;
	
	// mixing time
	float mixing;
	
	// volume and channel pan
	float volume, channel;

	// RNG
	Random rng = new Random();

	// sparse array of samples
	SparseArray<short[]> samples;
	
	/**
	 * initialize static params
	 */
	public static void initialize(int s) {
		sampleRate = s;
		samplePeriod = 1f / (float)s;
	}
	
	/**
	 * ctor, creates wave table
	 */
	public Voice() {
		if (sine == null) {
			sine = new float[SinLen];
			for (int i = 0; i < SinLen; i++) {
				sine[i] = (float) Math.sin(2 * Math.PI * i / SinLen);
			}
		}
		
		samples = new SparseArray<short[]>();

		source0 = new float[SrcLen];
		source1 = new float[SrcLen];
		source2 = new float[SrcLen];
		generate();
	}

	public void generate() {
	
		generate(source0);
		generate(source1);
		generate(source2);
		
		for (int i = 0; i < SrcLen; i++) {
			source0[i] = (source0[i] + source2[i]) * 0.5f;
			source1[i] = (source1[i] + source2[i]) * 0.5f;
		}
		
		attack = (float)Math.pow(10, rng.nextInt(3) - 3);
		release = (float)Math.pow(2, rng.nextInt(10) - 8);
		
		mixing = (float)Math.pow(1.5, -rng.nextInt(7));
		
		switch(rng.nextInt(8)) {
		case 0: volume = 0.6f; channel = -0.75f; break;
		case 1: volume = 0.7f; channel = -0.5f; break;
		case 2: volume = 0.8f; channel = -0.25f; break;
		case 3: volume = 0.9f; channel = 0; break;
		case 4: volume = 0.8f; channel = 0.25f; break;
		case 5: volume = 0.7f; channel = 0.5f; break;
		case 6: volume = 0.6f; channel = 0.75f; break;
		}
		
		octave = (float) Math.pow(2, rng.nextInt(5) - 3);
		
		samples.clear();
		
	}

	/**
	 * get the sample for a given frequency/duration
	 */
	public short[] getSample(float freq, float hold) {
		int key = ((int)(hold * 16) << 16) + (int)(freq);
		short[] s = samples.get(key);
		if (s == null) {
			s = sample(freq, hold);
			samples.put(key, s);
		}
		return s;
	}
	
	/**
	 * fill a buffer with a random waveform
	 */
	private void generate(float[] buffer) {
		Arrays.fill(buffer, 0);
		float sum = 0;
		
		int limit = 8 + rng.nextInt(32);
		int count = 1 + rng.nextInt(4);
		
		// add main harmonics
		for (int i = 0; i < count; i++) {
			// allow even and odd harmonics (odd is half-buffer)
			float h = 1 + rng.nextInt(limit);
			float w = (0.5f * SinLen * h) / SrcLen;
			float a = 0.25f * (1 + rng.nextInt(4));
			sum += a;
			for (int j = 0; j < SrcLen; j++) {
				buffer[j] += a * sine[(int)(w * j) & SinMod];
			}
		}

		// normalize waveform
		float div = 1f / sum;
		for (int i = 0; i < SrcLen; i++) {
			buffer[i] *= div;
		}
		
	}
	
	/**
	 * generate a sample at a specified frequency/duration
	 */
	private short[] sample(float freq, float hold) {
		
		float waveRate = SrcLen * samplePeriod * freq * octave;
		float waveTime = 0;
		float waveAmpl = 0;
		
		float envAttack = samplePeriod / attack;
		float envSustain = (hold > 0) ? samplePeriod / hold : 1;
		float envRelease = samplePeriod / release;
		float envTime = 0;
		
		float mixRate = samplePeriod / mixing;
		float mixTime = 0;
		
		float left = (float) Math.pow((volume) * (1 - channel) * 0.5, 4);
		float right = (float) Math.pow((volume) * (1 + channel) * 0.5, 4);
		
		int stage = 0;
		int index = 0;
		
		// buffer size covers entire voice, two channel stereo
		int bufferSize = 2 * (int)(sampleRate * (attack + hold + release));
		short[] buffer = null;
		try {
			buffer = new short[bufferSize];
		} catch (OutOfMemoryError e) {
			// smash the cache and bug out
			samples.clear();
			return null;
		}
		
		int mag = Short.MAX_VALUE / 2;
		
		// while we're not at the end of the buffer
		while (index < bufferSize) {

			// mix waveform
			int itime = (int)(waveTime) & SrcMod;
			float s0 = source0[itime];
			float s1 = source1[itime];
			float w = waveAmpl * ((1 - mixTime) * s0 + mixTime * s1);
			waveTime += waveRate;
			mixTime += mixRate;
			if (mixTime > 1 || mixTime < 0) {
				mixRate = -mixRate;
			}
			
			// handle stage and envelope adjustments
			switch (stage) {
			
			case 0:
				waveAmpl += envAttack;
				if (waveAmpl >= 1) {
					stage = 1;
				}
				break;
				
			case 1:
				envTime += envSustain;
				if (envTime >= 1) {
					stage = 2;
				}
				break;
				
			case 2:
				waveAmpl -= envRelease;
				if (waveAmpl <= 0) {
					stage = 3;
				}
				break;
				
			}
			
			buffer[index++] = (short)(left * w * mag);
			buffer[index++] = (short)(right * w * mag);
		}
		
		return buffer;
	}
}
