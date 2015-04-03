package com.wordsaretoys.freebeard.utils;

/**
 * guides a thread (hyuck hyuck)
 * provides safe suspend/resume semantics
 */

public abstract class Needle implements Runnable {

	enum ThreadState { RUN, PAUSE, STOP }
	protected Thread thread = new Thread(this);
	ThreadState state = ThreadState.PAUSE;
	long timeout;
	
	public Needle(String name, long timeout) {
		thread.setName(name);
		this.timeout = timeout;
	}
	
	public void start() {
		thread.start();
	}
	
	public void setName(String name) {
		thread.setName(name);
	}
	
	public boolean isRunning() {
		return state == ThreadState.RUN;
	}
	
	public boolean isPaused() {
		return state == ThreadState.PAUSE;
	}
	
	public boolean isStopped() {
		return state == ThreadState.STOP;
	}
	
	public boolean isLooping() {
		return state != ThreadState.STOP;
	}

	public synchronized void resume() {
		state = ThreadState.RUN;
		notify();
	}
	
	public synchronized void pause() {
		state = ThreadState.PAUSE;
	}
	
	public synchronized void stop() {
		state = ThreadState.STOP;
		notify();
	}
	
	protected boolean block(long time) {
		try {
			synchronized(this) {
				// timed wait
				wait(time);
				// thread suspension check/wait
				while(state == ThreadState.PAUSE) {
					wait();
				}
			}
		} catch(InterruptedException e) {
			return true;
		}
		return false;
	}
	
	protected boolean inPump() {
		block(timeout);
		return isLooping();
	}
	
	public void setPriority(int priority) {
		thread.setPriority(priority);
	}
	
	public abstract void run();
}
