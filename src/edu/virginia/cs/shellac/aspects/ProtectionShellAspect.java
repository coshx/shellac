/*
 * Created on Aug 3, 2007
 *
 */
package edu.virginia.cs.shellac.aspects;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.Satisfies;
import edu.virginia.cs.shellac.annotations.ReqVar;

// TODO: update this to use the KnowledgeBase
@Aspect
public class ProtectionShellAspect
{
    private static Logger log = Logger.getLogger(ProtectionShellAspect.class.getName());
    
    @Around("execution(* *(..)) && @annotation(satisfies)")
    public Object invokeProtectionShell(Satisfies satisfies, ProceedingJoinPoint pjp) {
        
        log.log(Level.INFO, "Invoking protection shells for: " + pjp.getSignature());
        
        // this maps the requirement variable names to their values
        // in the method being checked.
        //
        // It will be used to determine the correct arguments to the checker method
        Map<String, Object> reqVars = new HashMap<String, Object>();
        
        // get the requirements and the mappings for the checked method
        Method checkedMethod = getMethod(pjp);
        
        Annotation[][] paramAnnotations = checkedMethod.getParameterAnnotations();
        for (int param = 0; param < paramAnnotations.length; param++) {
            for (Annotation annot : paramAnnotations[param]) {
                if (annot instanceof ReqVar) {
                    ReqVar reqVar = (ReqVar) annot;
                    if (reqVar.isOutput()) {
                        // copy of input
                        reqVars.put(reqVar.value(), copy(pjp.getArgs()[param]));
                        
                        // output (which may change value during proceed)
                        reqVars.put(reqVar.value()+"'", pjp.getArgs()[param]);
                    } else {
                        reqVars.put(((ReqVar)annot).value(), pjp.getArgs()[param]);
                    }
                }
            }
        }
        
        // possibly copy the method's input if it is expected to change
        
        // invoke the method
        Object retVal = null;
        try {
            retVal = pjp.proceed(pjp.getArgs());
        } catch (Throwable t) {
            // couldn't even run the method, so fail
            log.log(Level.SEVERE, "Exception occurred while invoking " + pjp.getSignature(), t);
            System.exit(1);
        }

        // the returned object might correspond to a requirement variable
        ReqVar returnReqVar = checkedMethod.getAnnotation(ReqVar.class);
        if (returnReqVar != null) {
            reqVars.put(returnReqVar.value(), retVal);
        }
        

        
        // get all the protection shells
        // TODO: get all from the classpath in advance, not from the 
        // same class during this method
        for (String thm : satisfies.value()) {
            int numChecks = 0;
            for (Method checkerMethod : pjp.getSourceLocation().getWithinType().getMethods()) {
                Checks checks = checkerMethod.getAnnotation(Checks.class);
                if (checks != null && checks.value().equals(thm)) {
                    Annotation[][] paramAnnots = checkerMethod.getParameterAnnotations();
                    Object[] args = new Object[paramAnnots.length];
                    for (int i = 0; i < paramAnnots.length; i++) {
                        
                        // ensure we have exactly one ReqVar per parameter
                        boolean foundReqVar = false;
                        for (Annotation annot : paramAnnots[i]) {
                            if (annot instanceof ReqVar) {
                                if (foundReqVar) {
                                    log.log(Level.SEVERE, "Found multiple ReqVar annotations on parameter " + i + " for " + checkerMethod.toString());
                                    System.exit(1);
                                }

                                ReqVar reqVar = (ReqVar) annot;
                                if (!reqVars.containsKey(reqVar.value())) {
                                    log.log(Level.SEVERE, "Checker method needs requirement variable \"" + reqVar.value() + "\", but no such variable in checked method.");
                                    System.exit(1);
                                }
                                
                                args[i] = reqVars.get(reqVar.value());
                                foundReqVar = true;
                            }
                        }
                        
                        if (!foundReqVar) {
                            log.log(Level.SEVERE, "No ReqVar annotation on parameter " + i + " for " + checkerMethod.toString());
                            System.exit(1);
                        }
                    }
                    
                    try {
                        log.log(Level.INFO, "Invoking " + checkerMethod + " to check \"" + thm + "\"");
                        checkerMethod.invoke(pjp.getThis(), args);
                        numChecks++;
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Check failed!", t);
                        System.exit(0);
                    }
                }
            }
            
            if (numChecks == 0) {
                log.log(Level.INFO, "No checks available for \"" + thm + "\". Make sure to prove this statically.");
            }
            log.log(Level.INFO, "Ran " + numChecks + " checks for \"" + thm + "\"");
        }
        
        
        // return if everything is okay
        return retVal;
    }

    /**
     * Makes a deep copy of the given element
     * 
     * TODO: implement this for more than just int[]!
     */
    protected Object copy(Object value) {
        if (value instanceof int[]) {
        	return Arrays.copyOf((int[])value, ((int[])value).length); 
        } else {
            return null;
        }
        
    }
    
    
    /**
     * helper method to get the method that the proeceeding join point matches
     */
    protected Method getMethod(ProceedingJoinPoint pjp) {
        List<Method> possibleMethods = new ArrayList<Method>();
        
        Object[] args = pjp.getArgs();
        Class targetClass = pjp.getSourceLocation().getWithinType();
        
        for (Method m : targetClass.getMethods()) {
            if (m.getName().equals(pjp.getSignature().getName())) {
                Class<?>[] paramTypes = m.getParameterTypes();
                if (paramTypes.length != args.length)
                    continue;
                
                boolean paramsMatch = true;
                for (int i = 0; i < paramTypes.length && paramsMatch; i++) {
                    
                    // special case for primitive args, since aspectJ presents
                    // them all as objects
                    if (paramTypes[i].isPrimitive()) {
                        Class<?> argClass = args[i].getClass();
                        if (paramTypes[i] == Boolean.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Boolean.class);
                        } else if (paramTypes[i] == Character.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Character.class);
                        } else if (paramTypes[i] == Byte.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Byte.class);
                        } else if (paramTypes[i] == Short.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Short.class);
                        } else if (paramTypes[i] == Integer.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Integer.class);
                        } else if (paramTypes[i] == Long.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Long.class);
                        } else if (paramTypes[i] == Float.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Float.class);
                        } else if (paramTypes[i] == Double.TYPE) {
                            paramsMatch = argClass.isAssignableFrom(Double.class);                            
                        } else {
                            paramsMatch = false;
                        }
                    } else {
                        if (!args[i].getClass().equals(paramTypes[i]))
                            paramsMatch = false;
                    }
                }
                
                if (paramsMatch) {
                    possibleMethods.add(m);
                }
            }
        }
        
        if (possibleMethods.size() == 0) {
            log.log(Level.SEVERE, "Could not find method for: " + pjp.getSignature());
            System.exit(1);
        } else if (possibleMethods.size() > 1) {
            log.log(Level.SEVERE, "Found too many methods for: " + pjp.getSignature());
            System.exit(1);            
        }
        
        return possibleMethods.get(0);
    }
    
}
