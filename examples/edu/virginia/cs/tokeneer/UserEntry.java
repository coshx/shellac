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
	
	
	@Satisfies({"gotUserToken_to_quiescent"})
	// generates r1o2_dyn --> r1o2
	private void userTokenTorn() {
		// skipping audit logging
		display.setValue(Display.WELCOME);
		setStatus(StatusT.Quiescent);
		stats.addFailedEntry();
		userToken.clear();
	}
	
	@Satisfies({"gotUserToken_to_quiescent"})
	@ReqVar(value = "status", isInstance = true)
	private void setStatus(StatusT newStatus) {
		this.status = newStatus;
	}
	
	@Checks("r1o2_dyn")
	public void checkR1O2(@ReqVar(value="status_hist") StatusT[] oldStatus, 
			@ReqVar("status!") StatusT newStatus) {
		if (newStatus == StatusT.WaitingRemoveTokenSuccess) {
			// check oldStatus[0], oldStatus[1], ...
		}
	}
	
	/**
	 * This isn't currently checking r1o2 exactly, that WaitingRemoveTokenSuccess
	 * can only be reached through one or two chains. This is checking that the
	 * state always transitions towards waitingRemoveTokenSuccess, which isn't
	 * always the case.
	 * 
	 * TODO: add syntax to maintain a discrete history
	 * 
	 */
	@Checks("r1o2_dyn")
	public void checkR1O2(@ReqVar("status") StatusT oldStatus, 
				@ReqVar("status'") StatusT newStatus) {
		switch(oldStatus) {
		case GotUserToken:
			if (newStatus != StatusT.WaitingFinger &&
					newStatus != StatusT.WaitingEntry) {
				// throw new CheckFailedException("GotUserToken must transition to either WaitingFinger or WaitingEntry");
			}
			break;
		case WaitingFinger:
			if (newStatus != StatusT.GotFinger) {
				// throw new CheckFailedException("WaitingFinger must transition to GotFinger");
			}
			break;
		case GotFinger:
			if (newStatus != StatusT.WaitingUpdateToken) {
				// throw new CheckFailedException("GotFigner must transition to WaitingUpdateToken");
			}
			break;
		case WaitingUpdateToken:
			if (newStatus != StatusT.WaitingEntry) {
				// throw new CheckFailedException("WaitingUpdateToken must transition to WaitingEntry");
			}
			break;
		case WaitingEntry:
			if (newStatus != StatusT.WaitingRemoveTokenSuccess) {
				// throw new CheckFailedException("WaitingEntry must transition to WaitingRemoveTokenSuccess");
			}
			break;
		}
	}
	
	@Satisfies({"r1o2"})
	// generates r1o2 | r1o2_dyn --> r1o2
	private void validateUserToken() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			boolean authCertOk = userToken.readAndCheckAuthCert();

			if (authCertOk) {
				// skipping audit logging
				display.setValue(Display.WAIT);
				setStatus(StatusT.WaitingEntry);
			} else {
				boolean tokenOk = userToken.readAndCheck();
				if (tokenOk) {
					// skipping audit logging
					display.setValue(Display.INSERT_FINGER);
					setStatus(StatusT.WaitingFinger);
					fingerTimeout = System.currentTimeMillis() + configData.getFingerWaitDuration();
					
					// flush any stale BIO data
					bio.flush();
				} else {
					// skipping audit logging
					display.setValue(Display.REMOVE_TOKEN);
					setStatus(StatusT.WaitingRemoveTokenFail);
				}
			}

		}
		
		
	}
	
	@Satisfies({"r1o2"})
	// generates r1o2 | r1o2_dyn | null --> r1o2
	private void readFinger() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			if (System.currentTimeMillis() > this.fingerTimeout) {
				// skipping audit logging
				display.setValue(Display.REMOVE_TOKEN);
				setStatus(StatusT.WaitingRemoveTokenFail);
			} else {
				if (bio.isFingerPresent()) {
					// skipping audit logging
				
					display.setValue(Display.WAIT);
					setStatus(StatusT.GotFinger);
				} else {
					// null (the Ada implementation doesn't do anything here)
				}
			}
		}
	}
	
	@Satisfies({"r1o2"})
	// generates r1o2 | r1o2_dyn --> r1o2
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
				setStatus(StatusT.WaitingUpdateToken);
				stats.addSuccessfulBio();
			} else {
				display.setValue(Display.REMOVE_TOKEN);
				setStatus(StatusT.WaitingRemoveTokenFail);
				stats.addFailedBio();
			}
		}
	}
	
	@Satisfies({"r1o2"})
	// generates r1o2 | r1o2_dyn --> r1o2
	private void updateToken() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			boolean updateOk = userToken.addAuthCert();
			setStatus(StatusT.WaitingEntry);
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
	// generates r1o2 | r1o2_dyn --> r1o2
	private void validateEntry() {
		if (!userToken.isPresent()) {
			userTokenTorn();
		} else {
			if (configData.isInEntryPeriod()) {
				// skipping audit logging
				display.setValue(Display.OPEN_DOOR);
				setStatus(StatusT.WaitingRemoveTokenSuccess);
				this.tokenRemovalTimeout = System.currentTimeMillis() + configData.getTokenRemovalDuration();
			} else {
				// skipping audit logging
				display.setValue(Display.REMOVE_TOKEN);
				setStatus(StatusT.WaitingRemoveTokenFail);
			}
		}
	}
	
	@Satsifies({"r1o2"})
	// generates r1o2_dyn | null --> r1o2
	private void unlockDoor() {
		if (!userToken.isPresent()) {
			door.unlockDoor();
			userToken.clear();
			display.setValue(Display.DOOR_UNLOCKED);
			setStatus(StatusT.Quiescent);
			stats.addSuccessfulEntry();
		} else {
			if (System.currentTimeMillis() > this.tokenRemovalTimeout) {
				// skip audit logging
				display.setValue(Display.REMOVE_TOKEN);
				setStatus(StatusT.WaitingRemoveTokenFail);
			} else {
				// null
			}
		}
	}
	
	@Satisfies({"r1o2"})
	// generates r1o2_dyn -> r1o2
	private void failedAccessTokenRemoved() {
		// skipping audit logging
		display.setValue(Display.WELCOME);
		setStatus(StatusT.Quiescent);
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
	// generates r1o2 | null --> r1o2
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
	// generates r1o2_dyn --> r1o2
	public void startEntry() {
		display.setValue(Display.WAIT);
		setStatus(StatusT.GotUserToken);
	}
	

	public void checkGotUserTokenToQuiescent(StatusT )
	
}
