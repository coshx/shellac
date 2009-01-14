package edu.virginia.cs.tokeneer.test;

import junit.framework.TestCase;
import edu.virginia.cs.tokeneer.StatusT;
import edu.virginia.cs.tokeneer.UserEntry;
import edu.virginia.cs.tokeneer.UserToken;

public class UserEntryTest extends TestCase {
	private UserEntry userEntry;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		UserToken.instance().clear();
		userEntry = new UserEntry();
	}
	public void testIsInProgress() {
		assertFalse(userEntry.isInProgress());

		// progress through one step
		userEntry.startEntry();
		
		assertTrue(userEntry.isInProgress());
	}
	public void testCanStart() {
		assertFalse(userEntry.canStart());
		UserToken.instance().setPresent(true);
		assertTrue(userEntry.canStart());
	}
	public void testProgress() {
		assertEquals(StatusT.Quiescent, userEntry.getStatus());
		
		// calling progress when in this state should have no effect
		userEntry.progress();
		assertEquals(StatusT.Quiescent, userEntry.getStatus());

		// start the transaction
		UserToken.instance().setPresent(true);
		userEntry.startEntry();
		assertEquals(StatusT.GotUserToken, userEntry.getStatus());
		
		// TODO: go through all the steps!
//		userEntry.progress();
//		assertEquals(StatusT.WaitingEntry, userEntry.getStatus());
//		
//		int successfulEntries = Stats.instance().getSuccessfulEntries();
//		userEntry.progress();
//		assertEquals(StatusT.Quiescent, userEntry.getStatus());
//		assertFalse(Door.instance().isLocked());
//		assertEquals(successfulEntries+1, Stats.instance().getSuccessfulEntries());
	}
}
