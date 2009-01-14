package edu.virginia.cs.tokeneer;

// TODO: unlocking the door just means unlocking the latch (with timeouts and alarms)
public class Door {
	private static Door instance = null;
	private Door() {}
	
	public static Door instance() {
		if (instance == null) {
			instance = new Door();
		}
		
		return instance;
	}
	
	private boolean locked = true;
	public void unlockDoor() {
		locked = false;
	}
	public void lockDoor() {
		locked = true;
	}
	public boolean isLocked() {
		return locked;
	}
}
