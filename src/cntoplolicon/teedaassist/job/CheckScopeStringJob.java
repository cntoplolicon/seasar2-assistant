package cntoplolicon.teedaassist.job;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import cntoplolicon.teedaassist.checker.ScopeStringChecker;
import cntoplolicon.teedaassist.util.NamingConventionUtil;

public class CheckScopeStringJob extends Job {

    private IType type;

    public CheckScopeStringJob(IType type) {
        super("checking scope strings");
        this.type = type;
    }

    private void checkScopeStringDecleration(TypeDeclaration td, VariableDeclarationFragment vdf)
            throws JavaModelException {
        Set<String> declaredFieldSet = new HashSet<String>();
        ITypeBinding binding = td.resolveBinding();
        while (binding != null) {
            IVariableBinding[] vbs = binding.getDeclaredFields();
            for (IVariableBinding vb : vbs) {
                declaredFieldSet.add(vb.getName());
            }
            binding = binding.getSuperclass();
        }
        CompilationUnit cu = (CompilationUnit) td.getRoot();
        IResource resource = cu.getJavaElement().getUnderlyingResource();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ICompilationUnit cu = type.getCompilationUnit();
        if (!cu.exists()) {
            return Status.CANCEL_STATUS;
        }
        try {
            cu.getResource().deleteMarkers(ScopeStringChecker.MARKER_SCOPE_STRING, true,
                    IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            return Status.CANCEL_STATUS;
        }
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setResolveBindings(true);
        parser.setSource(cu);
        CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

        astRoot.accept(new ASTVisitor() {

            private TypeDeclaration classDeclaration;

            @Override
            public boolean visit(TypeDeclaration td) {
                boolean isPageClass = NamingConventionUtil.isPageClass(td);
                if (isPageClass) {
                    classDeclaration = td;
                }
                return isPageClass;
            }

            @Override
            public boolean visit(FieldDeclaration fd) {
                ITypeBinding tb = fd.getType().resolveBinding();
                if (!tb.getQualifiedName().equals("java.lang.String")) {
                    return false;
                }
                for (Object object : fd.fragments()) {
                    VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
                    String identifier = vdf.getName().getIdentifier();
                    if (identifier.equals(ScopeStringChecker.PAGE_SCOPE_FIELD)
                            || identifier.equals(ScopeStringChecker.REDIRECT_SCOPE_FIELD)
                            || identifier.equals(ScopeStringChecker.SUBAPPLICATION_SCOPE_FIELD)) {
                        try {
                            checkScopeStringDecleration(classDeclaration, vdf);
                        } catch (JavaModelException e) {
                            return false;
                        }
                    }
                }
                return false;
            }

        });
        return Status.OK_STATUS;
    }
}
