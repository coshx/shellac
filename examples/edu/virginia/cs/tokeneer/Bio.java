package edu.virginia.cs.tokeneer;

public class Bio {
	private static Bio instance = null;
	private Bio() {	
	}
	
	public static Bio instance() {
		if (instance == null) {
			instance = new Bio();
		}
		
		return instance;
	}
	
	private boolean fingerPresent = false;
	
	public boolean isFingerPresent() {
		return fingerPresent;
	}
	public void setFingerPresent(boolean fingerPresent) {
		this.fingerPresent = fingerPresent;
	}
	
	public void flush() {
		this.fingerPresent = false;
	}
}
