package cntoplolicon.seasar2assist.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.actions.OpenNewInterfaceWizardAction;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;

import cntoplolicon.seasar2assist.job.GenerateCommonDaoStuffJob;
import cntoplolicon.seasar2assist.preference.ProjectPreferences;
import cntoplolicon.seasar2assist.util.JavaModelUtil;
import cntoplolicon.seasar2assist.util.LoggerUtil;
import cntoplolicon.seasar2assist.util.NamingConventionUtil;

public class SwitchToDaoHandler extends AbstractHandler {

	private IType getCurrentEntity() {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		if (activeEditor == null) {
			return null;
		}

		IFile file = (IFile) ResourceUtil.getFile(activeEditor.getEditorInput());
		if (file == null) {
			return null;
		}
		IJavaElement element = JavaCore.create(file);
		if (!(element instanceof ICompilationUnit)) {
			return null;
		}
		ICompilationUnit cu = (ICompilationUnit) element;
		IType entityType = null;
		try {
			for (IType candiate : cu.getAllTypes()) {
				if (NamingConventionUtil.isEntity(candiate)) {
					entityType = candiate;
					break;
				}
			}
		} catch (JavaModelException e) {
			LoggerUtil.error(e);
			return null;
		}
		if (entityType == null) {
			return null;
		}
		return entityType;
	}

	private void initNewInterfacePage(NewInterfaceWizardPage page, IType entityType) {
		IPackageFragment entityPf = entityType.getPackageFragment();

		IPackageFragmentRoot root = JavaModelUtil.getPackageFragmentRoot(entityPf);
		if (root == null) {
			return;
		}
		page.setPackageFragmentRoot(root, true);

		IPackageFragment daoPf = NamingConventionUtil.getDaoPackageFromEntity(entityType);
		if (daoPf != null) {
			page.setPackageFragment(daoPf, true);
		}

		page.setEnclosingType(null, false);
		page.setTypeName(entityType.getElementName() + "Dao", true);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IType entityType = getCurrentEntity();
		if (entityType == null) {
			return null;
		}

		ProjectPreferences prefs = ProjectPreferences.getPreference(entityType.getJavaProject()
				.getProject());
		if (!prefs.isUseSeasar2Assistant()) {
			return null;
		}

		ICompilationUnit daoCu = NamingConventionUtil.getDaoFromEntity(entityType);
		if (daoCu != null) {
			try {
				IDE.openEditor(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
						(IFile) daoCu.getUnderlyingResource());
			} catch (PartInitException e) {
				LoggerUtil.error(e);
			} catch (JavaModelException e) {
				LoggerUtil.error(e);
			}
			return null;
		}

		NewInterfaceWizardPage page = new NewInterfaceWizardPage();
		initNewInterfacePage(page, entityType);
		OpenNewInterfaceWizardAction action = new OpenNewInterfaceWizardAction();
		action.setConfiguredWizardPage(page);
		action.run();
		if (action.getCreatedElement() == null || !(action.getCreatedElement() instanceof IType)) {
			return null;
		}
		IType daoType = (IType) action.getCreatedElement();
		new GenerateCommonDaoStuffJob(daoType, entityType).schedule();

		return null;
	}

}
