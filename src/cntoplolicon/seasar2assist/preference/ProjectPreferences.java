package cntoplolicon.seasar2assist.preference;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import cntoplolicon.seasar2assist.Seasar2AssistantPlugin;
import cntoplolicon.seasar2assist.util.LoggerUtil;

public class ProjectPreferences {

	private IEclipsePreferences projectNode;

	@ProjectProperty
	private boolean useSeasar2Assistant;
	@ProjectProperty
	private boolean checkScopeStrings;
	@ProjectProperty
	private boolean generateCommonDaoMethods;
	@ProjectProperty
	private String rootPackage;
	@ProjectProperty
	private String viewRoot;

	private static Map<IProject, ProjectPreferences> preferenceMap = Collections
			.synchronizedMap(new HashMap<IProject, ProjectPreferences>());

	public static ProjectPreferences getPreference(IProject project) {
		if (!preferenceMap.containsKey(project)) {
			preferenceMap.put(project, new ProjectPreferences(project));
		}
		return preferenceMap.get(project);
	}

	private ProjectPreferences(IProject project) {
		IScopeContext projectScope = new ProjectScope(project);
		projectNode = projectScope.getNode(Seasar2AssistantPlugin.PLUGIN_ID);
		for (Field field : this.getClass().getDeclaredFields()) {
			if (!field.isAnnotationPresent(ProjectProperty.class)) {
				continue;
			}
			try {
				if (field.getType() == int.class || field.getType() == Integer.class) {
					field.setInt(this, projectNode.getInt(field.getName(), 0));
				}
				if (field.getType() == boolean.class || field.getType() == Boolean.class) {
					field.setBoolean(this, projectNode.getBoolean(field.getName(), false));
				}
				if (field.getType() == long.class || field.getType() == Long.class) {
					field.setLong(this, projectNode.getLong(field.getName(), 0L));
				}
				if (field.getType() == String.class) {
					field.set(this, projectNode.get(field.getName(), ""));
				}
			} catch (IllegalAccessException e) {
				LoggerUtil.error(e);
				throw new IllegalStateException(e);
			}
		}
	}

	public boolean isUseSeasar2Assistant() {
		return useSeasar2Assistant;
	}

	public void setUseSeasar2Assistant(boolean useSeasar2Assistant) {
		this.useSeasar2Assistant = useSeasar2Assistant;
	}

	public boolean isCheckScopeStrings() {
		return checkScopeStrings;
	}

	public void setCheckScopeStrings(boolean checkScopeStrings) {
		this.checkScopeStrings = checkScopeStrings;
	}

	public boolean isGenerateCommonDaoMethods() {
		return generateCommonDaoMethods;
	}

	public void setGenerateCommonDaoMethods(boolean generateCommonDaoMethods) {
		this.generateCommonDaoMethods = generateCommonDaoMethods;
	}

	public String getRootPackage() {
		return rootPackage;
	}

	public void setRootPackage(String rootPackage) {
		this.rootPackage = rootPackage;
	}

	public String getViewRoot() {
		return viewRoot;
	}

	public void setViewRoot(String viewRoot) {
		this.viewRoot = viewRoot;
	}

	public boolean flush() {
		for (Field field : this.getClass().getDeclaredFields()) {
			if (!field.isAnnotationPresent(ProjectProperty.class)) {
				continue;
			}
			try {
				if (field.getType() == int.class || field.getType() == Integer.class) {
					projectNode.putInt(field.getName(), field.getInt(this));
				}
				if (field.getType() == boolean.class || field.getType() == Boolean.class) {
					projectNode.putBoolean(field.getName(), field.getBoolean(this));
				}
				if (field.getType() == long.class || field.getType() == Long.class) {
					projectNode.putLong(field.getName(), field.getLong(this));
				}
				if (field.getType() == String.class) {
					projectNode.put(field.getName(), (String) field.get(this));
					field.set(this, projectNode.get(field.getName(), ""));
				}
			} catch (IllegalAccessException e) {
				LoggerUtil.error(e);
				throw new IllegalStateException(e);
			}
		}
		try {
			projectNode.flush();
			return true;
		} catch (BackingStoreException e) {
			LoggerUtil.error(e);
			return false;
		}
	}
}
