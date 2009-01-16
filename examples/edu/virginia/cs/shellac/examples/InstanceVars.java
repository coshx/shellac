package edu.virginia.cs.shellac.examples;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.ReqVar;
import edu.virginia.cs.shellac.annotations.Satisfies;

public class InstanceVars {
	public int value = 1;
	
	@Satisfies({"valueUpdatedOkay"})
	@ReqVar(value="value", isInstance = true)
	public void setValue(int val) {
		this.value = val;
	}
	
	@Checks("valueUpdatedOkay")
	public void checkValueUpdated(@ReqVar("value")int oldVal, @ReqVar("value'")int newVal) throws Exception {
		if (oldVal + 1 != newVal) {
			throw new Exception("bad value. " + oldVal + " + 1 != " + newVal);
		}
	}
	
	public int getValue() {
		return this.value;
	}
	
	// TODO: add a check with instance variable and history
	
	public static void main(String[] args) throws Exception {
		InstanceVars iv = new InstanceVars();
		
		// these should be okay
		iv.setValue(2);
		iv.setValue(3);
		iv.setValue(4);
		iv.setValue(5);

		System.out.println("should be here");

		// this should fail
		iv.setValue(0);

		System.out.println("should NOT be here!");
	}
}
