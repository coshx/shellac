package edu.virginia.cs.tokeneer;

public class UserToken {
	private static UserToken instance = null;
	private UserToken() {}
	
	public static UserToken instance() {
		if (instance == null) {
			instance = new UserToken();
		}
		
		return instance;
	}
	
	private boolean present = false;
	
	public boolean isPresent() {
		return present;
	}
	public void setPresent(boolean present) {
		this.present = present;
	}
	
	public void clear() {
		present = false;
	}
	
	public boolean readAndCheckAuthCert() {
		// TODO: Implement!
		return false;
	}
	
	public boolean readAndCheck() {
		// TODO: Implement!
		return false;
	}

	public boolean addAuthCert() {
		// TODO: Implement!
		return false;
	}
	
	public void updateAuthCert() {
		// TODO: Implement!
	}
}
