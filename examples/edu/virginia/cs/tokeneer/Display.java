package edu.virginia.cs.tokeneer;

public class Display {
	public static final String WELCOME = "Welcome to Tokeneer";
	public static final String WAIT = "Please Wait";
	public static final String INSERT_FINGER = "Please Insert Finger";
	public static final String REMOVE_TOKEN = "Please Remove Token";
	public static final String TOKEN_UPDATE_FAILED = "Updating Token Failed";
	public static final String OPEN_DOOR = "Open the Door";
	public static final String DOOR_UNLOCKED = "The door is unlocked";
	
	private static Display instance = null;
	private Display() {
		
	}
	
	public static Display instance() {
		if (instance == null) {
			instance = new Display();
		}
		
		return instance;
	}
	
	private String msg = "";
	private String doorUnlockedMsg = DOOR_UNLOCKED;
	
	public void setValue(String msg) {
		if (msg.equals(DOOR_UNLOCKED)) {
			this.msg = doorUnlockedMsg;
		} else {
			this.msg = msg;
		}
	}
	public String getValue() {
		return msg;
	}
	
	public void changeDoorUnlockedMsg(String msg) {
		doorUnlockedMsg = msg;
	}
}