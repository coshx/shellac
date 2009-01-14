package edu.virginia.cs.tokeneer;

public class ConfigData {
	private static ConfigData instance = null;
	private ConfigData() {
		
	}
	
	public static ConfigData instance() {
		if (instance == null) {
			instance = new ConfigData();
		}
		
		return instance;
	}
	
	
	private long fingerWaitDuration;
	private long tokenRemovalDuration;
	
	public long getFingerWaitDuration() {
		return fingerWaitDuration;
	}

	public void setFingerWaitDuration(long fingerWaitDuration) {
		this.fingerWaitDuration = fingerWaitDuration;
	}
	
	public boolean isInEntryPeriod() {
		// TODO: implement!
		return true;
	}

	public long getTokenRemovalDuration() {
		return tokenRemovalDuration;
	}

	public void setTokenRemovalDuration(long tokenRemovalDuration) {
		this.tokenRemovalDuration = tokenRemovalDuration;
	}
}
