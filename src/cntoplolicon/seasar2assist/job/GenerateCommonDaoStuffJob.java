package cntoplolicon.seasar2assist.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import cntoplolicon.seasar2assist.preference.ProjectPreferences;
import cntoplolicon.seasar2assist.util.LoggerUtil;

public class GenerateCommonDaoStuffJob extends Job {

	private IType daoType;
	private IType entityType;

	public GenerateCommonDaoStuffJob(IType daoType, IType entityType) {
		super("generate common dao stuff");
		this.daoType = daoType;
		this.entityType = entityType;
	}

	private void importEntityClass(ICompilationUnit cu) throws JavaModelException {
		cu.createImport(entityType.getFullyQualifiedName(), null, null);
	}

	private boolean isGenericAvalable() {
		IJavaProject project = daoType.getJavaProject();
		return Float.valueOf(project.getOption(JavaCore.COMPILER_COMPLIANCE, true)) > 1.45;
	}

	private void createBeanClassField(IType daoWorkingCopy) throws JavaModelException {
		StringBuilder content = new StringBuilder();
		content.append("public static final ");
		content.append(isGenericAvalable() ? "Class<?> " : "Class ");
		content.append("BEAN = ");
		content.append(entityType.getElementName());
		content.append(".class;");
		daoWorkingCopy.createField(content.toString(), null, false, null);
	}

	private void generateCommonVoidMethod(IType daoWorkingCopy, String method)
			throws JavaModelException {
		StringBuilder content = new StringBuilder();
		content.append("public void ");
		content.append(method);
		content.append("(");
		content.append(entityType.getElementName());
		content.append(" entity);");
		daoWorkingCopy.createMethod(content.toString(), null, false, null);
	}

	private void createSaveMethod(IType daoWorkingCopy) throws JavaModelException {
		generateCommonVoidMethod(daoWorkingCopy, "save");
	}

	private void createUpdateMethod(IType daoWorkingCopy) throws JavaModelException {
		generateCommonVoidMethod(daoWorkingCopy, "update");
	}

	private void createSaveOrUpdateMethod(IType daoWorkingCopy) throws JavaModelException {
		generateCommonVoidMethod(daoWorkingCopy, "saveOrUpdate");
	}

	private void createDeleteMethod(IType daoWorkingCopy) throws JavaModelException {
		generateCommonVoidMethod(daoWorkingCopy, "delete");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ProjectPreferences prefs = ProjectPreferences.getPreference(daoType.getJavaProject()
				.getProject());
		ICompilationUnit originCu = daoType.getCompilationUnit();
		if (originCu == null || !originCu.exists()) {
			return Status.CANCEL_STATUS;
		}
		try {
			ICompilationUnit workingCopy = originCu.getWorkingCopy(null);
			IType daoWorkingCopy = workingCopy.getType(daoType.getElementName());
			importEntityClass(workingCopy);
			createBeanClassField(daoWorkingCopy);
			if (prefs.isGenerateCommonDaoMethods()) {
				createSaveMethod(daoWorkingCopy);
				createUpdateMethod(daoWorkingCopy);
				createSaveOrUpdateMethod(daoWorkingCopy);
				createDeleteMethod(daoWorkingCopy);
			}
			workingCopy.commitWorkingCopy(false, null);
			workingCopy.discardWorkingCopy();
			return Status.OK_STATUS;
		} catch (JavaModelException e) {
			LoggerUtil.error(e);
			return Status.CANCEL_STATUS;
		}
	}
}
