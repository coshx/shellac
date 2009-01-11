package edu.virginia.cs.shellac.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Checks {
	/**
	 * Indicates which requirement a checking method is 
	 * responsible for checking.
	 * 
	 * @return the dynamic obligation to be checked by the annotated method
	 */
	String value();
}
