package cntoplolicon.seasar2assist.preference;

import java.lang.reflect.Field;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import cntoplolicon.seasar2assist.Seasar2AssistantPlugin;
import cntoplolicon.seasar2assist.util.LoggerUtil;

public class ProjectPreferences {

	private IProject project;

	boolean useSeasar2Assistant;
	boolean checkScopeStrings;
	boolean generateCommonDaoMethods;
	String rootPackage;
	String viewRoot;

	public ProjectPreferences(IProject project) {
		this.project = project;
		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope.getNode(Seasar2AssistantPlugin.PLUGIN_ID);
		for (Field field : this.getClass().getDeclaredFields()) {
			if (field.getName().equals("prject")) {
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

	public boolean flush() {
		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope.getNode(Seasar2AssistantPlugin.PLUGIN_ID);
		for (Field field : this.getClass().getDeclaredFields()) {
			if (field.getName().equals("prject")) {
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
