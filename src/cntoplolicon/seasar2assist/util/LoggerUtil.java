package cntoplolicon.seasar2assist.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import cntoplolicon.seasar2assist.Activator;

public class LoggerUtil {

	public static final void info(String message) {
		Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, message));
	}

	public static final void info(Throwable t) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, t.getMessage(), t));
	}

	public static final void info(String message, Throwable t) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, message, t));
	}

	public static final void warn(String message) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, message));
	}

	public static final void warn(Throwable t) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, t.getMessage(), t));
	}

	public static final void warn(String message, Throwable t) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, message, t));
	}

	public static final void error(String message) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message));
	}

	public static final void error(Throwable t) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, t.getMessage(), t));
	}

	public static final void error(String message, Throwable t) {
		Activator.getDefault().getLog()
				.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, t));
	}
}