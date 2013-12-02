package cntoplolicon.seasar2assist.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import cntoplolicon.seasar2assist.Seasar2AssistantPlugin;

public class LoggerUtil {

	public static final void info(String message) {
		Seasar2AssistantPlugin.getDefault().getLog().log(new Status(IStatus.INFO, Seasar2AssistantPlugin.PLUGIN_ID, message));
	}

	public static final void info(Throwable t) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.INFO, Seasar2AssistantPlugin.PLUGIN_ID, t.getMessage(), t));
	}

	public static final void info(String message, Throwable t) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.INFO, Seasar2AssistantPlugin.PLUGIN_ID, message, t));
	}

	public static final void warn(String message) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.WARNING, Seasar2AssistantPlugin.PLUGIN_ID, message));
	}

	public static final void warn(Throwable t) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.WARNING, Seasar2AssistantPlugin.PLUGIN_ID, t.getMessage(), t));
	}

	public static final void warn(String message, Throwable t) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.WARNING, Seasar2AssistantPlugin.PLUGIN_ID, message, t));
	}

	public static final void error(String message) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.ERROR, Seasar2AssistantPlugin.PLUGIN_ID, message));
	}

	public static final void error(Throwable t) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.ERROR, Seasar2AssistantPlugin.PLUGIN_ID, t.getMessage(), t));
	}

	public static final void error(String message, Throwable t) {
		Seasar2AssistantPlugin.getDefault().getLog()
				.log(new Status(IStatus.ERROR, Seasar2AssistantPlugin.PLUGIN_ID, message, t));
	}
}