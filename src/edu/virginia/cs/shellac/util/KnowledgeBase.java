package edu.virginia.cs.shellac.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * This represents all of the knowledge we have about different annotated
 * classes, methods, parameters, and fields. We also keep information about
 * the annotations themselves here so we can generate useful messages.
 * 
 * @author btaitelb
 */
public class KnowledgeBase {
	
	private static final String ERROR_MARKER = "edu.virginia.cs.shellac.markers.ErrorMarker";
	private static final String PROOF_MARKER = "edu.virginia.cs.shellac.markers.ProofMarker";
	
	// keep a map of which methods claim to satisfy which requirements
	private Map<String, String[]> methodRequirements = new HashMap<String, String[]>();
	
	// keep track of which methods check which requirements so we can
	// properly update the list of requirements being checked when a method changes.
	private Map<String, String> checkedRequirementsByMethod = new HashMap<String, String>();
	
	// keep a set of which requirements have dynamic checkers
	private Set<String> checkedRequirements = new HashSet<String>();
	
	// keep track of which proofs the user claims to have proven,
	// based on checking off the proof task markers we add
	private Set<String> completedProofs = new HashSet<String>();
	
	// keep track of which proofs belong to which unit so we can properly
	// clear out old proofs that may have changed (or been marked as not completed)
	private Map<String, ArrayList<String>> completedProofsByUnit = new HashMap<String, ArrayList<String>>();
	
	private static KnowledgeBase instance = null;
	private KnowledgeBase() {}
	
	public static KnowledgeBase instance() {
		if (instance == null)
			instance = new KnowledgeBase();
		
		return instance;
	}
	
	/**
	 * updates our list of completed proofs to include only those whose 
	 * task markers have been marked as completed.
	 * 
	 * This removes all the completed proofs for the compilation unit, and then fills
	 * in the completed proofs with information from the proof markers marked as done.
	 * @param unit
	 */
	private void updateCompletedProofs(CompilationUnit unit) {
		String name = unit.getJavaElement().getElementName();
		ArrayList<String> previouslyCompleted = completedProofsByUnit.get(name);
		if (previouslyCompleted != null) {
			for (String proof : previouslyCompleted) {
				completedProofs.remove(proof);
			}
		}

		ArrayList<String> doneProofs = new ArrayList<String>();
		
		try {
			IMarker[] proofMarkers = unit.getJavaElement().getResource().findMarkers(PROOF_MARKER, true, IResource.DEPTH_INFINITE);
			//System.out.println(proofMarkers.length + " proof markers");
			for (IMarker marker : proofMarkers) {
				if (marker.getAttribute(IMarker.DONE, false)) {
					doneProofs.add(marker.getAttribute(IMarker.MESSAGE, ""));
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (doneProofs.size() > 0) {
			completedProofsByUnit.put(name, doneProofs);
			completedProofs.addAll(doneProofs);
		}
	}
	
	public void processContext(ReconcileContext context) {
		// for now, reprocess the entire compilation unit
		
		try {
			CompilationUnit unit = context.getAST3();
			if (unit == null)
				return;
			
			// update the completed proofs for this context
			updateCompletedProofs(unit);
			

			processContext(unit);
			
			// we always want to process the context twice. The first time
			// builds up the KB, so that the second run can more accurately
			// report errors
			processContext(unit);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	public void processContext(CompilationUnit unit) {
		try {			
			unit.getJavaElement().getResource().deleteMarkers(ERROR_MARKER, false, 0);
			unit.getJavaElement().getResource().deleteMarkers(PROOF_MARKER, false, 0);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		ASTNode rootNode = unit;
		while (rootNode.getParent() != null) {
			rootNode = rootNode.getParent();
		}
		rootNode.accept(new KnowledgeBaseUpdatingASTVisitor(unit));
	}
	
	private void addError(CompilationUnit unit, ASTNode node, String msg) {
		try {
			IMarker marker = unit.getJavaElement().getResource().createMarker(ERROR_MARKER);
			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.CHAR_START, node.getStartPosition());
			marker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	// TODO: When I save the file, the completed tasks get wiped away :(
	private void addProof(CompilationUnit unit, ASTNode node, String msg) {
		// has the user marked this proof as completed?
		boolean isDone = completedProofs.contains(msg);
		
		try {
			IMarker marker = unit.getJavaElement().getResource().createMarker(PROOF_MARKER);
			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.CHAR_START, node.getStartPosition());
			marker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
			marker.setAttribute(IMarker.DONE, isDone);
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		if (!isDone) {
			addError(unit, node, "One or more proofs need to be completed, or a dynamic checker must be added.");
		}
	}
	
	private String getKeyName(MethodDeclaration node) {
		// find the class that this method is declared in
		TypeDeclaration parent = (TypeDeclaration) node.getParent();
		return parent.getName().getFullyQualifiedName() + "." + node.getName().getFullyQualifiedName();
	}
	
	private String getKeyName(MethodInvocation node) {
		return node.resolveMethodBinding().getDeclaringClass().getName() + "." + node.getName().getFullyQualifiedName();
	}
	
	/**
	 * Gets the requirements that a given method is annotated to satisfy,
	 * and also updates the methodRequirements map with this info.
	 * 
	 * @param node
	 * @return
	 */
	private String[] getRequirementsFromAnnotation(MethodDeclaration node) {
		String[] reqs = new String[] {};
		
		// assumption: a method can have only one @Satisfies declaration
		// assumption: the value of an @Satisfies annotation is an Object[]
		
		for (IExtendedModifier modifier : (List<IExtendedModifier>)node.modifiers()) {
			if (modifier.isAnnotation()) {
				Annotation annotation = (Annotation) modifier;
				if (annotation.getTypeName().toString().equals("Satisfies")) {
					for (IMemberValuePairBinding binding : annotation.resolveAnnotationBinding().getAllMemberValuePairs()) {
						if (binding.getName().equals("value")) {
							Object[] reqObjs = (Object[]) binding.getValue();
							reqs = new String[reqObjs.length];
							for (int i = 0; i < reqObjs.length; i++) {
								reqs[i] = reqObjs[i].toString();
							}
						}
					}
				}
			}
		}
		
		//System.out.println("Got requirements: " + Arrays.toString(reqs));

		methodRequirements.put(getKeyName(node), reqs);
		return reqs;
	}
	
	/**
	 * Gets the requirements that a given method claims to check,
	 * and also updates the checkedRequirements set with this info.
	 * 
	 * @param node
	 * @return
	 */
	private String getCheckedRequirementFromAnnotation(MethodDeclaration node) {
		// assumption: a method can only have one @Checks declaration
		// assumption: a method can only check a single requirement, so the
		//             value will always be a single string
		
		String req = checkedRequirementsByMethod.get(getKeyName(node));
		if (req != null) {
			checkedRequirements.remove(req);
		}
		
		String checkedReq = null;
		
		for (IExtendedModifier modifier : (List<IExtendedModifier>)node.modifiers()) {
			if (modifier.isAnnotation()) {
				Annotation annotation = (Annotation) modifier;
				if (annotation.getTypeName().toString().equals("Checks")) {
					for (IMemberValuePairBinding binding : annotation.resolveAnnotationBinding().getAllMemberValuePairs()) {
						if (binding.getName().equals("value")) {
							checkedReq = (String) binding.getValue();
						}
					}
				}
			}
		}
		
		if (checkedReq != null) {
			checkedRequirements.add(checkedReq);
			checkedRequirementsByMethod.put(getKeyName(node), checkedReq);
		}

		return checkedReq;
	}
	
	/**
	 * Gets the requirements that the given method claims to satisfy.
	 * This will only return requirements if the method has previously
	 * been visited.
	 * 
	 * @param methodName
	 * @return
	 */
	private String[] getRequirements(String methodName) {
		if (methodRequirements.containsKey(methodName)) {
			return methodRequirements.get(methodName);
		} else {
			return new String[] {};
		}
	}
	
	/**
	 * Whether or not there exists a dynamic check for the given requirement
	 * 
	 * @param requirement
	 * @return
	 */
	private boolean hasCheck(String requirement) {
		return checkedRequirements.contains(requirement);
	}
	
	
	/**
	 * TODO:
	 *  1. track all the ReqVar variables (both in the method params as well
	 *     as in the local return variables
	 *  2. ensure that assignment of reqvars is only through method calls
	 *  3. ensure that method calls list their requirements (@Satisfies)
	 *  4. construct proof obligations based on where method calls are
	 *     (inside if-statements or inside loops)
	 *  5. ignore errors from #s 2,3,4 if there is a dynamic checker 
	 * @author btaitelb
	 *
	 */
	public class KnowledgeBaseUpdatingASTVisitor extends ASTVisitor {
		String[] unprovenReqs = {};
		
		// are we currently analyzing a method with unsatisfied requirements
		boolean inSatisfiesMethod = false;
		
		CompilationUnit unit = null;
		String methodCallString = "";

		public KnowledgeBaseUpdatingASTVisitor(CompilationUnit unit) {
			super();
			this.unit = unit;
		}
		
		// TODO: handle nested classes / methods
		@Override
		public boolean visit(MethodDeclaration node) {
			if (inSatisfiesMethod) {
				addError(unit, node, "shellac cannot currently handle nested methods");
				return false;
			} else {
				
				if (getCheckedRequirementFromAnnotation(node) != null) {
					return false;
				} else {
					// a method cannot be both a checker and claim to satisfy
					// requirements

					ArrayList<String> reqs = new ArrayList<String>();
					for (String req : getRequirementsFromAnnotation(node)) {
						if (!hasCheck(req)) {
							reqs.add(req);
						}
					}

					unprovenReqs = reqs.toArray(new String[reqs.size()]);
					methodCallString = "";
					
					// for now at least, only consider ourselves in a satisfies method
					// if there are unchecked requirements
					inSatisfiesMethod = (unprovenReqs.length > 0);
					
					return true;
				}
			}
		}
		
		@Override
		public boolean visit(ConditionalExpression node) {
			//System.out.println("In conditional expression " + node);
			if (inSatisfiesMethod)
				methodCallString += "; (";
			return true;
		}
		
		@Override
		public void endVisit(ConditionalExpression node) {
			//System.out.println("Done with conditional expression " + node);
			if (inSatisfiesMethod)
				methodCallString += ")? ";
		}

		@Override
		public boolean visit(IfStatement node) {
			//System.out.println("In if-stmt " + node);
			if (inSatisfiesMethod) {
				node.getExpression().accept(this);
				methodCallString += "; (";
				node.getThenStatement().accept(this);
				
				if (node.getElseStatement() == null) {
					methodCallString += ")? ";
				} else {
					methodCallString += " || ";
					node.getElseStatement().accept(this);
					methodCallString += ") ";
				}
				return false;
			}
			return true;
		}
		
		
		@Override
		public boolean visit(SwitchCase node) {
			//System.out.println("In switchcase " + node);
			if (inSatisfiesMethod)
				methodCallString += "; (";
			return true;
		}
		@Override
		public void endVisit(SwitchCase node) {
			//System.out.println("Done with switchcase " + node);
			if (inSatisfiesMethod)
				methodCallString += ")? ";
		}

		@Override
		public boolean visit(DoStatement node) {
			//System.out.println("In Do statement " + node);
			if (inSatisfiesMethod)
				methodCallString += "; (";
			return true;
		}
		@Override
		public void endVisit(DoStatement node) {
			//System.out.println("Done with do stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += ")* ";
		}
		
		@Override
		public boolean visit(ForStatement node) {
			//System.out.println("In for stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += "; (";
			return true;
		}
		@Override
		public void endVisit(ForStatement node) {
			//System.out.println("Done with for stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += ")* ";
		}

		@Override
		public boolean visit(WhileStatement node) {
			//System.out.println("In while stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += "; (";
			return true;
		}
		@Override
		public void endVisit(WhileStatement node) {
			//System.out.println("done with while stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += ")* ";
		}

		@Override
		public boolean visit(EnhancedForStatement node) {
			//System.out.println("in enhanced for stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += "; (";
			return true;
		}
		@Override
		public void endVisit(EnhancedForStatement node) {
			//System.out.println("Done with enhanced for stmt " + node);
			if (inSatisfiesMethod)
				methodCallString += ")* ";
		}

		@Override
		public boolean visit(MethodInvocation node) {
			if (inSatisfiesMethod) {
				// get the reqs that this method claims to satisfy
				
				String[] reqs = getRequirements(getKeyName(node));
				if (reqs.length == 0) {
					addError(unit, node, "method calls to non-annotated methods are not allowed inside annotated methods");
				} else {
					methodCallString += "; ( " + reqs[0];
					for (int i = 1; i < reqs.length; i++) {
						methodCallString += " && " + reqs[i];
					}
					methodCallString += " ) ";
				}
			}

			return true;
		}
		
		@Override
		public void endVisit(MethodDeclaration node) {
			inSatisfiesMethod = false;
			
			// clean up the methodCallString
			methodCallString = methodCallString.replaceAll(";\\s*;", ";");
			
			// remove semi-colon from beginning of string
			methodCallString = methodCallString.replaceAll("^;\\s*", "");
			
			// remove useless semi-colons
			methodCallString = methodCallString.replaceAll("\\(\\s*;\\s*", "(");
			methodCallString = methodCallString.replaceAll("\\|\\|\\s*;", "||");

			// TODO: remove marks of loops/conditionals without any method calls
			// TODO: simplify qualifiers, like convert (( something )?)* to ( something )*
			// but we can't use regular expressions to match parens, and
			// remove the ones we want :(

			
			for (String req : unprovenReqs) {
				addProof(unit, node, methodCallString + " -> " + req);
			}
			unprovenReqs = new String[] {};
		}

	}
	
	
	
}
