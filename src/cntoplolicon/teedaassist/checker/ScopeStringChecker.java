package cntoplolicon.teedaassist.checker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import cntoplolicon.teedaassist.job.CheckScopeStringJob;
import cntoplolicon.teedaassist.util.NamingConventionUtil;
import cntoplolicon.teedaassist.visitor.JavaElementDeltaVisitor;

public class ScopeStringChecker implements IMarkerResolutionGenerator2, IElementChangedListener {

    public static final String PAGE_SCOPE_FIELD = "PAGE_SCOPE";
    public static final String REDIRECT_SCOPE_FIELD = "REDIRECT_SCOPE";
    public static final String SUBAPPLICATION_SCOPE_FIELD = "SUBAPPLICATION_SCOPE";
    public static final String MARKER_SCOPE_STRING_DUPLICATE = "cntoplolicon.teedaassist.marker.scopestring.duplicatefield";
    public static final String MARKER_SCOPE_STRING_MISSING = "cntoplolicon.teedaassist.marker.scopestring.missingfield";

    public static final String MARKER_ATTR_CLEAR_RANGE_START = "cntoplolicon.teedaassist.attr.clearstart";
    public static final String MARKER_ATTR_CLEAR_RANGE_END = "cntoplolicon.teedaassist.attr.clearend";

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        return null;
    }

    @Override
    public void elementChanged(ElementChangedEvent event) {
        JavaElementDeltaVisitor.accept(event.getDelta(), new JavaElementDeltaVisitor() {
            @Override
            protected boolean visit(ICompilationUnit cu) {
                try {
                    for (IType type : cu.getTypes()) {
                        visit(type);
                    }
                } catch (JavaModelException e) {
                    return false;
                }
                return false;
            }

            private boolean visit(IType type) {
                if (NamingConventionUtil.isPageClass(type)) {
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

    @Override
    public boolean hasResolutions(IMarker arg0) {
        return false;
    }
}
