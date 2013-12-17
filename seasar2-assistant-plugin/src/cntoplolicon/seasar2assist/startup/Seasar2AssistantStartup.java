package cntoplolicon.seasar2assist.startup;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IStartup;

import cntoplolicon.seasar2assist.checker.ScopeStringChecker;

public class Seasar2AssistantStartup implements IStartup {

	@Override
	public void earlyStartup() {
		JavaCore.addElementChangedListener(new ScopeStringChecker(),
				ElementChangedEvent.POST_RECONCILE | ElementChangedEvent.POST_CHANGE);
	}

}
