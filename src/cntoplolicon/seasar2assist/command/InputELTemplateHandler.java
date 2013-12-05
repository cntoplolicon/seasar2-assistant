package cntoplolicon.seasar2assist.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import cntoplolicon.seasar2assist.preference.ProjectPreferences;
import cntoplolicon.seasar2assist.util.LoggerUtil;
import cntoplolicon.seasar2assist.util.NamingConventionUtil;

public class InputELTemplateHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		if (activeEditor == null) {
			return null;
		}

		IFile file = (IFile) ResourceUtil.getFile(activeEditor.getEditorInput());
		if (file == null || !NamingConventionUtil.isWebPageUnderViewRoot(file)) {
			return null;
		}

		ProjectPreferences prefs = ProjectPreferences.getPreference(file.getProject());
		if (!prefs.isUseSeasar2Assistant()) {
			return null;
		}

		ITextEditor editor = (ITextEditor) activeEditor;
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		String elExp = NamingConventionUtil.getPageClassELExpression(file);
		if (elExp == null) {
			LoggerUtil.error("failed generating el expression for html " + file.getName());
			return null;
		}
		String insertion = elExp + ".";
		try {
			new ReplaceEdit(selection.getOffset(), selection.getLength(), insertion).apply(doc);
			editor.selectAndReveal(selection.getOffset() + insertion.length(), 0);
		} catch (MalformedTreeException e) {
			LoggerUtil.error(e);
		} catch (BadLocationException e) {
			LoggerUtil.error(e);
		}
		return null;
	}
}
