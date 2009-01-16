package edu.virginia.cs.shellac.examples;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.Satisfies;

public class IfStatements {
	
	@Satisfies({"a"})
	public void doA() {}
	
	@Checks("a")
	public void checkA() {}

	@Satisfies({"b"})
	public void doB() {}
	
	@Checks("b")
	public void checkB() {}

	@Satisfies({"c"})
	public void doC() {}
	
	@Checks("c")
	public void checkC() {}
	
	@Satisfies({"d"})
	public char doD() {return 'd';}

	@Checks("d")
	public void checkD() {}
	
	@Satisfies({"run"})
	public void run(char runChar) {
		if (runChar == 'a')
			doA();
		else if (runChar == 'b')
			doB();
		else
			doC();
	}
	
	@Satisfies({"testing_expr"})
	public void doSomething() {
		if (doD()) {
			// no-op
		}
	}
}
