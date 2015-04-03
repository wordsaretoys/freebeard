package com.wordsaretoys.freebeard.audio;

import java.util.ArrayList;
import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.wordsaretoys.freebeard.utils.Needle;

/**
 * audio synthesizer engine
 */
public enum Audio {

	INSTANCE;
	
	static String TAG = "AudioEngine";
	
	static float ShortToFloat = 2f / Short.MAX_VALUE; 

	/**
	 * synth object
	 */
	class Synth {
		// reference to voice sample buffer
		public short[] voice;
		// stage (0 == ready, 1 == sampling, 2 == idle)
		public int stage;
		// absolute frame position
		public int frame;
		// index into staging buffer
		public int bufferIndex;
		// index into voice sample buffer
		public int sampleIndex;
	}
	
	/**
	 * listener for audio events
	 */
	public interface AudioListener {
		public boolean onTimeElapsed(int index);
	}

	// audio pump thread helper
	Needle audioPump;
	
	// client event listener object
	private AudioListener listener;

	// audio track object
	private AudioTrack track;
	
	// length of buffer in shorts
	private int bufferLength;
	
	// length of buffer in frames
	private int frameCount;
	
	// sampling rate in Hz
	private int sampleRate;
	
	// synthesizer pool
	private ArrayList<Synth> synths;

	// absolute frame position
	private int frame;
	
	// absolute time index
	private int index;
	
	// audio staging buffer
	private float[] stager;
	
	// audio hardware buffer
	private short[] buffer;
	
	// delay line
	private Delay delay = new Delay();
	
	// last queued frame
	private int queued;
	
	// external volume control
	private float volume;
	
	/**
	 * set audio listener callback object 
	 */
	public void setListener(AudioListener l) {
		listener = l;
	}

	/**
	 * start playback at time zero
	 */
	public void play() {
		audioPump.resume();
		track.play();
	}
	
	/**
	 * reset time and frame position
	 */
	public synchronized void reset() {
		index = 0;
		frame = 0;
		queued = 0;
		synths.clear();
	}
	
	/**
	 * pause playback
	 */
	public void pause() {
		audioPump.pause();
		track.pause();
	}
	
	/**
	 * terminate audio engine
	 */
	public void close() {
		audioPump.stop();
	}

	/**
	 * flush any active samples
	 */
	public void flush() {
		track.flush();
		delay.flush();
		reset();
	}
	
	/**
	 * return true if audio engine is running
	 */
	public boolean isPlaying() {
		return audioPump.isRunning();
	}
	
	/**
	 * return true if the audio engine has been terminated
	 */
	public boolean isTerminated() {
		return audioPump.isStopped();
	}

	/**
	 * increase volume one log step
	 */
	public void louden() {
		volume = Math.min(10f, volume * 1.25f);
	}
	
	/**
	 * decrease volume one log step
	 */
	public void soften() {
		volume = Math.max(0.01f, volume * 0.75f);
	}

	/**
	 * schedule a note to be played 
	 */
	public synchronized void schedule(float start, Voice voice, float freq, float hold) {

		short[] sample = voice.getSample(freq, hold);
		if (sample == null) {
			return;
		}
		
		// look for an inactive synth object in the pool
		Synth synth = null;
		for (int i = 0, il = synths.size(); i < il; i++) {
			Synth s = synths.get(i);
			if (s.stage == 2) {
				synth = s;
				break;
			}
		}
		
		// couldn't find one, create it & add to pool
		if (synth == null) {
			synth = new Synth();
			synths.add(synth);
		}
		
		// initialize synth parameters
		synth.voice = sample;
		synth.stage = 0;
		synth.frame = queued = (int)(start * sampleRate) + sampleRate;
	}
	
	/**
	 * ctor; called on first reference to singleton
	 */
	private Audio() {
		audioPump = new Needle("audio pump", 1) {
			@Override
			public void run() {
				init();
				while (inPump()) {
					loop();
				}
				term();
			}
		};
		audioPump.start();
		volume = 1;
	}

	/**
	 * startup and initialization  
	 */
	private void init() {
		audioPump.setPriority(Thread.MAX_PRIORITY);
		
		synths = new ArrayList<Synth>();
		
		sampleRate = AudioTrack.getNativeOutputSampleRate(
				AudioManager.STREAM_MUSIC);

		bufferLength = AudioTrack.getMinBufferSize(
				sampleRate, 
				AudioFormat.CHANNEL_OUT_STEREO, 
				AudioFormat.ENCODING_PCM_16BIT) & 0xfffe;

		frameCount = bufferLength / 2;
		buffer = new short[bufferLength];
		stager = new float[bufferLength];
		Voice.initialize(sampleRate);
		
		track = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				sampleRate,
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferLength * 2, // length in bytes
				AudioTrack.MODE_STREAM);
		
		if (track.getState() != AudioTrack.STATE_INITIALIZED) {
			throw new RuntimeException("Couldn't initialize AudioTrack object");
		}
		
		track.setStereoVolume(1, 1);
	}
	
	/**
	 * main audio pump loop
	 */
	private void loop() {
		synchronized (INSTANCE) {
			stage();
		}
		track.write(buffer, 0, buffer.length);
	}

	/**
	 * terminate and cleanup
	 */
	private void term() {
		track.stop();
		track.release();
	}
	
	/**
	 * stage all active voices to audio buffer
	 */
	private void stage() {
		Arrays.fill(stager, 0);

		// get the next batch of synths
		boolean more = true;
		while (more && ((queued - frame) < frameCount * 4)) {
			more = listener.onTimeElapsed(index++);
		}
		
		// process the list
		int idle = 0;
		for (int i = synths.size() - 1; i >= 0; i--) {
			Synth s = synths.get(i);

			// if the synth is ready
			if (s.stage == 0) {
				// is the sample queued up?
				if (s.frame >= frame && s.frame < (frame + frameCount)) {
					// initiate sampling
					s.stage = 1;
					s.bufferIndex = (s.frame - frame) * 2;
					s.sampleIndex = 0;
				}
			}
			
			// if the synth is sampling
			if (s.stage == 1) {
				// add generated sample to staging buffer
				while (s.bufferIndex < bufferLength && s.sampleIndex < s.voice.length) {
					stager[s.bufferIndex++] += ShortToFloat * s.voice[s.sampleIndex++];
				}
				// reset buffer index
				s.bufferIndex = 0;
				// check if we're done
				if (s.sampleIndex >= s.voice.length) {
					s.stage = 2;
				}
			}
			
			// count idle synths
			if (s.stage == 2) {
				idle++;
			}
			
		}
		
		// add fake reverb
		delay.process(stager);
		
		// headroom mix from staging buffer to audio buffer
		for (int i = 0, il = buffer.length; i < il; i++) {
			float b = volume * stager[i];
			if (b <= -1.25f)
			{
			    b = -0.987654f;
			}
			else if (b >= 1.25f)
			{
			    b = 0.987654f;
			}
			else
			{
			    b = 1.1f * b - 0.2f * b * b * b;
			}
			buffer[i] = (short)(32767 * b);
		}

		// if nothing's running, start over
		if (!more && idle == synths.size()) {
			reset();
		} else {
			// otherwise, advance frame counter
			frame += frameCount;
		}
	}
}
