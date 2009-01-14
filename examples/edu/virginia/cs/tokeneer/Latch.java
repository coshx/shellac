package edu.virginia.cs.tokeneer;

// TODO: add latch timeout and alarms for door!
public class Latch {
	private static Latch instance = null;
	private Latch() {}
	
	public static Latch instance() {
		if (instance == null) {
			instance = new Latch();
		}
		
		return instance;
	}
	
	private boolean locked = true;
	public void lockLatch() {
		locked = true;
	}
	public void unlockLatch() {
		locked = false;
	}
	public boolean isLocked() {
		return locked;
	}
}
