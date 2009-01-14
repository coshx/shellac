package edu.virginia.cs.tokeneer;

public enum StatusT {
	Quiescent,
	GotUserToken,
	WaitingFinger,
	GotFinger,
	WaitingUpdateToken,
	WaitingEntry,
	WaitingRemoveTokenSuccess,
	WaitingRemoveTokenFail
}
