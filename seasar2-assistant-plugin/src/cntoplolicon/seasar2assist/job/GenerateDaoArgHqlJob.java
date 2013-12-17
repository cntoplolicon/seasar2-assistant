package cntoplolicon.seasar2assist.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import cntoplolicon.seasar2assist.constants.DaoConstants;
import cntoplolicon.seasar2assist.util.LoggerUtil;
import cntoplolicon.seasar2assist.util.NamingConventionUtil;

public class GenerateDaoArgHqlJob extends Job {

	private IMember member;

	public GenerateDaoArgHqlJob(IMember member) {
		super("generate args or hql for s2hibernate dao");
		this.member = member;
	}

	private String createArgsContent(IMethod method) throws JavaModelException {
		StringBuilder content = new StringBuilder();
		content.append("public static final String ");
		content.append(method.getElementName());
		content.append(DaoConstants.ARGS_SUFFIX);
		content.append(" = \"");
		String[] params = method.getParameterNames();
		for (int i = 0; i < params.length; i++) {
			content.append(params[i]);
			if (i != params.length - 1) {
				content.append(", ");
			}
		}
		content.append("\";");
		return content.toString();
	}

	private String createHqlContent(IMethod method) {
		StringBuilder content = new StringBuilder();
		content.append("public static final String ");
		content.append(method.getElementName());
		content.append(DaoConstants.HQL_SUFFIX);
		content.append(" = \"");
		content.append("\";");
		return content.toString();
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		int memberType = NamingConventionUtil.getDaoMemberType(member);
		if (memberType == DaoConstants.ELEMENT_TYPE_UNKOWN) {
			return Status.CANCEL_STATUS;
		}

		IMethod method = null;
		if (member.getElementType() == IJavaElement.METHOD) {
			method = (IMethod) member;
		} else {
			method = (IMethod) NamingConventionUtil.getDaoGroupedMember(member,
					DaoConstants.ELEMENT_TYPE_METHOD);
		}
		if (method == null) {
			return Status.CANCEL_STATUS;
		}
		try {
			IType type = member.getDeclaringType();
			IField argsField = (IField) NamingConventionUtil.getDaoGroupedMember(member,
					DaoConstants.ELEMENT_TYPE_ARGS);
			if (method.getParameterNames().length != 0 && argsField == null) {
				type.createField(createArgsContent(method), member, true, null);
			} else {
				IField hqlField = (IField) NamingConventionUtil.getDaoGroupedMember(member,
						DaoConstants.ELEMENT_TYPE_HQL);
				if (hqlField == null) {
					type.createField(createHqlContent(method), member, true, null);
				}
			}
			return Status.OK_STATUS;
		} catch (JavaModelException e) {
			LoggerUtil.error(e);
			return Status.CANCEL_STATUS;
		}
	}
}
