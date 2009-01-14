package edu.virginia.cs.tokeneer;

public class Stats {
	private static Stats instance = null;
	private Stats() {
	}
	
	public static Stats instance() {
		if (instance == null) {
			instance = new Stats();
		}
		
		return instance;
	}
	
	private int failedEntries = 0;
	private int successfulBios = 0;
	private int failedBios = 0;
	private int successfulEntries = 0;
	
	
	public void addFailedEntry() {
		failedEntries++;
	}
	
	public void addSuccessfulBio() {
		successfulBios++;
	}

	public void addFailedBio() {
		failedBios++;
	}

	public void addSuccessfulEntry() {
		successfulEntries++;
	}

	public int getFailedEntries() {
		return failedEntries;
	}

	public int getSuccessfulBios() {
		return successfulBios;
	}

	public int getSuccessfulEntries() {
		return successfulEntries;
	}


}
