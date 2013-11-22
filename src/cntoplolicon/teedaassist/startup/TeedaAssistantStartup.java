package cntoplolicon.teedaassist.startup;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IStartup;

import cntoplolicon.teedaassist.checker.ScopeStringChecker;

public class TeedaAssistantStartup implements IStartup {

	@Override
	public void earlyStartup() {
		JavaCore.addElementChangedListener(new ScopeStringChecker());
	}
	
}
