package cntoplolicon.teedaassist.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class NamingConventionUtil {

    private static final String PAGE_CLASS_SUFFIX = "Page";
    private static final String JAVA_EXTENSION = ".java";

    private static String getPrimaryClassName(ICompilationUnit cu) {
        String filename = cu.getElementName();
        if (!filename.endsWith(JAVA_EXTENSION)) {
            return null;
        }
        return filename.substring(0, filename.length() - JAVA_EXTENSION.length());
    }

    public static boolean isPageClass(IType type) {
        ICompilationUnit cu = type.getCompilationUnit();
        return cu.exists() && getPrimaryClassName(cu).equals(type.getElementName())
                && type.getElementName().endsWith(PAGE_CLASS_SUFFIX);
    }

    public static boolean isPageClass(TypeDeclaration td) {
        if (!(td.getParent() instanceof CompilationUnit)) {
            return false;
        }
        CompilationUnit astCu = (CompilationUnit) td.getParent();
        if (!(astCu.getJavaElement() instanceof ICompilationUnit)) {
            return false;
        }
        ICompilationUnit jmCu = (ICompilationUnit) astCu.getJavaElement();
        return td.getName().getIdentifier().equals(getPrimaryClassName(jmCu));
    }
}
