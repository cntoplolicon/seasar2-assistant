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

	private Map<String, Object> createLiteralMarkerAttrs(StringLiteral literal, String property,
			int position, boolean singleProperty) {
		String literalValue = literal.getLiteralValue();
		int clearRangeStart = 0, clearRangeEnd = 0;
		if (singleProperty) {
			clearRangeStart = position;
			clearRangeEnd = position + property.length();
		} else {
			int lastComma = literalValue.substring(0, position).lastIndexOf(',');
			if (lastComma != -1) {
				clearRangeStart = lastComma;
				clearRangeEnd = position + property.length();
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
		attributes.put(IMarker.CHAR_END, startPosition + position + property.length());
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

	public String getDuplicateErrorMessage(String property) {
		return new StringBuilder().append("duplicate declaration of ").append(property).toString();
	}

	private void markDuplicateProperty(IResource resource, String property, int lineNumber) {
		Map<String, Object> attributes = createLineMarkerAttrs(lineNumber);
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE,
				ScopeStringChecker.MARKER_TYPE_DUPLICATE);
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE,
				ScopeStringChecker.MARKER_TYPE_DUPLICATE);
		attributes.put(IMarker.MESSAGE, getDuplicateErrorMessage(property));
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private void markDuplicateProperty(IResource resource, StringLiteral literal, String property,
			int position, boolean singleProperty) {
		Map<String, Object> attributes = createLiteralMarkerAttrs(literal, property, position,
				singleProperty);
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE,
				ScopeStringChecker.MARKER_TYPE_DUPLICATE);
		attributes.put(IMarker.MESSAGE, getDuplicateErrorMessage(property));
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private String getMissingErrorMessage(String property) {
		return new StringBuilder().append("property ").append(property).append(" doesn't exist")
				.toString();
	}

	private void markMissingProperty(IResource resource, StringLiteral literal, String property,
			int position, boolean singleProperty) {
		Map<String, Object> attributes = createLiteralMarkerAttrs(literal, property, position,
				singleProperty);
		attributes.put(IMarker.MESSAGE, getMissingErrorMessage(property));
		attributes.put(ScopeStringChecker.MARKER_ATTR_TYPE, ScopeStringChecker.MARKER_TYPE_MISSING);
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	private void markMissingProperty(IResource resource, String property, int lineNumber) {
		Map<String, Object> attributes = createLineMarkerAttrs(lineNumber);
		attributes.put(IMarker.MESSAGE, getMissingErrorMessage(property));
		createScopeStringMarkerWithAttrs(resource, attributes);
	}

	public String getModifierErrorMessage(String property, List<Integer> modifiers) {
		return new StringBuilder().append("property ").append(property).append(" cannot be ")
				.append(Modifier.ModifierKeyword.fromFlagValue(modifiers.get(0))).toString();
	}

	private void checkScopeStringDecleration(TypeDeclaration td,
			List<VariableDeclarationFragment> vdfs) throws JavaModelException {
		final Set<String> declaredProperties = NamingConventionUtil.getProperties(td);
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
			final List<String> finalPropertiesInScope = new ArrayList<String>(Arrays.asList(value
					.split(DELIMITER)));
			final boolean singleProperty = finalPropertiesInScope.size() == 1;

			final Set<String> processedProperties = new HashSet<String>();
			vdf.accept(new ASTVisitor() {
				@Override
				public boolean visit(StringLiteral stringLiteral) {
					String literalValue = stringLiteral.getLiteralValue();
					String[] propertysInScope = literalValue.split(DELIMITER);
					int[] positions = new int[propertysInScope.length];
					for (int i = 0, j = 0; i < propertysInScope.length; i++) {
						positions[i] = literalValue.indexOf(propertysInScope[i], j);
						j += propertysInScope[i].length();
					}

					for (int i = 0; i < propertysInScope.length; i++) {
						String property = propertysInScope[i];
						// string literals and contain a partial name of a
						// property
						if (!finalPropertiesInScope.contains(property)) {
							continue;
						}
						finalPropertiesInScope.remove(property);
						if (processedProperties.contains(property)) {
							markDuplicateProperty(resource, stringLiteral, property, positions[i],
									singleProperty);
							continue;
						} else {
							processedProperties.add(property);
						}

						if (!declaredProperties.contains(property)) {
							markMissingProperty(resource, stringLiteral, property, positions[i],
									singleProperty);
						}
					}
					return false;
				}
			});

			// remaining propertys that not resolved in any string literals
			for (String property : finalPropertiesInScope) {
				int lineNumber = cu.getLineNumber(vdf.getStartPosition());
				if (processedProperties.contains(property)) {
					markDuplicateProperty(resource, property, lineNumber);
					continue;
				} else {
					processedProperties.add(property);
				}

				if (!declaredProperties.contains(property)) {
					markMissingProperty(resource, property, lineNumber);
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
		parser.setIgnoreMethodBodies(true);
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
