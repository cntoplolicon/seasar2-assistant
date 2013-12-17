package cntoplolicon.seasar2assist.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

import cntoplolicon.seasar2assist.constants.DaoConstants;
import cntoplolicon.seasar2assist.job.GenerateDaoArgHqlJob;
import cntoplolicon.seasar2assist.preference.ProjectPreferences;
import cntoplolicon.seasar2assist.util.JavaModelUtil;
import cntoplolicon.seasar2assist.util.NamingConventionUtil;

public class GenerateDaoArgHqlHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IJavaElement element = JavaModelUtil.getCurrnetElement();
		ProjectPreferences prefs = ProjectPreferences.getPreference(element.getJavaProject()
				.getProject());
		if (!prefs.isUseSeasar2Assistant()) {
			return null;
		}
		if (!(element instanceof IMember)) {
			return null;
		}
		IMember member = (IMember) element;
		IType type = (IType) member.getAncestor(IJavaElement.TYPE);
		if (type == null || !NamingConventionUtil.isDao(type)) {
			return null;
		}
		int memberType = NamingConventionUtil.getDaoMemberType(member);
		if (memberType != DaoConstants.ELEMENT_TYPE_UNKOWN) {
			new GenerateDaoArgHqlJob(member).schedule();
		}
		return null;
	}

}
