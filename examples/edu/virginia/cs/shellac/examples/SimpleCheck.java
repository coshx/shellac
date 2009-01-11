package edu.virginia.cs.shellac.examples;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.ReqVar;
import edu.virginia.cs.shellac.annotations.Satisfies;

public class SimpleCheck {
	
	@Satisfies({"adds_one"})
	@ReqVar("output!")
	public int addOne(@ReqVar("input?")int a) {
		return a + 1;
	}
	
	@Satisfies({"adds_one"})
	@ReqVar("output!")
	public int badAddOne(@ReqVar("input?")int b) {
		return b - 1;
	}
	
	@Checks("adds_one")
	public void checkAddsOne(@ReqVar("input?") int input,
							 @ReqVar("output!") int output) throws Throwable {
		if (output != input + 1) {
			throw new Exception(input + " + 1 != " + output);
		}
	}
	
	public static void main(String[] args) {
		new SimpleCheck().addOne(1);
		new SimpleCheck().badAddOne(2);
	}
}
