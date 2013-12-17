package cntoplolicon.seasar2assist.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

public final class JavaModelUtil {

	private JavaModelUtil() {
	}

	public static ICompilationUnit getCurrentCompilationUnit() {
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
		if (!(element instanceof ICompilationUnit) || !element.exists()) {
			return null;
		}
		return (ICompilationUnit) element;
	}

	public static IJavaElement getCurrnetElement() {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		ICompilationUnit cu = getCurrentCompilationUnit();
		if (cu == null) {
			return null;
		}
		ITextSelection selection = (ITextSelection) activeEditor.getEditorSite()
				.getSelectionProvider().getSelection();
		try {
			return cu.getElementAt(selection.getOffset());
		} catch (JavaModelException e) {
			LoggerUtil.error(e);
			return null;
		}
	}
}
