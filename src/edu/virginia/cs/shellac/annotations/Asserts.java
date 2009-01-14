package edu.virginia.cs.shellac.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to assert that a given method does indeed
 * satisfy the named requirements. This is useful if the requirements
 * have been proven.
 * 
 * TODO: This annotation is currently not used / read by the system.
 * 
 * @author btaitelb
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Asserts {
	String[] value();
}
