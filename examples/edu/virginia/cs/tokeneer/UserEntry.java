package edu.virginia.cs.tokeneer;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.ReqVar;
import edu.virginia.cs.shellac.annotations.Satisfies;

public class UserEntry {
	private StatusT status = StatusT.Quiescent;
	private long fingerTimeout = 0;
	private long tokenRemovalTimeout = 0;
	private UserToken userToken = UserToken.instance();
	private Display display = Display.instance();
	private ConfigData configData = ConfigData.instance();
	private Bio bio = Bio.instance();
	private Stats stats = Stats.instance();
	private Door door = Door.instance();
	private Latch latch = Latch.instance();
	
	private boolean hasUserDeparted() {
		return status != StatusT.Quiescent && !userToken.isPresent();
	}
	
	
	@Satisfies({"r1o2"})
	@ReqVar(value="status", isInstance=true)
	private void userTokenTorn() {
		// skipping audit logging
		display.setValue(Display.WELCOME);
		quiescent();
		stats.addFailedEntry();
		userToken.clear();
	}
	
	
	@Satisfies({"r1o2"})
	@ReqVar(value="status", isInstance=true)
	private void validateUserToken() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			boolean authCertOk = userToken.readAndCheckAuthCert();

			if (authCertOk) {
				// skipping audit logging
				display.setValue(Display.WAIT);
				waitingEntry();
			} else {
				boolean tokenOk = userToken.readAndCheck();
				if (tokenOk) {
					// skipping audit logging
					display.setValue(Display.INSERT_FINGER);
					waitingFinger();
					fingerTimeout = System.currentTimeMillis() + configData.getFingerWaitDuration();
					
					// flush any stale BIO data
					bio.flush();
				} else {
					// skipping audit logging
					display.setValue(Display.REMOVE_TOKEN);
					waitingRemoveTokenFail();
				}
			}

		}
		
		
	}
	
	@Satisfies({"r1o2"})
	private void readFinger() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			if (System.currentTimeMillis() > this.fingerTimeout) {
				// skipping audit logging
				display.setValue(Display.REMOVE_TOKEN);
				waitingRemoveTokenFail();
			} else {
				if (bio.isFingerPresent()) {
					// skipping audit logging
				
					display.setValue(Display.WAIT);
					gotFinger();
				} else {
					// null (the Ada implementation doesn't do anything here)
				}
			}
		}
	}
	
	@Satisfies({"r1o2"})
	private void validateFinger() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			// verify the bio data
			// for now, just assume it's valid
			
			// TODO: call the Bio methods and check the match
			boolean isMatch = true;
			
			bio.flush();
			
			if (isMatch) {
				display.setValue(Display.WAIT);
				waitingUpdateToken();
				stats.addSuccessfulBio();
			} else {
				display.setValue(Display.REMOVE_TOKEN);
				waitingRemoveTokenFail();
				stats.addFailedBio();
			}
		}
	}
	
	@Satisfies({"r1o2"})
	private void updateToken() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			boolean updateOk = userToken.addAuthCert();
			waitingEntry();
			if (updateOk) {
				userToken.updateAuthCert();
				// skipping audit logging
				display.setValue(Display.WAIT);
				//certificateStore.updateStore();
			} else {
				// skipping audit logging
				display.setValue(Display.TOKEN_UPDATE_FAILED);
			}
		}
	}
	
	@Satisfies({"r1o2"})
	private void validateEntry() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			if (configData.isInEntryPeriod()) {
				// skipping audit logging
				display.setValue(Display.OPEN_DOOR);
				waitingRemoveTokenSuccess();
				this.tokenRemovalTimeout = System.currentTimeMillis() + configData.getTokenRemovalDuration();
			} else {
				// skipping audit logging
				display.setValue(Display.REMOVE_TOKEN);
				waitingRemoveTokenFail();
			}
		}
	}
	
	@Satsifies({"r1o2"})
	private void unlockDoor() {
		if (!userToken.isPresent()) {
			door.unlockDoor();
			userToken.clear();
			display.setValue(Display.DOOR_UNLOCKED);
			quiescent();
			stats.addSuccessfulEntry();
		} else {
			if (System.currentTimeMillis() > this.tokenRemovalTimeout) {
				// skip audit logging
				display.setValue(Display.REMOVE_TOKEN);
				waitingRemoveTokenFail();
			} else {
				// null
			}
		}
	}
	
	@Satisfies({"r1o2"})
	private void failedAccessTokenRemoved() {
		// skipping audit logging
		display.setValue(Display.WELCOME);
		quiescent();
		stats.addFailedEntry();
		userToken.clear();
	}
	
	public StatusT getStatus() {
		return status;
	}
	

	public boolean isInProgress() {
		return 
			status == StatusT.GotUserToken || 
			status == StatusT.WaitingFinger ||
			status == StatusT.GotFinger ||
			status == StatusT.WaitingUpdateToken ||
			status == StatusT.WaitingEntry ||
			status == StatusT.WaitingRemoveTokenSuccess;
	}
	
	public boolean isCurrentActivityPossible() {
		return isInProgress() || hasUserDeparted();
	}
	
	public boolean canStart() {
		return status == StatusT.Quiescent && userToken.isPresent();
	}
	
	public void displayPollUpdate() {
		String msg;
		
		if (latch.isLocked()) {
			msg = Display.REMOVE_TOKEN;
		} else {
			msg = Display.WELCOME;
		}
		
		display.changeDoorUnlockedMsg(msg);
	}
	
	@Satisfies({"r1o2"})
	public void progress() {
		StatusT localStatus = status;
		
		switch (localStatus) {
		case GotUserToken:
			validateUserToken();
			break;
		case WaitingFinger:
			readFinger();
			break;
		case GotFinger:
			validateFinger();
			break;
		case WaitingUpdateToken:
			updateToken();
			break;
		case WaitingEntry:
			validateEntry();
			break;
		case WaitingRemoveTokenSuccess:
			unlockDoor();
			break;
		case WaitingRemoveTokenFail:
			failedAccessTokenRemoved();
			break;
		}
	}
	
	@Satisfies({"r1o2"})
	public void startEntry() {
		display.setValue(Display.WAIT);
		gotUserToken();
	}
	

	// TODO: annotate somehow that this is the only place we're allowed to change status
	private void setStatus(StatusT newStatus) {
		this.status = newStatus;
	}
	

	/*
	 * State transition functions
	 */
	
	@Satisfies({"quiescent_to_gotUserToken"})
	@ReqVar(value="status", isInstance=true)
	private void gotUserToken() {
		setStatus(StatusT.GotUserToken);
	}

	@Satisfies({"gotUserToken_to_waitingFinger", 
				"waitingFinger_to_waitingFinger"})
	@ReqVar(value="status", isInstance=true)
	private void waitingFinger() {
		setStatus(StatusT.WaitingFinger);
	}

	@Satisfies({"waitingFinger_to_gotFinger"})
	@ReqVar(value="status", isInstance=true)
	private void gotFinger() {
		setStatus(StatusT.GotFinger);
	}

	@Satisfies({"gotFinger_to_waitingUpdateToken"})
	@ReqVar(value="status", isInstance=true)
	private void waitingUpdateToken() {
		setStatus(StatusT.WaitingUpdateToken);
	}

	@Satisfies({"gotUserToken_to_waitingEntry", 
				"waitingUpdateToken_to_waitingEntry"})
	@ReqVar(value="status", isInstance=true)
	private void waitingEntry() {
		setStatus(StatusT.WaitingEntry);
	}

	@Satisfies({"gotUserToken_to_waitingRemoveTokenFail",
				"waitingFinger_to_waitingRemoveTokenFail",
				"gotFinger_to_waitingRemoveTokenFail",
				"waitingEntry_to_waitingRemoveTokenFail",
				"waitingRemoveTokenSuccess_to_waitingRemoveTokenFail"})
	@ReqVar(value="status", isInstance=true)
	private void waitingRemoveTokenFail() {
		setStatus(StatusT.WaitingRemoveTokenFail);
	}

	@Satisfies({"waitingEntry_to_waitingRemoveTokenSuccess",
				"waitingRemoveTokenSuccess_to_waitingRemoveTokenSuccess"})
	@ReqVar(value="status", isInstance=true)
	private void waitingRemoveTokenSuccess() {
		setStatus(StatusT.WaitingRemoveTokenSuccess);
	}

	@Satisfies({"gotUserToken_to_quiescent",
				"waitingFinger_to_quiescent",
				"gotFinger_to_quiescent",
				"waitingUpdateToken_to_quiescent",
				"waitingEntry_to_quiescent",
				"waitingRemoveTokenSuccess_to_quiescent",
				"waitingRemoveTokenFail_to_quiescent"})
	@ReqVar(value="status", isInstance=true)
	private void quiescent() {
		setStatus(StatusT.Quiescent);
	}


	/*
	 *
	 * Checks for status updates.
	 * 
	 */
	
	
	@Checks("quiescent_to_gotUserToken")
	public void checkQuiescentToGotUserToken(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.Quiescent && newStatus != StatusT.GotUserToken)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}
	@Checks("gotUserToken_to_quiescent")
	public void checkGotUserTokenToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotUserToken && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("gotUserToken_to_waitingFinger")
	public void checkGotUserTokenToWaitingFinger(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotUserToken && newStatus != StatusT.WaitingFinger)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("gotUserToken_to_waitingEntry")
	public void checkGotUserTokenToWaitingEntry(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotUserToken && newStatus != StatusT.WaitingEntry)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("gotUserToken_to_waitingRemoveTokenFail")
	public void checkGotUserTokenToWaitingRemoveTokenFail(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotUserToken && newStatus != StatusT.WaitingRemoveTokenFail)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingFinger_to_waitingFinger")
	public void checkWaitingFingerToWaitingFinger(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingFinger && newStatus != StatusT.WaitingFinger)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}
	
	@Checks("waitingFinger_to_quiescent")
	public void checkWaitingFingerToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingFinger && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingFinger_to_gotFinger")
	public void checkWaitingFingerToGotFinger(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingFinger && newStatus != StatusT.GotFinger)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingFinger_to_waitingRemoveTokenFail")
	public void checkWaitingFingerToWaitingRemoveTokenFail(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingFinger && newStatus != StatusT.WaitingRemoveTokenFail)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}
	
	@Checks("gotFinger_to_quiescent")
	public void checkGotFingerToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotFinger && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}
	
	@Checks("gotFinger_to_waitingUpdateToken")
	public void checkGotFingerToWaitingUpdateToken(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotFinger && newStatus != StatusT.WaitingUpdateToken)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("gotFinger_to_waitingRemoveTokenFail")
	public void checkGotFingerToWaitingRemoveTokenFail(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.GotFinger && newStatus != StatusT.WaitingRemoveTokenFail)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingUpdateToken_to_quiescent")
	public void checkWaitingUpdateTokenToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingUpdateToken && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}
	
	@Checks("waitingUpdateToken_to_waitingEntry")
	public void checkWaitingUpdateTokenTokenToWaitingEntry(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingUpdateToken && newStatus != StatusT.WaitingEntry)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingEntry_to_quiescent")
	public void checkWaitingEntryToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingEntry && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}
	
	@Checks("waitingEntry_to_waitingRemoveTokenSuccess")
	public void checkWaitingEntryToWaitingRemoveTokenSuccess(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingEntry && newStatus != StatusT.WaitingRemoveTokenSuccess)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingEntry_to_waitingRemoveTokenFail")
	public void checkWaitingEntryToWaitingRemoveTokenFail(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingEntry && newStatus != StatusT.WaitingRemoveTokenFail)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingRemoveTokenSuccess_to_waitingRemoveTokenSuccess")
	public void checkWaitingRemoveTokenSuccessToWaitingRemoveTokenSuccess(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingRemoveTokenSuccess && newStatus != StatusT.WaitingRemoveTokenSuccess)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingRemoveTokenSuccess_to_quiescent")
	public void checkWaitingRemoveTokenSuccessToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingRemoveTokenSuccess && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingRemoveTokenSuccess_to_waitingRemoveTokenFail")
	public void checkWaitingRemoveTokenSuccessToWaitingRemoveTokenFail(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingRemoveTokenSuccess && newStatus != StatusT.WaitingRemoveTokenFail)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingRemoveTokenFail_to_waitingRemoveTokenFail")
	public void checkWaitingRemoveTokenFailToWaitingRemoveTokenFail(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingRemoveTokenFail && newStatus != StatusT.WaitingRemoveTokenFail)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}

	@Checks("waitingRemoveTokenFail_to_quiescent")
	public void checkWaitingRemoveTokenFailToQuiescent(@ReqVar("status")StatusT oldStatus, 
			@ReqVar("status'")StatusT newStatus) throws Exception {
		if (oldStatus == StatusT.WaitingRemoveTokenFail && newStatus != StatusT.Quiescent)
			throw new Exception(oldStatus.toString() + " -> " + newStatus.toString() 
					+ " is not a valid transition.");
	}


}
