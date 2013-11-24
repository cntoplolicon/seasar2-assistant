package cntoplolicon.teedaassist.visitor;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public abstract class JavaElementDeltaVisitor {

    public static void accept(IJavaElementDelta delta, JavaElementDeltaVisitor visitor) {
        if (visitor.visit(delta.getElement())) {
            accept(delta.getAffectedChildren(), visitor);
        }
    }

    public static void accept(IJavaElementDelta[] deltas, JavaElementDeltaVisitor visitor) {
        for (IJavaElementDelta delta : deltas) {
            accept(delta, visitor);
        }
    }

    protected boolean visit(IJavaElement element) {
        switch (element.getElementType()) {
        case IJavaElement.JAVA_MODEL:
            return visit((IJavaModel) element);
        case IJavaElement.JAVA_PROJECT:
            return visit((IJavaProject) element);
        case IJavaElement.PACKAGE_FRAGMENT_ROOT:
            return visit((IPackageFragmentRoot) element);
        case IJavaElement.PACKAGE_FRAGMENT:
            return visit((IPackageFragment) element);
        case IJavaElement.COMPILATION_UNIT:
            return visit((ICompilationUnit) element);
        case IJavaElement.CLASS_FILE:
            return visit((IClassFile) element);
        }
        return true;
    }

    protected boolean visit(IJavaModel model) {
        return true;
    }

    protected boolean visit(IJavaProject project) {
        return true;
    }

    protected boolean visit(IPackageFragmentRoot root) {
        return true;
    }

    protected boolean visit(IPackageFragment fragment) {
        return true;
    }

    protected boolean visit(ICompilationUnit unit) {
        return true;
    }

    protected boolean visit(IClassFile clazz) {
        return true;
    }
}
