package edu.virginia.cs.shellac.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method parameters that indicates their
 * mapping to a requirement variable.
 * 
 * When used on a method, this indicates the mapping for
 * the return value of the method.
 * 
 * If the same variable is used both for input and output, then
 * the isOutput property should be set to true. In this case,
 * the value will be appended with a single quote to indicate the
 * output form.
 */
@Target({ElementType.METHOD,ElementType.PARAMETER,ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReqVar {
    String value();
    
    /**
     * signifies that this variable will be used for output as well as 
     * input. In other words, it may be modified during the call.
     * 
     * This is only relevant for methods with the Satisfies annotation
     */
    boolean isOutput() default false;
}
