package cntoplolicon.teedaassist.job;

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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import cntoplolicon.teedaassist.checker.ScopeStringChecker;
import cntoplolicon.teedaassist.util.NamingConventionUtil;

public class CheckScopeStringJob extends Job {

    private static final String DELIMITER = " *, *";

    private IType type;

    public CheckScopeStringJob(IType type) {
        super("checking scope strings");
        this.type = type;
    }

    private void markDuplicateField(IResource resource, StringLiteral literal, String field,
            int position) {
        String literalValue = literal.getLiteralValue();
        int clearRangeStart = 0, clearRangeEnd = 0;
        int lastComma = literalValue.substring(0, position).lastIndexOf(',');
        if (lastComma != -1) {
            clearRangeStart = lastComma;
            clearRangeEnd = position + field.length();
        } else {
            int nextComma = literalValue.indexOf(',', position);
            if (nextComma != -1) {
                clearRangeStart = position;
                clearRangeEnd = nextComma + 1;
            }
        }

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(IMarker.CHAR_START, literal.getStartPosition() + 1 + position);
        attributes
                .put(IMarker.CHAR_END, literal.getStartPosition() + 1 + position + field.length());
        attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        attributes.put(IMarker.MESSAGE, "duplicate field declaration in scope string");
        if (clearRangeEnd != 0) {
            attributes.put(ScopeStringChecker.MARKER_ATTR_CLEAR_RANGE_START, clearRangeStart);
            attributes.put(ScopeStringChecker.MARKER_ATTR_CLEAR_RANGE_END, clearRangeEnd);
        }
        try {
            IMarker marker = resource.createMarker(IMarker.PROBLEM);
            marker.setAttributes(attributes);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private void markMissingField(IResource resource, StringLiteral literal, String field,
            int position) {

    }

    private void checkScopeStringDecleration(TypeDeclaration td,
            List<VariableDeclarationFragment> vdfs) throws JavaModelException {
        final Set<String> declaredFieldSet = new HashSet<String>();
        ITypeBinding binding = td.resolveBinding();
        while (binding != null) {
            IVariableBinding[] vbs = binding.getDeclaredFields();
            for (IVariableBinding vb : vbs) {
                declaredFieldSet.add(vb.getName());
            }
            binding = binding.getSuperclass();
        }

        CompilationUnit cu = (CompilationUnit) td.getRoot();
        final IResource resource = cu.getJavaElement().getUnderlyingResource();

        for (VariableDeclarationFragment vdf : vdfs) {
            String value = null;
            Expression initializer = vdf.getInitializer();
            if (initializer != null) {
                value = (String) initializer.resolveConstantExpressionValue();
            }
            if (value == null) {
                continue;
            }
            final Set<String> finalFieldsInScope = new HashSet<String>(Arrays.asList(value
                    .split(DELIMITER)));
            final Set<String> processedFields = new HashSet<String>();
            vdf.accept(new ASTVisitor() {
                @Override
                public boolean visit(StringLiteral stringLiteral) {
                    String literalValue = stringLiteral.getLiteralValue();
                    String[] fieldsInScope = literalValue.split(DELIMITER);
                    int[] positions = new int[fieldsInScope.length];
                    for (int i = 0, j = 0; i < fieldsInScope.length; i++) {
                        positions[i] = literalValue.indexOf(fieldsInScope[i], j);
                        j += fieldsInScope[i].length();
                    }

                    for (int i = 0; i < fieldsInScope.length; i++) {
                        String field = fieldsInScope[i];
                        if (!finalFieldsInScope.contains(field)) {
                            continue;
                        }
                        if (processedFields.contains(field)) {
                            markDuplicateField(resource, stringLiteral, field, positions[i]);
                        } else {
                            processedFields.add(field);
                        }
                        if (!declaredFieldSet.contains(field)) {
                            markMissingField(resource, stringLiteral, field, positions[i]);
                        }
                    }
                    return false;
                }
            });
        }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ICompilationUnit cu = type.getCompilationUnit();
        if (!cu.exists()) {
            return Status.CANCEL_STATUS;
        }
        try {
            cu.getResource().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            e.printStackTrace();
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
                List<VariableDeclarationFragment> vdfs = new ArrayList<VariableDeclarationFragment>();
                for (Object object : fd.fragments()) {
                    VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
                    String identifier = vdf.getName().getIdentifier();
                    if (identifier.equals(ScopeStringChecker.PAGE_SCOPE_FIELD)
                            || identifier.equals(ScopeStringChecker.REDIRECT_SCOPE_FIELD)
                            || identifier.equals(ScopeStringChecker.SUBAPPLICATION_SCOPE_FIELD)) {
                        vdfs.add(vdf);
                    }
                    try {
                        checkScopeStringDecleration(classDeclaration, vdfs);
                    } catch (JavaModelException e) {
                        return false;
                    }
                }
                return false;
            }

        });
        return Status.OK_STATUS;
    }
}
