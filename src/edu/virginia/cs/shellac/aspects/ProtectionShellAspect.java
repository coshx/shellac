/*
 * Created on Aug 3, 2007
 *
 */
package edu.virginia.cs.shellac.aspects;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import edu.virginia.cs.shellac.annotations.Checks;
import edu.virginia.cs.shellac.annotations.ReqVar;
import edu.virginia.cs.shellac.annotations.Satisfies;

// TODO: update this to use the KnowledgeBase
@Aspect
public class ProtectionShellAspect
{
    private static Logger log = Logger.getLogger(ProtectionShellAspect.class.getName());
    private static Map<String, Object> reqVarHistories = new HashMap<String, Object>();
    
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
        
        // look for any isInstance ReqVars and copy their current values
        ReqVar instanceReqVar = checkedMethod.getAnnotation(ReqVar.class);
        if (instanceReqVar != null && instanceReqVar.isInstance()) {
        	Object instanceVal = getInstanceValueCopy(pjp, instanceReqVar);
            reqVars.put(instanceReqVar.value(), instanceVal);
            if (instanceReqVar.history() > 0) {
            	boolean isPrimitive = false;
            	try {
            		isPrimitive = pjp.getThis().getClass().getField(instanceReqVar.value()).getType().isPrimitive();
            	} catch (NoSuchFieldException ex) {
        			log.log(Level.SEVERE, "Cannot find instance field with name " + instanceReqVar.value() + " in "
        					+ pjp.getThis().getClass(), ex);
        			System.exit(1);            		
            	}
            	Object hist = updateHistory(instanceReqVar, instanceVal, pjp, isPrimitive);
            	reqVars.put(instanceReqVar.value() + "[]", hist);
            }
        }

        // store, and possibly copy, annotated parameters
        Annotation[][] paramAnnotations = checkedMethod.getParameterAnnotations();
        for (int param = 0; param < paramAnnotations.length; param++) {
            for (Annotation annot : paramAnnotations[param]) {
                if (annot instanceof ReqVar) {
                    ReqVar reqVar = (ReqVar) annot;
                    if (reqVar.isOutput()) {
                        // copy of input
                        reqVars.put(reqVar.value(), copy(pjp.getArgs()[param], checkedMethod.getParameterTypes()[param].isPrimitive()));
                        
                        // output (which may change value during proceed)
                        reqVars.put(reqVar.value()+"'", pjp.getArgs()[param]);
                    } else {
                        reqVars.put(reqVar.value(), pjp.getArgs()[param]);
                    }
                    
                    if (reqVar.history() > 0) {
                    	Object hist = updateHistory(reqVar, pjp.getArgs()[param], pjp, checkedMethod.getParameterTypes()[param].isPrimitive());
                    	reqVars.put(reqVar.value() + "[]", hist);
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
        	Object value = retVal;
        	String name = returnReqVar.value();
        	boolean isPrimitive = checkedMethod.getReturnType().isPrimitive();
        	
        	if (returnReqVar.isInstance()) {
        		name = returnReqVar.value() + "'";
        		value = getInstanceValueCopy(pjp, returnReqVar);
        		try {
        			isPrimitive = pjp.getThis().getClass().getField(returnReqVar.value()).getType().isPrimitive();
        		} catch (NoSuchFieldException ex) {
        			log.log(Level.SEVERE, "Cannot find instance field with name " + returnReqVar.value() + " in "
        					+ pjp.getThis().getClass(), ex);
        			System.exit(1);
        		}
        	} 
        	
        	reqVars.put(name, value);
        	if (returnReqVar.history() > 0) {
        		Object hist = updateHistory(returnReqVar, value, pjp, isPrimitive);
        		reqVars.put(returnReqVar.value() + "[]", hist);
        	}
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
     * Updates the history for the given requirements variable.
     *
     * @return the updated history object
     */
    protected Object updateHistory(ReqVar reqVar, Object value, 
    		ProceedingJoinPoint pjp, boolean isPrimitive) {
    	Object hist = null;
    	if (reqVarHistories.containsKey(reqVar.value())) {
    		hist = reqVarHistories.get(reqVar.value());
	    	int currentLen = Array.getLength(hist);
	    	
	    	// possibly grow the array
	    	Object newHist = hist;
	    	if (currentLen < reqVar.history()) {
	    		newHist = Array.newInstance(hist.getClass().getComponentType(), currentLen + 1);
	    		currentLen++;
	    	}
	    	
	    	// shift the array
	    	for (int i = currentLen - 1; i > 0; i--) {
	    		Array.set(newHist, i, Array.get(hist, i-1));
	    	}
	    	
	    	hist = newHist;
    	} else {
    		// start it at a length of 1, and we'll increase until it is at reqVar.history()
    		// this is a good way of easily telling if we haven't made history number of calls yet
    		if (isPrimitive) {
    			if (value.getClass().isAssignableFrom(Boolean.class))
    				hist = Array.newInstance(Boolean.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Byte.class))
    				hist = Array.newInstance(Byte.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Character.class))
    				hist = Array.newInstance(Character.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Short.class))
    				hist = Array.newInstance(Short.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Integer.class))
    				hist = Array.newInstance(Integer.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Long.class))
    				hist = Array.newInstance(Long.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Float.class))
    				hist = Array.newInstance(Float.TYPE, 1);
    			else if (value.getClass().isAssignableFrom(Double.class))
    				hist = Array.newInstance(Double.TYPE, 1);
    			else {
    				log.log(Level.SEVERE, value.getClass() + " is not assignable from any primitive types!");
    				System.exit(1);
    			}
    		} else {
    			hist = Array.newInstance(value.getClass(), 1);
    		}
    	}
    	
    	
    	// add a copy of the current value to the array
    	Array.set(hist, 0, copy(value, isPrimitive));

    	// update our copy of the history
		reqVarHistories.put(reqVar.value(), hist);
		
		return hist;
	}

	/**
     * Makes a deep copy of the given element
     */
    protected Object copy(Object value, boolean isPrimitive) {
    	if (value == null || isPrimitive) {
    		return value;
    	} else if (value.getClass().isArray()) {
    		int len = Array.getLength(value);
    		Object copy = Array.newInstance(value.getClass().getComponentType(), Array.getLength(value));
    		for (int i = 0; i < len; i++) {
    			Array.set(copy, i, copy(Array.get(value, i), value.getClass().getComponentType().isPrimitive()));
    		}
    		return copy;
    	} else {
    		// the object had better have a copy constructor!
    		Constructor<?> constructor = null;
    		
    		try {
				constructor = value.getClass().getConstructor(value.getClass());
			} catch (SecurityException e) {
				log.log(Level.SEVERE, "Unable to get constructor of class " + value.getClass(), e);
				System.exit(1);
			} catch (NoSuchMethodException e) {
				log.log(Level.SEVERE, value.getClass() + " must have a copy constructor in order to use it!", e);
				System.exit(1);
			}

			try {
				return constructor.newInstance(value);
			} catch (Exception e) {
				log.log(Level.SEVERE, value.getClass() + " must have a copy constructor in order to use it!", e);
				System.exit(1);
			}
			
			return null; // stub to keep compiler happy
			
    	}
    }
    
    /**
     * helper method to get the method that the proceeding join point matches
     */
    protected Method getMethod(ProceedingJoinPoint pjp) {
        List<Method> possibleMethods = new ArrayList<Method>();
        
        Object[] args = pjp.getArgs();
        Class<?> targetClass = pjp.getSourceLocation().getWithinType();
        
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
    
    /**
     * gets a copy of the current value of an instance variable represented by the given reqvar
     * the reqvar should have isInstance() == true
     * @param pjp
     * @param reqVarName
     * @return
     */
    protected Object getInstanceValueCopy(ProceedingJoinPoint pjp, ReqVar reqvar) {
    	String name = reqvar.value();
		String getter = "get"+Character.toUpperCase(name.charAt(0)) + name.substring(1);
		try {
			Field field = pjp.getThis().getClass().getField(name);
			boolean isPrimitive = field.getType().isPrimitive();
			return copy(field.get(pjp.getThis()), isPrimitive);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "To use instance variable ReqVar annotations, you must provide a getter "
					+ "for that instance variable. An error occurred trying to invoke the getter " + getter, ex);
			System.exit(1);
			return null; // make the compiler happy
		}
    }
    
}
