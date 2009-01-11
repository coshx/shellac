package edu.virginia.cs.shellac;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.ReconcileContext;

import edu.virginia.cs.shellac.util.KnowledgeBase;

/**
 * We use this because we want to perform annotation processing, but that is
 * not currently working on AspectJ projects.
 * 
 * With this participant on AspectJ projects, we only receive calls to
 * isActive and reconcile
 * 
 * @author btaitelb
 *
 */
public class StaticAnalyzer extends
		org.eclipse.jdt.core.compiler.CompilationParticipant {

	private KnowledgeBase knowledgeBase = KnowledgeBase.instance();
	
	public StaticAnalyzer() {
	}

	/**
	 * Determine if we've done a full analysis of this project, and if not,
	 * then complete one.
	 * 
	 * If we have already gone through the entire project, then we can use the deltas
	 * passed into reconcile to make smaller changes.
	 */
	@Override
	public boolean isActive(IJavaProject project) {
		return true;
	}
	
	@Override
	public void reconcile(ReconcileContext context) {
		knowledgeBase.processContext(context);
		super.reconcile(context);
	}
}
