package edu.virginia.cs.shellac.examples;

import java.util.Arrays;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.ReqVar;
import edu.virginia.cs.shellac.annotations.Satisfies;

/**
 * This shows the use of dynamic checks, static proof obligations,
 * and in particular, our hybrid of the two.
 * 
 * sortedOrder is checked dynamically, but there is no check for permutation.
 * Instead, the only calls we make inside sort() are one call to copy,
 * and then zero or more calls to swap(). Since copy() and swap() are both
 * dynamically checked, we don't need to prove that they satisfy their requirements.
 * Because of this, the static analyzer is able to come up with the proof obligation
 * that a copy followed by zero or more swaps must imply permutation.
 * This static proof can be handled fairly easily, so we can mark that task
 * as completed when we have done the proof.
 * 
 * @author Ben Taitelbaum
 */
public class Sorter {
	
	@Satisfies({"sortedOrder", "permutation"})
	@ReqVar("output!")
	public int[] sort(@ReqVar("input?")int[] input) {
		@ReqVar("output!")int[] output = copy(input);
		
		// simple bubble sort
		for (int i = 0; i < output.length; i++) {
			for (int j = i; j < output.length; j++) {
				if (output[i] > output[j])
					swap(output, i, j);
			}
		}
		
		return output;
	}
	
	@Satisfies({"copy"})
	@ReqVar("output!")
	public int[] copy(@ReqVar("input?")int[] input) {
		int[] output = new int[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}
	
	@Checks("copy")
	public void checkCopy(@ReqVar("input?")int[] input, 
			@ReqVar("output!")int[] output) throws Throwable {
		if (input.length != output.length) {
			throw new Exception("lengths don't match (" + input.length + " != " + output.length + ")");
		} else {
			for (int i = 0; i < input.length; i++) {
				if (input[i] != output[i]) {
					throw new Exception("lists differ at position " + i + " ( " + input[i] + " != " + output[i] + " )");
				}
			}
		}
	}
	
	@Satisfies({"swap"})
	public void swap(@ReqVar(value="arr",isOutput=true)int[] arr, @ReqVar("pos1")int i, @ReqVar("pos2")int j) {
		int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}
	
	@Checks("swap")
	public void checkSwap(@ReqVar(value="arr")int[] arr_initial, 
						  @ReqVar("arr'")int[] arr_final, 
						  @ReqVar("pos1")int pos1, @ReqVar("pos2")int pos2) 
			throws Throwable {
		if (arr_initial.length != arr_final.length) {
			throw new Exception("lengths differ");
		}
		
		for (int i = 0; i < arr_initial.length; i++) {
			if (i != pos1 && i != pos2 && arr_initial[i] != arr_final[i]) {
				throw new Exception("arrays differ at poisition " + i);
			}
		}
		
		if (arr_initial[pos1] != arr_final[pos2] || arr_initial[pos2] != arr_final[pos1]) {
			throw new Exception("bad swap");
		}
	}
	
	@Checks("sortedOrder")
	public void checkSortedOrder(@ReqVar("output!")int[] output) throws Throwable {
		for (int i = 0; i < output.length - 1; i++) {
			if (output[i] > output[i+1]) {
				throw new Exception(Arrays.toString(output) + " not sorted! " + output[i] + " > " + output[i+1]);
			}
		}
	}
	
	public static void main(String[] args) {
		Sorter sorter = new Sorter();
		int[] output = sorter.sort(new int[] {9,8,7,6,5,4,3,2,1});
		System.out.println("Got: " + Arrays.toString(output));
	}
}
