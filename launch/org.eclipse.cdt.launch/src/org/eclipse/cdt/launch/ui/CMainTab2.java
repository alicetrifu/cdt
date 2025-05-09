/*******************************************************************************
 * Copyright (c) 2008, 2016 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *     Ken Ryall (Nokia) - bug 178731
 *     Ericsson - Support for tracepoint post-mortem debugging
 *     IBM Corporation
 *     Marc Khouzam (Ericsson) - Support setting the path in which the core file
 *                               dialog should start (Bug 362039)
 *     Anton Gorenkov
 *     Marc Khouzam (Ericsson) - Move from dsf.gdb to cdt.launch
 *******************************************************************************/
package org.eclipse.cdt.launch.ui;

import java.io.File;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.launch.LaunchUtils;
import org.eclipse.cdt.launch.internal.ui.LaunchImages;
import org.eclipse.cdt.launch.internal.ui.LaunchMessages;
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.ui.CElementLabelProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

/**
 * A launch configuration tab that displays and edits project and main type name launch
 * configuration attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed.
 * </p>
 * @since 7.3
 */

public class CMainTab2 extends CAbstractMainTab {

	/**
	 * Tab identifier used for ordering of tabs added using the
	 * <code>org.eclipse.debug.ui.launchConfigurationTabs</code>
	 * extension point.
	 */
	// Keep the same id as the original Run launch main tab for backwards-compatibility
	public static final String TAB_ID = "org.eclipse.cdt.cdi.launch.mainTab"; //$NON-NLS-1$

	private static final String CORE_FILE = LaunchMessages.CMainTab2_CoreFile_type;
	private static final String TRACE_FILE = LaunchMessages.CMainTab2_TraceFile_type;

	/**
	 * Combo box to select which type of post mortem file should be used.
	 * We currently support core files and trace files.
	 */
	protected Combo fCoreTypeCombo;

	private boolean fDontCheckProgram = false;
	private final boolean fSpecifyCoreFile;
	private final boolean fIncludeBuildSettings;

	public static final int DONT_CHECK_PROGRAM = 2;
	public static final int SPECIFY_CORE_FILE = 4;
	public static final int INCLUDE_BUILD_SETTINGS = 8;

	public CMainTab2() {
		this(INCLUDE_BUILD_SETTINGS);
	}

	public CMainTab2(int flags) {
		fDontCheckProgram = (flags & DONT_CHECK_PROGRAM) != 0;
		fSpecifyCoreFile = (flags & SPECIFY_CORE_FILE) != 0;
		fIncludeBuildSettings = (flags & INCLUDE_BUILD_SETTINGS) != 0;
	}

	/**
	 * @since 11.0
	 */
	protected void setDontCheckProgram(boolean dontCheck) {
		fDontCheckProgram = dontCheck;
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				ICDTLaunchHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB);

		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);

		createVerticalSpacer(comp, 1);
		createProjectGroup(comp, 1);
		createExeFileGroup(comp, 1);

		if (fIncludeBuildSettings) {
			createBuildOptionGroup(comp, 1);
		}
		createVerticalSpacer(comp, 1);
		if (fSpecifyCoreFile) {
			createCoreFileGroup(comp, 1);
		}

		LaunchUIPlugin.setDialogShell(parent.getShell());
	}

	protected void createExeFileGroup(Composite parent, int colSpan) {
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		mainComp.setLayoutData(gd);
		fProgLabel = new Label(mainComp, SWT.NONE);
		fProgLabel.setText(LaunchMessages.CMainTab_C_Application);
		gd = new GridData();
		fProgLabel.setLayoutData(gd);
		fProgText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProgText.setLayoutData(gd);
		fProgText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		Composite buttonComp = new Composite(mainComp, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonComp.setLayout(layout);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComp.setLayoutData(gd);
		buttonComp.setFont(parent.getFont());

		createVariablesButton(buttonComp, LaunchMessages.CMainTab_Variables, fProgText);
		fSearchButton = createPushButton(buttonComp, LaunchMessages.CMainTab_Search, null);
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});

		Button browseForBinaryButton;
		browseForBinaryButton = createPushButton(buttonComp, LaunchMessages.Launch_common_Browse_2, null);
		browseForBinaryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				String text = handleBrowseButtonSelected(LaunchMessages.CMainTab2_Application_Selection);
				if (text != null) {
					fProgText.setText(text);
				}
				updateLaunchConfigurationDialog();
			}
		});
	}

	/*
	 * Overridden to add the possibility to choose a trace file as a post mortem debug file.
	 */
	@Override
	protected void createCoreFileGroup(Composite parent, int colSpan) {
		Composite coreComp = new Composite(parent, SWT.NONE);
		GridLayout coreLayout = new GridLayout();
		coreLayout.numColumns = 3;
		coreLayout.marginHeight = 0;
		coreLayout.marginWidth = 0;
		coreComp.setLayout(coreLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		coreComp.setLayoutData(gd);

		Label comboLabel = new Label(coreComp, SWT.NONE);
		comboLabel.setText(LaunchMessages.CMainTab2_Post_mortem_file_type);
		comboLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		fCoreTypeCombo = new Combo(coreComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		fCoreTypeCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		fCoreTypeCombo.add(CORE_FILE);
		fCoreTypeCombo.add(TRACE_FILE);

		fCoreLabel = new Label(coreComp, SWT.NONE);
		fCoreLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		fCoreText = new Text(coreComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fCoreText.setLayoutData(gd);
		fCoreText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		Button browseForCoreButton;
		browseForCoreButton = createPushButton(coreComp, LaunchMessages.Launch_common_Browse_3, null);
		browseForCoreButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				String text;
				String coreType = getSelectedCoreType();
				if (coreType.equals(ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_CORE_FILE)) {
					text = handleBrowseButtonSelected(LaunchMessages.CMainTab2_Core_Selection);
				} else if (coreType.equals(ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_TRACE_FILE)) {
					text = handleBrowseButtonSelected(LaunchMessages.CMainTab2_Trace_Selection);
				} else {
					assert false : "Unknown core file type"; //$NON-NLS-1$
					text = handleBrowseButtonSelected(LaunchMessages.CMainTab2_Core_Selection);
				}

				if (text != null) {
					fCoreText.setText(text);
				}
				updateLaunchConfigurationDialog();
			}
		});

		fCoreTypeCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateCoreFileLabel();
				updateLaunchConfigurationDialog();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		fCoreTypeCombo.select(0);
	}

	/**
	 * Show a dialog that lets the user select a file.
	 * This method allows to set the title of the dialog.
	 *
	 * @param title The title the dialog should show.
	 */
	protected String handleBrowseButtonSelected(String title) {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
		fileDialog.setText(title);
		fileDialog.setFileName(fProgText.getText());
		return fileDialog.open();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		filterPlatform = getPlatform(config);
		updateProjectFromConfig(config);
		updateProgramFromConfig(config);
		updateCoreFromConfig(config);
		updateBuildOptionFromConfig(config);
	}

	protected void updateCoreFromConfig(ILaunchConfiguration config) {
		if (fCoreText != null) {
			String coreName = EMPTY_STRING;
			String coreType = ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_TYPE_DEFAULT;
			try {
				coreName = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_COREFILE_PATH, EMPTY_STRING);
				coreType = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_POST_MORTEM_TYPE,
						ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_TYPE_DEFAULT);
			} catch (CoreException ce) {
				LaunchUIPlugin.log(ce);
			}
			fCoreText.setText(coreName);
			if (coreType.equals(ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_CORE_FILE)) {
				fCoreTypeCombo.setText(CORE_FILE);
			} else if (coreType.equals(ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_TRACE_FILE)) {
				fCoreTypeCombo.setText(TRACE_FILE);
			} else {
				assert false : "Unknown core file type"; //$NON-NLS-1$
				fCoreTypeCombo.setText(CORE_FILE);
			}
			updateCoreFileLabel();
		}
	}

	protected String getSelectedCoreType() {
		int selectedIndex = fCoreTypeCombo.getSelectionIndex();
		if (fCoreTypeCombo.getItem(selectedIndex).equals(CORE_FILE)) {
			return ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_CORE_FILE;
		} else if (fCoreTypeCombo.getItem(selectedIndex).equals(TRACE_FILE)) {
			return ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_TRACE_FILE;
		} else {
			assert false : "Unknown post mortem file type"; //$NON-NLS-1$
			return ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_CORE_FILE;
		}
	}

	protected void updateCoreFileLabel() {
		String coreType = getSelectedCoreType();
		if (coreType.equals(ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_CORE_FILE)) {
			fCoreLabel.setText(LaunchMessages.CMainTab2_CoreFile_path);
		} else if (coreType.equals(ICDTLaunchConfigurationConstants.DEBUGGER_POST_MORTEM_TRACE_FILE)) {
			fCoreLabel.setText(LaunchMessages.CMainTab2_TraceFile_path);
		} else {
			assert false : "Unknown post mortem file type"; //$NON-NLS-1$
			fCoreLabel.setText(LaunchMessages.CMainTab2_CoreFile_path);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		super.performApply(config);
		ICProject cProject = this.getCProject();
		if (cProject != null && cProject.exists()) {
			config.setMappedResources(new IResource[] { cProject.getProject() });
		} else {
			config.setMappedResources(null);
		}

		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText());
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, fProgText.getText());
		if (fCoreText != null) {
			config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_COREFILE_PATH, fCoreText.getText());
			config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_POST_MORTEM_TYPE, getSelectedCoreType());
		}
	}

	/**
	 * Show a dialog that lists all main types
	 */
	@Override
	protected void handleSearchButtonSelected() {
		if (getCProject() == null) {
			MessageDialog.openInformation(getShell(), LaunchMessages.CMainTab_Project_required,
					LaunchMessages.CMainTab_Enter_project_before_searching_for_program);

			return;
		}

		ILabelProvider programLabelProvider = new CElementLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IBinary) {
					IBinary bin = (IBinary) element;
					StringBuilder name = new StringBuilder();
					name.append(bin.getPath().lastSegment());
					return name.toString();
				}
				return super.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				if (!(element instanceof ICElement)) {
					return super.getImage(element);
				}
				ICElement celement = (ICElement) element;

				if (celement.getElementType() == ICElement.C_BINARY) {
					IBinary belement = (IBinary) celement;
					if (belement.isExecutable()) {
						return DebugUITools.getImage(IDebugUIConstants.IMG_ACT_RUN);
					}
				}

				return super.getImage(element);
			}
		};

		ILabelProvider qualifierLabelProvider = new CElementLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IBinary) {
					IBinary bin = (IBinary) element;
					StringBuilder name = new StringBuilder();
					name.append(bin.getCPU()).append(bin.isLittleEndian() ? "le" : "be"); //$NON-NLS-1$ //$NON-NLS-2$
					name.append(" - "); //$NON-NLS-1$
					name.append(bin.getPath().toString());
					return name.toString();
				}
				return super.getText(element);
			}
		};

		TwoPaneElementSelector dialog = new TwoPaneElementSelector(getShell(), programLabelProvider,
				qualifierLabelProvider);
		dialog.setElements(getBinaryFiles(getCProject()));
		dialog.setMessage(LaunchMessages.CMainTab_Choose_program_to_run);
		dialog.setTitle(LaunchMessages.CMainTab_Program_Selection);
		dialog.setUpperListLabel(LaunchMessages.Launch_common_BinariesColon);
		dialog.setLowerListLabel(LaunchMessages.Launch_common_QualifierColon);
		dialog.setMultipleSelection(false);
		// dialog.set
		if (dialog.open() == Window.OK) {
			IBinary binary = (IBinary) dialog.getFirstResult();
			fProgText.setText(binary.getResource().getProjectRelativePath().toString());
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);

		if (!fDontCheckProgram) {
			String programName = fProgText.getText().trim();
			try {
				programName = VariablesPlugin.getDefault().getStringVariableManager()
						.performStringSubstitution(programName);
			} catch (CoreException e) {
				// Silently ignore substitution failure (for consistency with "Arguments" and "Work directory" fields)
			}
			if (programName.length() == 0) {
				setErrorMessage(LaunchMessages.CMainTab_Program_not_specified);
				return false;
			}
			if (programName.equals(".") || programName.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
				setErrorMessage(LaunchMessages.CMainTab_Program_does_not_exist);
				return false;
			}
			IPath exePath = new Path(programName);
			if (exePath.isAbsolute()) {
				// For absolute paths, we don't need a project, we can debug or run the binary directly
				// as long as it exists
				File executable = exePath.toFile();
				if (!executable.exists()) {
					setErrorMessage(LaunchMessages.CMainTab_Program_does_not_exist);
					return false;
				}
				if (!executable.isFile()) {
					setErrorMessage(LaunchMessages.CMainTab_Selection_must_be_file);
					return false;
				}
			} else {
				// For relative paths, we need a proper project
				String projectName = fProjText.getText().trim();
				if (projectName.length() == 0) {
					setErrorMessage(LaunchMessages.CMainTab_Project_not_specified);
					return false;
				}
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (!project.exists()) {
					setErrorMessage(LaunchMessages.Launch_common_Project_does_not_exist);
					return false;
				}
				if (!project.isOpen()) {
					setErrorMessage(LaunchMessages.CMainTab_Project_must_be_opened);
					return false;
				}
				exePath = LaunchUtils.toAbsoluteProgramPath(project, exePath);
				File executable = exePath.toFile();
				if (!executable.exists()) {
					setErrorMessage(LaunchMessages.CMainTab_Program_does_not_exist);
					return false;
				}
				if (!executable.isFile()) {
					setErrorMessage(LaunchMessages.CMainTab_Selection_must_be_file);
					return false;
				}
			}
			// Notice that we don't check if exePath points to a valid executable since such
			// check is too expensive to be done on the UI thread.
			// See "https://bugs.eclipse.org/bugs/show_bug.cgi?id=328012".
		}

		if (fCoreText != null) {
			String coreName = fCoreText.getText().trim();
			// We accept an empty string.  This should trigger a prompt to the user
			// This allows to re-use the launch, with a different core file.
			// We also accept an absolute or workspace-relative path, including variables.
			// This allows the user to indicate in which directory the prompt will start (Bug 362039)
			if (!coreName.equals(EMPTY_STRING)) {
				try {
					// Replace the variables
					coreName = VariablesPlugin.getDefault().getStringVariableManager()
							.performStringSubstitution(coreName, false);
				} catch (CoreException e) {
					setErrorMessage(e.getMessage());
					return false;
				}

				coreName = coreName.trim();
				File filePath = new File(coreName);
				if (!filePath.isDirectory() && !filePath.exists()) {
					setErrorMessage(LaunchMessages.CMainTab2_File_does_not_exist);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		// We set empty attributes for project & program so that when one config is
		// compared to another, the existence of empty attributes doesn't cause and
		// incorrect result (the performApply() method can result in empty values
		// for these attributes being set on a config if there is nothing in the
		// corresponding text boxes)
		// plus getContext will use this to base context from if set.
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, EMPTY_STRING);
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_COREFILE_PATH, EMPTY_STRING);

		// Set the auto choose build configuration to true for new configurations.
		// Existing configurations created before this setting was introduced will have this disabled.
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_AUTO, true);

		ICElement cElement = null;
		cElement = getContext(config, getPlatform(config));
		if (cElement != null) {
			initializeCProject(cElement, config);
			initializeProgramName(cElement, config);
		} else {
			// don't want to remember the interim value from before
			config.setMappedResources(null);
		}
	}

	/**
	 * Set the program name attributes on the working copy based on the ICElement
	 */
	protected void initializeProgramName(ICElement cElement, ILaunchConfigurationWorkingCopy config) {
		boolean renamed = false;

		if (!(cElement instanceof IBinary)) {
			cElement = cElement.getCProject();
		}

		if (cElement instanceof ICProject) {
			IProject project = cElement.getCProject().getProject();
			String name = project.getName();
			ICProjectDescription projDes = CCorePlugin.getDefault().getProjectDescription(project);
			if (projDes != null) {
				String buildConfigName = projDes.getActiveConfiguration().getName();
				name = name + " " + buildConfigName; //$NON-NLS-1$
			}
			name = getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
			renamed = true;
		}

		IBinary binary = null;
		if (cElement instanceof ICProject) {
			IBinary[] bins = getBinaryFiles((ICProject) cElement);
			if (bins != null && bins.length == 1) {
				binary = bins[0];
			}
		} else if (cElement instanceof IBinary) {
			binary = (IBinary) cElement;
		}

		if (binary != null) {
			String path;
			path = binary.getResource().getProjectRelativePath().toOSString();
			config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, path);
			if (!renamed) {
				String name = binary.getElementName();
				int index = name.lastIndexOf('.');
				if (index > 0) {
					name = name.substring(0, index);
				}
				name = getLaunchConfigurationDialog().generateName(name);
				config.rename(name);
				renamed = true;
			}
		}

		if (!renamed) {
			String name = getLaunchConfigurationDialog().generateName(cElement.getCProject().getElementName());
			config.rename(name);
		}
	}

	@Override
	public String getId() {
		return TAB_ID;
	}

	@Override
	public String getName() {
		return LaunchMessages.CMainTab_Main;
	}

	@Override
	public Image getImage() {
		return LaunchImages.get(LaunchImages.IMG_VIEW_MAIN_TAB);
	}
}
