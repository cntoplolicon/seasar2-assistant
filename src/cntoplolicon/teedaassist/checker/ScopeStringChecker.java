package cntoplolicon.teedaassist.checker;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.JavaModelException;

import cntoplolicon.teedaassist.visitor.JavaElementDeltaVisitor;

public class ScopeStringChecker implements IMarkerResolutionGenerator2, IElementChangedListener {

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return null;
	}

	@Override
	public void elementChanged(ElementChangedEvent event) {
		JavaElementDeltaVisitor.accept(event.getDelta(), new JavaElementDeltaVisitor() {
			@Override
			protected boolean visit(ICompilationUnit unit) {
				try {
					System.out.println(unit.getUnderlyingResource().getName());
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
				return false;
			}
		});
	}

	@Override
	public boolean hasResolutions(IMarker arg0) {
		return false;
	}

}
