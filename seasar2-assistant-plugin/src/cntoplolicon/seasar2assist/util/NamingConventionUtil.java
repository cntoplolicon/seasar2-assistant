package cntoplolicon.seasar2assist.util;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import cntoplolicon.seasar2assist.constants.DaoConstants;
import cntoplolicon.seasar2assist.preference.ProjectPreferences;

public final class NamingConventionUtil {

	private static final String PAGE_CLASS_SUFFIX = "Page";
	private static final String DAO_CLASS_SUFFIX = "Dao";
	private static final String JAVA_EXTENSION = ".java";
	private static final String GETTER_PREFIX = "get";
	private static final String BOOLEAN_GETTER_PREFIX = "is";
	private static final String SETTER_PREFIX = "set";

	private NamingConventionUtil() {
	}

	public static String getPrimaryClassName(ICompilationUnit cu) {
		String filename = cu.getElementName();
		if (!filename.endsWith(JAVA_EXTENSION)) {
			return null;
		}
		return filename.substring(0, filename.length() - JAVA_EXTENSION.length());
	}

	private static String getRootPackage(ICompilationUnit cu) {
		if (!cu.exists()) {
			return null;
		}
		IProject project = cu.getJavaProject().getProject();
		ProjectPreferences preferences = ProjectPreferences.getPreference(project);
		String rootPackage = preferences.getRootPackage();
		return rootPackage.isEmpty() ? null : rootPackage;
	}

	public static IPackageDeclaration getPackageDeclaration(ICompilationUnit cu) {
		try {
			IPackageDeclaration[] pds = cu.getPackageDeclarations();
			int i = 0, j = -1, count = 0;
			for (; i < pds.length; i++) {
				if (pds[i].exists()) {
					count++;
					j = i;
				}
			}
			if (count != 1) {
				return null;
			}
			return pds[j];
		} catch (JavaModelException e) {
			LoggerUtil.error(e);
			return null;
		}
	}

	private static boolean isEntityCompilationUnit(ICompilationUnit cu) {
		String rootPackage = getRootPackage(cu);
		if (rootPackage == null) {
			return false;
		}
		IPackageDeclaration pd = getPackageDeclaration(cu);
		if (pd == null) {
			return false;
		}
		return pd.exists() && pd.getElementName().startsWith(rootPackage)
				&& pd.getElementName().endsWith("entity");
	}

	public static boolean isEntity(IType type) {
		ICompilationUnit cu = type.getCompilationUnit();
		return type.exists() && isEntityCompilationUnit(cu)
				&& getPrimaryClassName(cu).equals(type.getElementName());
	}

	private static boolean isDaoCompilationUnit(ICompilationUnit cu) {
		String rootPackage = getRootPackage(cu);
		if (rootPackage == null) {
			return false;
		}
		IPackageDeclaration pd = getPackageDeclaration(cu);
		if (pd == null) {
			return false;
		}
		return pd.exists() && pd.getElementName().startsWith(rootPackage)
				&& pd.getElementName().endsWith("dao");
	}

	public static boolean isDao(IType type) {
		ICompilationUnit cu = type.getCompilationUnit();
		return type.exists() && isDaoCompilationUnit(cu)
				&& getPrimaryClassName(cu).equals(type.getElementName())
				&& type.getElementName().endsWith(DAO_CLASS_SUFFIX);
	}

	private static boolean isPageClassCompilationUnit(ICompilationUnit cu) {
		String rootPackage = getRootPackage(cu);
		if (rootPackage == null) {
			return false;
		}
		IPackageDeclaration pd = getPackageDeclaration(cu);
		if (pd == null) {
			return false;
		}
		return pd.exists() && pd.getElementName().startsWith(rootPackage + ".web");
	}

	public static boolean isPageClass(IType type) {
		ICompilationUnit cu = type.getCompilationUnit();
		return type.exists() && isPageClassCompilationUnit(cu)
				&& getPrimaryClassName(cu).equals(type.getElementName())
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
		return isPageClassCompilationUnit(jmCu)
				&& td.getName().getIdentifier().equals(getPrimaryClassName(jmCu));
	}

	private static boolean isValidPropertyModifier(int modifer) {
		return (modifer & Modifier.STATIC) == 0 && (modifer & Modifier.FINAL) == 0
				&& (modifer & Modifier.PUBLIC) != 0;
	}

	private static boolean isGetter(String methodName) {
		return (methodName.startsWith(GETTER_PREFIX)
				&& methodName.length() > GETTER_PREFIX.length() && Character.isUpperCase(methodName
				.charAt(GETTER_PREFIX.length())))
				|| (methodName.startsWith(BOOLEAN_GETTER_PREFIX)
						&& methodName.length() > BOOLEAN_GETTER_PREFIX.length() && Character
							.isUpperCase(methodName.charAt(BOOLEAN_GETTER_PREFIX.length())));

	}

	private static boolean isSetter(String methodName) {
		return methodName.startsWith(SETTER_PREFIX) && methodName.length() > SETTER_PREFIX.length()
				&& Character.isUpperCase(methodName.charAt(GETTER_PREFIX.length()));
	}

	private static String getPropertyNameFromGetter(String getter) {
		if (!isGetter(getter)) {
			return null;
		}
		if (getter.startsWith(GETTER_PREFIX)) {
			return Character.toLowerCase(getter.charAt(GETTER_PREFIX.length()))
					+ getter.substring(1 + GETTER_PREFIX.length());
		} else {
			return Character.toLowerCase(getter.charAt(BOOLEAN_GETTER_PREFIX.length()))
					+ getter.substring(1 + BOOLEAN_GETTER_PREFIX.length());
		}
	}

	private static String getPropertyNameFromSetter(String setter) {
		if (!isSetter(setter)) {
			return null;
		}
		return Character.toLowerCase(setter.charAt(GETTER_PREFIX.length()))
				+ setter.substring(1 + GETTER_PREFIX.length());
	}

	private static String getNormalGetterNameFromProperty(String property) {
		return new StringBuilder().append(GETTER_PREFIX)
				.append(Character.toUpperCase(property.charAt(0))).append(property.substring(1))
				.toString();
	}

	private static String getBooleanGetterNameFromProperty(String property) {
		return new StringBuilder().append(BOOLEAN_GETTER_PREFIX)
				.append(Character.toUpperCase(property.charAt(0))).append(property.substring(1))
				.toString();
	}

	private static String getSetterNameFromProperty(String property) {
		return new StringBuilder().append(SETTER_PREFIX)
				.append(Character.toUpperCase(property.charAt(0))).append(property.substring(1))
				.toString();
	}

	private static boolean isValidGetterSetter(IMethodBinding getter, IMethodBinding setter) {
		if (!isValidPropertyModifier(getter.getModifiers())
				|| !isValidPropertyModifier(setter.getModifiers())) {
			return false;
		}
		if (getter.getName().startsWith(BOOLEAN_GETTER_PREFIX)) {
			ITypeBinding binding = getter.getReturnType();
			if (!binding.getQualifiedName().equals(boolean.class.getName())
					&& !binding.getQualifiedName().equals(Boolean.class.getName())) {
				return false;
			}
		}
		if (!setter.getReturnType().getQualifiedName().equals(void.class.getName())) {
			return false;
		}
		ITypeBinding[] setterParams = setter.getParameterTypes();
		if (setterParams.length != 1) {
			return false;
		}
		if (getter.getParameterTypes().length != 0) {
			return false;
		}
		String getterReturnType = getter.getReturnType().getBinaryName();
		String setterParamType = setter.getParameterTypes()[0].getBinaryName();
		if (getterReturnType == null || setterParamType == null
				|| !getterReturnType.equals(setterParamType)) {
			return false;
		}
		return true;
	}

	public static Set<String> getProperties(TypeDeclaration td) {
		Set<String> properties = new HashSet<String>();
		ITypeBinding binding = td.resolveBinding();

		while (binding != null) {

			IVariableBinding[] vbs = binding.getDeclaredFields();
			for (IVariableBinding vb : vbs) {
				if (isValidPropertyModifier(vb.getModifiers())) {
					properties.add(vb.getName());
				}
			}

			IMethodBinding[] mbs = binding.getDeclaredMethods();
			Map<String, IMethodBinding> nameToMethod = new HashMap<String, IMethodBinding>();
			for (IMethodBinding mb : mbs) {
				nameToMethod.put(mb.getName(), mb);
			}

			for (IMethodBinding mb : mbs) {
				IMethodBinding getter = null, setter = null;
				String property = null;
				if (isGetter(mb.getName())) {
					getter = mb;
					property = getPropertyNameFromGetter(mb.getName());
					setter = nameToMethod.remove(getSetterNameFromProperty(property));
				} else if (isSetter(mb.getName())) {
					setter = mb;
					property = getPropertyNameFromSetter(mb.getName());
					getter = nameToMethod.remove(getNormalGetterNameFromProperty(property));
					if (getter == null) {
						getter = nameToMethod.remove(getBooleanGetterNameFromProperty(property));
					}
				}
				if (getter != null && setter != null && isValidGetterSetter(getter, setter)) {
					properties.add(property);
				}
			}

			binding = binding.getSuperclass();
		}

		return properties;
	}

	public static IPackageFragment getDaoPackageFromEntity(IType entity) {
		if (!entity.exists()) {
			return null;
		}
		IPackageFragment entityPf = entity.getPackageFragment();
		try {
			if (entityPf.getKind() != IPackageFragmentRoot.K_SOURCE) {
				return null;
			}
		} catch (JavaModelException e) {
			LoggerUtil.error(e);
			return null;
		}
		IPackageFragmentRoot root = (IPackageFragmentRoot) entityPf
				.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);

		String entityPackage = entityPf.getElementName();
		String daoPackage = null;
		if (entityPackage.isEmpty()) {
			daoPackage = "dao";
		} else if (!entityPackage.endsWith("entity")) {
			daoPackage = entityPackage + ".dao";
		} else {
			daoPackage = entityPackage.substring(0, entityPackage.lastIndexOf("entity")) + "dao";
		}
		IPackageFragment daoPf = root.getPackageFragment(daoPackage);
		return daoPf;
	}

	public static ICompilationUnit getDaoFromEntity(IType entity) {
		IPackageFragment pf = getDaoPackageFromEntity(entity);
		if (pf == null) {
			return null;
		}
		ICompilationUnit cu = pf.getCompilationUnit(entity.getElementName() + "Dao.java");
		return cu.exists() ? cu : null;
	}

	public static boolean isWebPageUnderViewRoot(IFile file) {
		if (!file.exists()) {
			return false;
		}
		String extension = file.getFileExtension();
		if (extension == null || !extension.equals("html")) {
			return false;
		}
		IProject project = file.getProject();
		ProjectPreferences prefs = ProjectPreferences.getPreference(project);
		String viewRoot = prefs.getViewRoot();
		if (viewRoot == null || viewRoot.isEmpty()) {
			return false;
		}
		return file.getProjectRelativePath().toString().startsWith(viewRoot);
	}

	public static String getPageClassELExpression(IFile file) {
		if (!file.exists()) {
			return null;
		}

		IProject project = file.getProject();
		ProjectPreferences prefs = ProjectPreferences.getPreference(project);
		String viewRoot = prefs.getViewRoot();
		if (viewRoot == null || viewRoot.isEmpty()) {
			return null;
		}

		StringBuilder relativePath = new StringBuilder(file.getProjectRelativePath().toString()
				.substring(viewRoot.length() + 1));
		relativePath.replace(relativePath.lastIndexOf(".html"), relativePath.length(), "Page");

		int i = relativePath.lastIndexOf("/") + 1;
		relativePath.setCharAt(i, Character.toLowerCase(relativePath.charAt(i)));

		return relativePath.toString().replace('/', '_');
	}

	private static boolean isNoneCommonDaoMethod(IMethod method) {
		if (!method.exists()) {
			return false;
		}
		String name = method.getElementName();
		String[] commonMethods = { DaoConstants.METHOD_DELETEL, DaoConstants.METHOD_LOADBYID,
				DaoConstants.METHOD_SAVE, DaoConstants.METHOD_UPDATE,
				DaoConstants.METHOD_SAVEORUPDATE };
		Set<String> commonMethodSet = new HashSet<String>(Arrays.asList(commonMethods));
		return !commonMethodSet.contains(name);
	}

	private static boolean isDaoArgs(IField field) {
		String name = field.getElementName();
		return field.exists() && name.length() > DaoConstants.ARGS_SUFFIX.length()
				&& name.endsWith(DaoConstants.ARGS_SUFFIX);
	}

	private static boolean isDaoHql(IField field) {
		String name = field.getElementName();
		return field.exists() && name.length() > DaoConstants.HQL_SUFFIX.length()
				&& name.endsWith(DaoConstants.HQL_SUFFIX);
	}

	public static int getDaoMemberType(IMember element) {
		if (!element.exists()) {
			return DaoConstants.ELEMENT_TYPE_UNKOWN;
		}
		if (element instanceof IMethod && isNoneCommonDaoMethod((IMethod) element)) {
			return DaoConstants.ELEMENT_TYPE_METHOD;
		}
		if (element instanceof IField && isDaoArgs((IField) element)) {
			return DaoConstants.ELEMENT_TYPE_ARGS;
		}
		if (element instanceof IField && isDaoHql((IField) element)) {
			return DaoConstants.ELEMENT_TYPE_HQL;
		}
		return DaoConstants.ELEMENT_TYPE_UNKOWN;
	}

	public static String getDaoGroupedMethodName(IMember member) {
		int type = getDaoMemberType(member);
		String name = member.getElementName();
		switch (type) {
		case DaoConstants.ELEMENT_TYPE_ARGS:
			return name.substring(0, name.length() - DaoConstants.ARGS_SUFFIX.length());
		case DaoConstants.ELEMENT_TYPE_HQL:
			return name.substring(0, name.length() - DaoConstants.HQL_SUFFIX.length());
		case DaoConstants.ELEMENT_TYPE_METHOD:
			return name;
		default:
			return null;
		}
	}

	public static IMember getDaoGroupedMember(IMember member, int type) {
		String methodName = getDaoGroupedMethodName(member);
		if (methodName == null) {
			return null;
		}
		IType outerType = member.getDeclaringType();
		if (outerType == null) {
			return null;
		}

		IField field;
		switch (type) {
		case DaoConstants.ELEMENT_TYPE_ARGS:
			field = outerType.getField(methodName + "_ARGS");
			return field.exists() ? field : null;
		case DaoConstants.ELEMENT_TYPE_HQL:
			field = outerType.getField(methodName + "_HQL");
			return field.exists() ? field : null;
		case DaoConstants.ELEMENT_TYPE_METHOD:
			try {
				for (IMethod method : outerType.getMethods()) {
					if (method.getElementName().equals(methodName)) {
						return method;
					}
				}
			} catch (JavaModelException e) {
				LoggerUtil.error(e);
				return null;
			}
		default:
			return null;
		}
	}
}
