package cntoplolicon.seasar2assist.preference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cntoplolicon.seasar2assist.util.LoggerUtil;

public class Seasar2AssistantPropertyPage extends PropertyPage {

	private static final int NUM_COLUMNS = 3;
	private static final String USE_SEASAR2_ASSISTANT = "use Seasar2 Assistant";
	private static final String CHECK_SCOPE_STRING = "check scope declerations of properties in page class";
	private static final String GENERATE_COMMON_DAO_METHOD = "generate common methods when creating s2hibernate dao";

	private static final String BROWSE_TEXT = "Browser...";
	private static final String ROOT_PACKAGE_TEXT = "Root Package: ";
	private static final String VIEW_ROOT_TEXT = "View Root: ";

	private static final GridData rowSpanData = createRowSpanGridData();

	private ProjectPreferences preferences;

	private Button useSeasar2Assistant;
	private Button rootPackageButton;
	private Button viewRootButton;
	private Combo rootPackage;
	private Text viewRoot;
	private Button checkScopeStrings;
	private Button generateCommonDaoMethods;

	private IProject getProject() {
		return (IProject) getElement();
	}

	private static GridData createRowSpanGridData() {
		GridData data = new GridData();
		data.horizontalSpan = NUM_COLUMNS;
		return data;
	}

	private void createEmptyRow(Composite composite) {
		for (int i = 0; i < NUM_COLUMNS; i++) {
			new Label(composite, SWT.NONE);
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = NUM_COLUMNS;
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		useSeasar2Assistant = new Button(composite, SWT.CHECK);
		useSeasar2Assistant.setText(USE_SEASAR2_ASSISTANT);
		useSeasar2Assistant.setLayoutData(rowSpanData);

		checkScopeStrings = new Button(composite, SWT.CHECK);
		checkScopeStrings.setText(CHECK_SCOPE_STRING);
		checkScopeStrings.setLayoutData(rowSpanData);

		generateCommonDaoMethods = new Button(composite, SWT.CHECK);
		generateCommonDaoMethods.setText(GENERATE_COMMON_DAO_METHOD);
		generateCommonDaoMethods.setLayoutData(rowSpanData);

		createEmptyRow(composite);

		Label rootPackageLabel = new Label(composite, SWT.NONE);
		rootPackageLabel.setText(ROOT_PACKAGE_TEXT);
		rootPackage = new Combo(composite, SWT.NONE);
		rootPackageButton = new Button(composite, SWT.PUSH);
		rootPackageButton.setText(BROWSE_TEXT);

		Label viewRootLabel = new Label(composite, SWT.NONE);
		viewRootLabel.setText(VIEW_ROOT_TEXT);
		viewRoot = new Text(composite, SWT.SINGLE | SWT.BORDER);
		viewRoot.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		viewRootButton = new Button(composite, SWT.PUSH);
		viewRootButton.setText(BROWSE_TEXT);

		loadRootPackages();
		loadStoredPreferences();

		return composite;
	}

	private void loadRootPackages() {
		IProject project = getProject();
		IFile file = project.getFile("src/main/resources/convention.dicon");
		if (!file.exists()) {
			return;
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document = dBuilder.parse(file.getContents());
			NodeList candiates = document.getElementsByTagName("initMethod");
			for (int i = 0; i < candiates.getLength(); i++) {
				Element element = (Element) candiates.item(i);
				if (!"addRootPackageName".equals(element.getAttribute("name"))) {
					continue;
				}
				if (element.getParentNode().getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element parent = (Element) element.getParentNode();
				if (!parent.getNodeName().equals("component")
						|| !"org.seasar.framework.convention.impl.NamingConventionImpl"
								.equals(parent.getAttribute("class"))) {
					continue;
				}
				NodeList arguments = element.getElementsByTagName("arg");
				if (arguments.getLength() != 1) {
					continue;
				}
				Element argument = (Element) arguments.item(0);
				if (argument.getParentNode() != element) {
					continue;
				}
				String content = argument.getTextContent().trim();
				if (content.startsWith("\"") && content.endsWith("\"")) {
					content = content.substring(1, content.length() - 1);
				}
				rootPackage.add(content);
			}
		} catch (Exception e) {
			LoggerUtil.error(e);
		}
	}

	private void loadStoredPreferences() {
		preferences = new ProjectPreferences(getProject());
		useSeasar2Assistant.setSelection(preferences.isUseSeasar2Assistant());
		checkScopeStrings.setSelection(preferences.isCheckScopeStrings());
		generateCommonDaoMethods.setSelection(preferences.isGenerateCommonDaoMethods());
		rootPackage.setText(preferences.getRootPackage());
		viewRoot.setText(preferences.getViewRoot());
	}

	@Override
	protected void performDefaults() {
		checkScopeStrings.setSelection(true);
		generateCommonDaoMethods.setSelection(true);
		rootPackage.select(0);
		viewRoot.setText("src/main/webapp/view");
	}

	@Override
	public boolean performOk() {
		preferences.setUseSeasar2Assistant(useSeasar2Assistant.getSelection());
		preferences.setCheckScopeStrings(checkScopeStrings.getSelection());
		preferences.setGenerateCommonDaoMethods(generateCommonDaoMethods.getSelection());
		preferences.setRootPackage(rootPackage.getText());
		preferences.setViewRoot(viewRoot.getText());
		return preferences.flush();
	}
}
