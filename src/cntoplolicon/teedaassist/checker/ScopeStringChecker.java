package cntoplolicon.teedaassist.checker;

import org.eclipse.core.resources.IMarker;
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

import cntoplolicon.teedaassist.job.CheckScopeStringJob;
import cntoplolicon.teedaassist.util.LoggerUtil;
import cntoplolicon.teedaassist.util.NamingConventionUtil;
import cntoplolicon.teedaassist.visitor.JavaElementDeltaVisitor;

public class ScopeStringChecker implements IMarkerResolutionGenerator2, IElementChangedListener {

	public static final String PAGE_SCOPE_FIELD = "PAGE_SCOPE";
	public static final String REDIRECT_SCOPE_FIELD = "REDIRECT_SCOPE";
	public static final String SUBAPPLICATION_SCOPE_FIELD = "SUBAPPLICATION_SCOPE";
	public static final String MARKER_SCOPE_STRING = "cntoplolicon.teedaassist.marker.scopestring";

	public static final String MARKER_ATTR_CLEAR_RANGE_START = "cntoplolicon.teedaassist.marker.attr.clearstart";
	public static final String MARKER_ATTR_CLEAR_RANGE_END = "cntoplolicon.teedaassist.marker.attr.clearend";
	public static final String MARKER_ATTR_TYPE = "cntoplolicon.teedaassist.marker.attr.type";

	public static final int MARKER_TYPE_DUPLICATE = 0;
	public static final int MARKER_TYPE_MISSING = 1;

	@Override
	public void elementChanged(ElementChangedEvent event) {
		JavaElementDeltaVisitor.accept(event.getDelta(), new JavaElementDeltaVisitor() {
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
					for (IType type : cu.getTypes()) {
						if (type.exists()) {
							visit(type);
						}
					}
				} catch (JavaModelException e) {
					LoggerUtil.warn(e);
					return false;
				}
				return false;
			}

			private boolean visit(IType type) {
				if (type.exists() && NamingConventionUtil.isPageClass(type)) {
					String fieldNames[] = { PAGE_SCOPE_FIELD, PAGE_SCOPE_FIELD,
							SUBAPPLICATION_SCOPE_FIELD };
					boolean hasScopeString = false;
					for (String fieldName : fieldNames) {
						IField field = type.getField(fieldName);
						hasScopeString |= field != null && field.exists();
					}
					if (hasScopeString) {
						new CheckScopeStringJob(type).schedule();
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
				Integer start = (Integer) marker.getAttribute(MARKER_ATTR_CLEAR_RANGE_START);
				Integer end = (Integer) marker.getAttribute(MARKER_ATTR_CLEAR_RANGE_END);
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
			return marker.exists() && marker.getAttribute(MARKER_ATTR_CLEAR_RANGE_START) != null
					&& marker.getAttribute(MARKER_ATTR_CLEAR_RANGE_END) != null;
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
