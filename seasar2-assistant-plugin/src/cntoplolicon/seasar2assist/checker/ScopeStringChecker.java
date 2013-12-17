package cntoplolicon.seasar2assist.checker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import cntoplolicon.seasar2assist.constants.ScopeStringConstants;
import cntoplolicon.seasar2assist.job.CheckScopeStringJob;
import cntoplolicon.seasar2assist.preference.ProjectPreferences;
import cntoplolicon.seasar2assist.util.LoggerUtil;
import cntoplolicon.seasar2assist.util.NamingConventionUtil;
import cntoplolicon.seasar2assist.visitor.JavaElementDeltaVisitor;

public class ScopeStringChecker implements IMarkerResolutionGenerator2, IElementChangedListener {

	@Override
	public void elementChanged(ElementChangedEvent event) {

		JavaElementDeltaVisitor.accept(event.getDelta(), new JavaElementDeltaVisitor() {
			
			private ProjectPreferences prefs;

			@Override
			protected boolean preVisit(IJavaElementDelta delta) {
				if ((delta.getFlags() & IJavaElementDelta.F_CHILDREN) != 0) {
					return true;
				}
				if (delta.getElement() instanceof ICompilationUnit) {
					return (delta.getFlags() & IJavaElementDelta.F_CONTENT) != 0;
				}
				return true;
			}

			@Override
			protected boolean visit(ICompilationUnit cu) {
				try {
					IResource resource = cu.getUnderlyingResource();
					if (resource != null) {
						prefs = ProjectPreferences.getPreference(resource
								.getProject());
						if (!prefs.isUseSeasar2Assistant() || !prefs.isCheckScopeStrings()) {
							return false;
						}
					}
				} catch (JavaModelException e) {
					LoggerUtil.warn(e);
					return false;
				}

				try {
					for (IType type : cu.getTypes()) {
						if (type.exists()) {
							visit(type);
						}
					}
				} catch (JavaModelException e) {
					LoggerUtil.error(e);
					return false;
				}
				return false;
			}

			private boolean visit(IType type) {
				if (type.exists() && NamingConventionUtil.isPageClass(type)) {
					String fieldNames[] = { ScopeStringConstants.PAGE_SCOPE_FIELD,
							ScopeStringConstants.PAGE_SCOPE_FIELD,
							ScopeStringConstants.SUBAPPLICATION_SCOPE_FIELD };
					boolean hasScopeString = false;
					for (String fieldName : fieldNames) {
						IField field = type.getField(fieldName);
						hasScopeString |= field != null && field.exists();
					}
					if (hasScopeString) {
						new CheckScopeStringJob(type, prefs).schedule();
					}
				}
				return false;
			}
		});
	}

	static class RemovePropertyResolution implements IMarkerResolution {

		@Override
		public String getLabel() {
			return "remove this property";
		}

		@Override
		public void run(IMarker marker) {
			if (!marker.exists()) {
				return;
			}
			try {
				Integer start = (Integer) marker
						.getAttribute(ScopeStringConstants.MARKER_ATTR_CLEAR_RANGE_START);
				Integer end = (Integer) marker
						.getAttribute(ScopeStringConstants.MARKER_ATTR_CLEAR_RANGE_END);
				ICompilationUnit workingCopy = (ICompilationUnit) JavaCore.create(marker
						.getResource());
				DeleteEdit deleteEdit = new DeleteEdit(start, end - start);
				workingCopy.applyTextEdit(deleteEdit, null);
			} catch (JavaModelException e) {
				LoggerUtil.error(e);
			} catch (CoreException e) {
				LoggerUtil.error(e);
			}
		}
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			return marker.exists()
					&& marker.getAttribute(ScopeStringConstants.MARKER_ATTR_CLEAR_RANGE_START) != null
					&& marker.getAttribute(ScopeStringConstants.MARKER_ATTR_CLEAR_RANGE_END) != null;
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (hasResolutions(marker)) {
			return new IMarkerResolution[] { new RemovePropertyResolution() };
		} else {
			return new IMarkerResolution[] {};
		}
	}
}
