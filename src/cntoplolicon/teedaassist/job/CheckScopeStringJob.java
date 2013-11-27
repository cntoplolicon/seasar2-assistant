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
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import cntoplolicon.teedaassist.checker.ScopeStringChecker;
import cntoplolicon.teedaassist.util.LoggerUtil;
import cntoplolicon.teedaassist.util.NamingConventionUtil;

public class CheckScopeStringJob extends Job {

	private static final String DELIMITER = " *, *";

	private IType type;

	public CheckScopeStringJob(IType type) {
		super("checking scope strings");
		this.type = type;
	}

	private Map<String, Object> createLiteralMarkerAttrs(StringLiteral literal, String field,
			int position, boolean singleField) {
		String literalValue = literal.getLiteralValue();
		int clearRangeStart = 0, clearRangeEnd = 0;
		if (singleField) {
			clearRangeStart = position;
			clearRangeEnd = position + field.length();
		} else {
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
		}

		int startPosition = literal.getStartPosition() + 1;
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(IMarker.CHAR_START, startPosition + position);
		attributes.put(IMarker.CHAR_END, startPosition + position + field.length());
		attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

		if (clearRangeEnd != 0) {
			attributes.put(ScopeStringChecker.MARKER_ATTR_CLEAR_RANGE_START, startPosition
					+ clearRangeStart);
			attributes.put(ScopeStringChecker.MARKER_ATTR_CLEAR_RANGE_END, startPosition
					+ clearRangeEnd);
		}
		return attributes;
	}

	private Map<String, Object> createLineMarkerAttrs(int lineNumber) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		attributes.put(IMarker.LINE_NUMBER, lineNumber);
		return attributes;
	}

	private void createScopeStringMarkerWithAttrs(IResource resource, Map<String, Object> attributes) {
		try {
			IMarker marker = resource.createMarker(ScopeStringChecker.MARKER_SCOPE_STRING);
			marker.setAttributes(attributes);
		} catch (CoreException e) {
			LoggerUtil.warn(e);
		}
	}

	public String getDuplicateErrorMessage(String field) {
		return new StringBuilder().append("duplicate declaration of ").append(field).toString();
	}

	private void markDuplicateField(IResource resource, String field, int lineNumber) {
		Map<String, Object> attributes = createLineMarkerAttrs(lineNumber);
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE,
				ScopeStringChecker.MARKER_TYPE_DUPLICATE);
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE,
				ScopeStringChecker.MARKER_TYPE_DUPLICATE);
		attributes.put(IMarker.MESSAGE, getDuplicateErrorMessage(field));
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private void markDuplicateField(IResource resource, StringLiteral literal, String field,
			int position, boolean singleField) {
		Map<String, Object> attributes = createLiteralMarkerAttrs(literal, field, position,
				singleField);
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE,
				ScopeStringChecker.MARKER_TYPE_DUPLICATE);
		attributes.put(IMarker.MESSAGE, getDuplicateErrorMessage(field));
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private String getMissingErrorMessage(String field) {
		return new StringBuilder().append("field ").append(field).append(" doesn't exist")
				.toString();
	}

	private void markMissingField(IResource resource, StringLiteral literal, String field,
			int position, boolean singleField) {
		Map<String, Object> attributes = createLiteralMarkerAttrs(literal, field, position,
				singleField);
		attributes.put(IMarker.MESSAGE, getMissingErrorMessage(field));
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE, ScopeStringChecker.MARKER_TYPE_MISSING);
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private void markMissingField(IResource resource, String field, int lineNumber) {
		Map<String, Object> attributes = createLineMarkerAttrs(lineNumber);
		attributes.put(IMarker.MESSAGE, getMissingErrorMessage(field));
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private List<Integer> checkInvalidModifiers(IVariableBinding vb) {
		List<Integer> modifiers = new ArrayList<Integer>();
		int[] invalidModifiers = { Modifier.FINAL, Modifier.STATIC };
		for (int modifier : invalidModifiers) {
			if ((vb.getModifiers() & modifier) != 0) {
				modifiers.add(modifier);
			}
		}
		return modifiers;
	}

	public String getModifierErrorMessage(String field, List<Integer> modifiers) {
		return new StringBuilder().append("field ").append(field).append(" cannot be ")
				.append(Modifier.ModifierKeyword.fromFlagValue(modifiers.get(0))).toString();
	}

	private void markInvalidModifiers(IResource resource, StringLiteral literal, String field,
			int position, boolean singleField, List<Integer> modifiers) {
		Map<String, Object> attributes = createLiteralMarkerAttrs(literal, field, position,
				singleField);
		attributes
				.put(ScopeStringChecker.MARKER_ATTR_TYPE, ScopeStringChecker.MARKER_TYPE_MODIFIER);
		String errorMessage = getModifierErrorMessage(field, modifiers);
		attributes.put(IMarker.MESSAGE, errorMessage);
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private void markInvalidModifiers(IResource resource, String field, int lineNumber,
			List<Integer> modifiers) {
		Map<String, Object> attributes = createLineMarkerAttrs(lineNumber);
		attributes
				.put(ScopeStringChecker.MARKER_ATTR_TYPE, ScopeStringChecker.MARKER_TYPE_MODIFIER);
		String errorMessage = getModifierErrorMessage(field, modifiers);
		attributes.put(IMarker.MESSAGE, errorMessage);
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private Map<String, IVariableBinding> getDeclaredFields(TypeDeclaration td) {
		Map<String, IVariableBinding> declaredFields = new HashMap<String, IVariableBinding>();
		ITypeBinding binding = td.resolveBinding();
		IVariableBinding[] vbs = binding.getDeclaredFields();
		for (IVariableBinding vb : vbs) {
			declaredFields.put(vb.getName(), vb);
		}
		return declaredFields;
	}

	private void checkScopeStringDecleration(TypeDeclaration td,
			List<VariableDeclarationFragment> vdfs) throws JavaModelException {
		final Map<String, IVariableBinding> declaredFields = getDeclaredFields(td);
		CompilationUnit cu = (CompilationUnit) td.getRoot();
		final IResource resource = cu.getJavaElement().getUnderlyingResource();
		if (resource == null) {
			return;
		}

		for (VariableDeclarationFragment vdf : vdfs) {
			String value = null;
			Expression initializer = vdf.getInitializer();
			if (initializer != null) {
				value = (String) initializer.resolveConstantExpressionValue();
			}
			if (value == null || value.isEmpty()) {
				continue;
			}
			final List<String> finalFieldsInScope = new ArrayList<String>(Arrays.asList(value
					.split(DELIMITER)));
			final boolean singleField = finalFieldsInScope.size() == 1;

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
						// string literals and contain a partial name of a field
						if (!finalFieldsInScope.contains(field)) {
							continue;
						}
						finalFieldsInScope.remove(field);
						if (processedFields.contains(field)) {
							markDuplicateField(resource, stringLiteral, field, positions[i],
									singleField);
							continue;
						} else {
							processedFields.add(field);
						}

						if (!declaredFields.containsKey(field)) {
							markMissingField(resource, stringLiteral, field, positions[i],
									singleField);
						} else {
							IVariableBinding vb = declaredFields.get(field);
							List<Integer> invalidModifiers = checkInvalidModifiers(vb);
							if (!invalidModifiers.isEmpty()) {
								markInvalidModifiers(resource, stringLiteral, field, positions[i],
										singleField, invalidModifiers);
							}
						}
					}
					return false;
				}
			});

			// remaining fields that not resolved in any string literals
			for (String field : finalFieldsInScope) {
				int lineNumber = cu.getLineNumber(vdf.getStartPosition());
				if (processedFields.contains(field)) {
					markDuplicateField(resource, field, lineNumber);
					continue;
				} else {
					processedFields.add(field);
				}

				if (!declaredFields.containsKey(field)) {
					markMissingField(resource, field, lineNumber);
				} else {
					IVariableBinding vb = declaredFields.get(field);
					List<Integer> invalidModifiers = checkInvalidModifiers(vb);
					if (!invalidModifiers.isEmpty()) {
						markInvalidModifiers(resource, field, lineNumber, invalidModifiers);
					}
				}
			}
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ICompilationUnit cu = type.getCompilationUnit();
		if (!cu.exists()) {
			return Status.CANCEL_STATUS;
		}
		try {
			cu.getResource().deleteMarkers(ScopeStringChecker.MARKER_SCOPE_STRING, true,
					IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			LoggerUtil.warn(e);
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
				if (tb == null || !tb.getQualifiedName().equals("java.lang.String")) {
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
						LoggerUtil.warn(e);
						return false;
					}
				}
				return false;
			}

		});
		return Status.OK_STATUS;
	}
}
