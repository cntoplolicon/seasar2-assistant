package cntoplolicon.seasar2assist.util;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
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

import cntoplolicon.seasar2assist.preference.ProjectPreferences;

public class NamingConventionUtil {

	private static final String PAGE_CLASS_SUFFIX = "Page";
	private static final String JAVA_EXTENSION = ".java";
	private static final String GETTER_PREFIX = "get";
	private static final String SETTER_PREFIX = "set";

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

	public static boolean isGetter(String methodName) {
		return methodName.startsWith(GETTER_PREFIX) && methodName.length() > GETTER_PREFIX.length()
				&& Character.isUpperCase(methodName.charAt(GETTER_PREFIX.length()));
	}

	public static boolean isSetter(String methodName) {
		return methodName.startsWith(SETTER_PREFIX) && methodName.length() > SETTER_PREFIX.length()
				&& Character.isUpperCase(methodName.charAt(GETTER_PREFIX.length()));
	}

	public static String getPropertyNameFromGetter(String getter) {
		if (!isGetter(getter)) {
			return null;
		}
		return Character.toLowerCase(getter.charAt(GETTER_PREFIX.length()))
				+ getter.substring(1 + GETTER_PREFIX.length());
	}

	public static String getPropertyNameFromSetter(String setter) {
		if (!isSetter(setter)) {
			return null;
		}
		return Character.toLowerCase(setter.charAt(GETTER_PREFIX.length()))
				+ setter.substring(1 + GETTER_PREFIX.length());
	}

	public static String getGetterNameFromProperty(String property) {
		return new StringBuilder().append(GETTER_PREFIX)
				.append(Character.toUpperCase(property.charAt(0))).append(property.substring(1))
				.toString();
	}

	public static String getSetterNameFromProperty(String property) {
		return new StringBuilder().append(SETTER_PREFIX)
				.append(Character.toUpperCase(property.charAt(0))).append(property.substring(1))
				.toString();
	}

	private static boolean isValidGetterSetter(IMethodBinding getter, IMethodBinding setter) {
		if (!isValidPropertyModifier(getter.getModifiers())
				|| !isValidPropertyModifier(setter.getModifiers())) {
			return false;
		}
		if (!setter.getReturnType().getQualifiedName().equals("void")) {
			return false;
		}
		ITypeBinding[] setterParams = setter.getParameterTypes();
		if (setterParams.length != 1) {
			return false;
		}
		if (getter.getParameterTypes().length != 0) {
			return false;
		}
		if (!getter.getReturnType().getQualifiedName().equals(setterParams[0].getQualifiedName())) {
			return false;
		}
		return true;
	}

	public static Set<String> getProperties(TypeDeclaration td) {
		Set<String> properties = new HashSet<String>();
		ITypeBinding binding = td.resolveBinding();

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
				getter = nameToMethod.remove(getGetterNameFromProperty(property));
			}
			if (getter != null && setter != null && isValidGetterSetter(getter, setter)) {
				properties.add(property);
			}
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
		IPackageFragmentRoot root = JavaModelUtil.getPackageFragmentRoot(entityPf);

		String entityPackage = entityPf == null ? "" : entityPf.getElementName();
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
}
