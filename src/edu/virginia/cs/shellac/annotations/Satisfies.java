package edu.virginia.cs.shellac.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: Add ElementType.TYPE here and deal with whole classes being annotated
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Satisfies {
	
	/**
	 * Indicates which requirements must be satisfied by the annotated method.
	 * @return The names of requirements that must be satisfied by the annotated method.
	 */
	String[] value();
}
