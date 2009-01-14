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
     * When dealing with in/out requirements variables, the checker will have
     * two variables available, the original value (same name as value()),  
     * and the modified value, whose name is value() with an apostrophe appended.
     * For example, if this ReqVar is named "myList" and isOutput() == true,
     * then the checker can use both "myList" and "myList'"
     * 
     * This is only relevant for methods with the Satisfies annotation
     */
    boolean isOutput() default false;
    
    /**
     * Setting this to anything other than 1 will allow the checker to use previous
     * values of this ReqVar. The previous values will be the initial values when calls
     * to the method where this was used (annotated with Satisfies). 
     * 
     * The checker will have available the variable with name equal to this value with "_hist"
     * appended, which will be an array of previous values.
     * For example, if this ReqVar is named "myValue", then the checker can refer to the history
     * as "myValue_hist". The first element of the array is the most recent value.
     * Note that the array's length will be less than the value of history() until that
     * many calls have been made.
     */
    int history() default 1;
}
