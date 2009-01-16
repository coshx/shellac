package edu.virginia.cs.shellac.examples;

import java.util.Arrays;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.ReqVar;
import edu.virginia.cs.shellac.annotations.Satisfies;

public class HistoryCheck {
	
	/**
	 * Let's assume we have some requirement that says that the number cannot
	 * be more than 5 of what it was 4 calls ago.
	 * @param num
	 */
	@Satisfies("processA")
	public void processNext(@ReqVar(value="num", history=5)int num) {
		return;
	}
	
	@Checks("process")
	public void checkProcess(@ReqVar("num[]") int[] num) throws Exception {
		System.out.println("history is " + Arrays.toString(num));
		if (num.length < 5)
			return;
		else {
			if (num[0] - num[4] > 5) {
				throw new Exception(num[0] + " - " + num[4] + " > 5");
			} else if (num[4] - num[0] > 5) {
				throw new Exception(num[4] + " - " + num[0] + " > 5");				
			}
		}
	}
	
	@Satisfies({"compute"})
	@ReqVar(value = "output!", history = 5)
	public int computeNext(int a) {
		return a;
	}
	
	/**
	 * if the output has gone between postive and negative 3 or more times
	 * in the last 5, then something is wrong...
	 */
	@Checks("compute")
	public void checkCompute(@ReqVar("output![]") int[] values) throws Exception {
		System.out.println("history is " + Arrays.toString(values));
		int switches = 0;
		
		int lastVal = 0;
		for (int i = 0; i < values.length; i++) {
			int val = values[i];
			if (val * lastVal < 0)
				switches++;
			if (val != 0)
				lastVal = val;
		}
		
		if (switches >= 3) {
			throw new Exception("switched between negative and postive " + switches + " times!");
		}
	}
	
	public static void testProcess() {
		HistoryCheck hc = new HistoryCheck();
		
		// these should all pass
		for (int i = 0; i < 10; i++) {
			hc.processNext(i);
		}
		
		// history should now be
		// 9, 8, 7, 6, 5
		
		
		// this is okay, because the history will be
		// 2, 9, 8, 7, 6
		hc.processNext(2);
		
		System.out.println("Got Here, which is good.");
		
		// still okay
		// 2, 2, 9, 8, 7
		hc.processNext(2);
		
		System.out.println("Got Here, which is good.");
		
		// this should fail
		hc.processNext(2);
		
		System.out.println("FAILURE -- should not have reached here!");		
	}
	
	public static void testCompute() {
		HistoryCheck hc = new HistoryCheck();
		hc.computeNext(1);
		hc.computeNext(2);
		hc.computeNext(3);
		hc.computeNext(-1);
		hc.computeNext(-2);
		hc.computeNext(-3);
		hc.computeNext(0);
		hc.computeNext(-1);
		hc.computeNext(0);
		hc.computeNext(-1);
		hc.computeNext(0);
		hc.computeNext(1);
		hc.computeNext(-1);
		System.out.println("I expect to fail now");
		hc.computeNext(1); // here's where it hits 3 so it should fail
		System.out.println("Should not get this far");
	}
	
	
	public static void main(String[] args) {
		//testProcess();
		testCompute();
	}
}
