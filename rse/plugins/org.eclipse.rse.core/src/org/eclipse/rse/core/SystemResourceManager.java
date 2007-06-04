/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * Dave McKnight (IBM) - [177155] Move from rse.ui/systems/org.eclipse.rse.core
 * Martin Oberhuber (Wind River) - Re-add missing methods for user actions
 * David Dykstal (IBM) - [189858] delayed the creation of the remote systems project
 *                                removed unneeded first time logic and flags
 *                                renamed createRemoteSystemsProjectInternal to ensureRemoteSystemsProject
 *                                made ensureRemoteSystemsProject private instead of protected 
 ********************************************************************************/

package org.eclipse.rse.core;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemHostPool;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.internal.core.RemoteSystemsProject;
import org.eclipse.rse.internal.core.SystemResourceConstants;


/**
 * Static methods that manage the workbench resource tree for the remote systems project.
 * All code in the framework uses this to access the file system for save/restore purposes.
 * By limiting all access to one place, we simply changes should we decide to change the
 * underlying file system map.
 * <p>
 * <b>Assumptions</b>
 * <ul>
 *   <li>Each SystemConnectionPool object manages the connections for a given system profile
 *   <li>Each SystemFilterPoolManager object manages the filter pools for a given subsystem factory,
 *         for a given system profile!
 *   <li>Each SystemFilterPool object is an arbitrary named collection of filters all stored
 *         in one folder on disk with the same name as the pool.
 * </ul>
 * <p>
 */
public class SystemResourceManager implements SystemResourceConstants
{

	private static IProject remoteSystemsProject = null;
	private static IProject remoteSystemsTempFilesProject = null;
	private static SystemResourceHelpers helpers = null;

	private static ISystemResourceListener _listener = null;

	/**
     * Turn off event listening. Please call this before do anything that modifies resources and
     * turn it on again after.
     */
    public static void turnOffResourceEventListening()
    {
    	if (_listener != null)
    	  _listener.turnOffResourceEventListening();
    }

    /**
     * Turn off event listening. Please call this after modifying resources.
     */
    public static void turnOnResourceEventListening()
    {
    	if (_listener != null)
    	  _listener.turnOnResourceEventListening();
    }
    /**
     * Ensure event listening is on. Called at start of team synch action to be safe.
     */
    public static void ensureOnResourceEventListening()
    {
    	if (_listener != null)
    	  _listener.ensureOnResourceEventListening();
    }

    /**
     * Start event listening. Requests to turn on and off are ignored until this is called,
     *  which is at the appropriate point in the startup sequence.
     */
    public static void startResourceEventListening(ISystemResourceListener listener)
    {
	    _listener = listener;
	    listener.turnOnResourceEventListening();
	    
	    
    	IWorkspace ws = remoteSystemsProject.getWorkspace();
    	int eventMask = IResourceChangeEvent.POST_CHANGE;
    	
    	// add listener for global events ;
    	ws.addResourceChangeListener(listener, eventMask);    	
    }
    /**
     * End event listening. Requests to turn on and off are ignored after this is called,
     *  which is at the appropriate point in the shutdown sequence.
     */
    public static void endResourceEventListening()
    {
    	if (_listener != null)
    	{
    	  IWorkspace ws = remoteSystemsProject.getWorkspace();
    	  ws.removeResourceChangeListener(_listener);    	
    	  _listener = null;
    	}
    }

	/**
	 * Register a listener for resource change events on objects in our remote system project.
	 * No attempt is made to filter the events, they are simply passed on and the listener can
	 * decide if the event applies to them or not.
	 * <p>
	 * However, the event will only be fired if a change is made to that resource outside of the
	 * normal activities of the Remote Systems Framework, and only for resources within the
	 * Remote Systems Connection project.
	 */
	public static void addResourceChangeListener(IResourceChangeListener l)
	{
		if (_listener != null)
		  _listener.addResourceChangeListener(l);
	}
	/**
	 * Remove a listener for resource change events on an object in our remote system project.
	 */
	public static void removeResourceChangeListener(IResourceChangeListener l)
	{
		if (_listener != null)
		  _listener.removeResourceChangeListener(l);
	}
	
    /**
	 * Get the default remote systems project.
	 * If found but closed, this will open the project.
	 * @return IProject handle of the project. Use exists() to test existence.
	 */
	public static IProject getRemoteSystemsProject() 
	{
		return getRemoteSystemsProject(true);
	}

	/**
	 * Get the default remote systems project.
	 * If found but closed, this will open the project.
	 * @param force if true force the creation of the project if not found.
	 * In any case, returns handle to the project.
	 * @return IProject handle of the project. Use exists() to test existence.
	 */
	public static IProject getRemoteSystemsProject(boolean force) {
		if (remoteSystemsProject == null) {
			remoteSystemsProject = ResourcesPlugin.getWorkspace().getRoot().getProject(RESOURCE_PROJECT_NAME);
		}
		if ((!remoteSystemsProject.exists() && force) || (remoteSystemsProject.exists() && !remoteSystemsProject.isOpen())) {
			ensureRemoteSystemsProject(remoteSystemsProject);
		}
		return remoteSystemsProject;
	}

    /**
	 * Get the default remote systems temp files project.
	 * @return IProject handle of the project. Use exists() to test existence.
	 */
	public static IProject getRemoteSystemsTempFilesProject()
	{
		if (remoteSystemsTempFilesProject == null)
		{
		  remoteSystemsTempFilesProject = ResourcesPlugin.getWorkspace().getRoot().getProject(RESOURCE_TEMPFILES_PROJECT_NAME);
		}
		return remoteSystemsTempFilesProject;
	}
    /**
     * Create a remote systems project, plus the core subfolders required.
     * @param proj the handle for the remote systems project
     * @return the IProject handle of the project (the argument)
     */
    private static IProject ensureRemoteSystemsProject(IProject proj) 
    {
		// Check first for the project to be closed. If yes, try to open it and if this fails,
		// try to delete if first before failing here. The case is that the user removed the
		// directory in the workspace and we must be able to recover from it.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=172437.
		if (!proj.isOpen()) {
			try {
				proj.open(null);
			} catch (Exception e) {
				try {
					proj.delete(false, true, null);
					RSECorePlugin.getDefault().getLogger().logWarning("Removed stale remote systems project reference. Re-creating remote system project to recover."); //$NON-NLS-1$
				} catch (CoreException exc) {
					// If the delete fails, the original opening error will be passed to the error log.
					RSECorePlugin.getDefault().getLogger().logError("error opening remote systems project", e); //$NON-NLS-1$
				}
			}
		}
		if (!proj.exists()) {
			try {
				proj.create(null);
				proj.open(null);
				IProjectDescription description = proj.getDescription();
				String newNatures[] = { RemoteSystemsProject.ID };
				description.setNatureIds(newNatures);
				proj.setDescription(description, null);
			} catch (Exception e) {
				RSECorePlugin.getDefault().getLogger().logError("error creating remote systems project", e); //$NON-NLS-1$
			}
		}
		return proj;
	}

    // --------------------------------------------
    // GET ALL EXISTING PROFILE NAMES OR FOLDERS...
    // --------------------------------------------
    /**
     * Each root folder of the project is assumed to be a profile, if it has a file named profile.xmi
     */
    /*
    public static IFolder[] getProfileFolders()
    {
    	IProject proj = getRemoteSystemsProject();
    	IFolder[] allFolders = getResourceHelpers().listFolders(proj);
    	//System.out.println("Inside getProfileFolders. allFolders.length = " + allFolders.length);
    	Vector v = new Vector();
    	for (int idx=0; idx<allFolders.length; idx++)
    	{
    		String saveFileName = SystemProfileManager.getSaveFileName(allFolders[idx].getName());
    		IFile saveFile = getResourceHelpers().getFile(allFolders[idx], saveFileName);    		
    		boolean saveFileExists = getResourceHelpers().fileExists(saveFile);
    	    //System.out.println("...folderName = " + allFolders[idx].getName());
    	    //System.out.println("...saveFileName = " + saveFileName);
    	    //System.out.println("...saveFile.exists() = " + saveFileExists);
    		if (saveFileExists)
    		  v.addElement(allFolders[idx]);
    	}
    	return getResourceHelpers().convertToFolderArray(v);
    }
    */

    /**
     * Guess the profile names by itemizing all the root folders, and
     *  assuming any such folder that has a file in it named "profile.xmi" is
     *  indeed a profile whose name equals the folder name.
     */
    /*
    public static String[] deduceProfileNames()
    {
    	IFolder[] folders = getProfileFolders();
    	String[] names = new String[folders.length];
    	for (int idx=0; idx<names.length; idx++)
    	   names[idx] = folders[idx].getName();
    	return names;
    }
    */

    /**
     * Get profiles folder for a given profile name
     */
    public static IFolder getProfileFolder(String profileName)
    {
        return getResourceHelpers().getOrCreateFolder(getRemoteSystemsProject(),profileName);      	
    }

    /*
     * --------------------------------------------------------------------------------------------------------------------------------
     * USER ACTIONS SUBTREE FOLDER METHODS...
     * ======================================
     *  .--- Team (folder)           - getProfileFolder(SystemProfile/"team")
     *  |  |
     *  |  |
     *  |  .--- UserActions (folder)    - getUserActionsFolder(SystemProfile/"team")
     *  |     |
     *  |     .--- SubSystemConfigurationID1 (folder) - getUserActionsFolder(SystemProfile/"team", SubSystemConfiguration)
     *  |     |  .--- actions.xml (file)
     *  |     .--- SubSystemConfigurationID2 (folder)
     *  |        .--- actions.xml (file)
     * --------------------------------------------------------------------------------------------------------------------------------
     */
    // ---------------------------------------------------
    // GET USER DEFINED ACTIONS ROOT FOLDER PER PROFILE...
    // ---------------------------------------------------
    /**
     * Get user defined actions root folder given a system profile name
     */
    protected static IFolder getUserActionsFolder(String profileName)
    {
        return getResourceHelpers().getOrCreateFolder(getProfileFolder(profileName),RESOURCE_USERACTIONS_FOLDER_NAME);      	
    }
    /**
     * Get user defined actions root folder given a system profile object and subsystem factory
     */
    public static IFolder getUserActionsFolder(ISystemProfile profile, ISubSystemConfiguration ssFactory)
    {
        return getUserActionsFolder(profile.getName(),ssFactory);
    }
    /**
     * Get user defined actions root folder given a system profile name and subsystem factory
     */
    public static IFolder getUserActionsFolder(String profileName, ISubSystemConfiguration ssFactory)
    {
        IFolder parentFolder = getUserActionsFolder(profileName);
        String folderName = getFolderName(ssFactory);
        return getResourceHelpers().getOrCreateFolder(parentFolder, folderName); // Do create it.
    }
	/**
	 * Test for existence of user defined actions root folder given a system profile name and subsystem factory
	 */
	public static boolean testUserActionsFolder(String profileName, ISubSystemConfiguration ssFactory)
	{
		IFolder parentFolder = getUserActionsFolder(profileName);
		String folderName = getFolderName(ssFactory);
		return (getResourceHelpers().getFolder(parentFolder, folderName).exists()); // Do NOT create it.
	}
	
    /**
     * Get user defined actions root folder given a system profile name and subsystem factory Id.
     * This is a special-needs method provided for the Import action processing,
     * when a subsystem instance is not available.
     */
    public static IFolder getUserActionsFolder( String profileName, String factoryId)
    {
        IFolder parentFolder = getUserActionsFolder(profileName);
        return getResourceHelpers().getOrCreateFolder(parentFolder, factoryId); // Do create it.
    }

    /*
     * --------------------------------------------------------------------------------------------------------------------------------
     * COMPILE COMMAND SUBTREE FOLDER METHODS...
     * ======================================
     *  .--- Team (folder)           - getProfileFolder(SystemProfile/"team")
     *  |  |
     *  |  |
     *  |  .--- CompileCommands (folder)    - getCompileCommandsFolder(SystemProfile/"team")
     *  |     |
     *  |     .--- SubSystemConfigurationID1 (folder) - getCompileCommandsFolder(SystemProfile/"team", SubSystemConfiguration)
     *  |     |  .--- compileCommands.xml (file)
     *  |     .--- SubSystemConfigurationID2 (folder)
     *  |        .--- compileCommands.xml (file)
     * --------------------------------------------------------------------------------------------------------------------------------
     */
    // ---------------------------------------------------
    // GET COMPILE COMMANDS ROOT FOLDER PER PROFILE...
    // ---------------------------------------------------
    /**
     * Get compile commands root folder given a system profile name
     */
    protected static IFolder getCompileCommandsFolder(String profileName)
    {
        return getResourceHelpers().getOrCreateFolder(getProfileFolder(profileName),RESOURCE_COMPILECOMMANDS_FOLDER_NAME);      	
    }
    /**
     * Get compile commands root folder given a system profile object and subsystem factory
     */
    public static IFolder getCompileCommandsFolder(ISystemProfile profile, ISubSystemConfiguration ssFactory)
    {
        return getCompileCommandsFolder(profile.getName(),ssFactory);
    }
    /**
     * Get compile commands root folder given a system profile name and subsystem factory
     */
    public static IFolder getCompileCommandsFolder(String profileName, ISubSystemConfiguration ssFactory)
    {
        IFolder parentFolder = getCompileCommandsFolder(profileName);
        String folderName = getFolderName(ssFactory);
        return getResourceHelpers().getOrCreateFolder(parentFolder, folderName); // Do create it.
    }
   
    /**
     * Get compile commands root folder given a system profile name and subsystem factory Id.
     * This is a special-needs method provided for the Import action processing,
     * when a subsystem instance is not available.
     */
    public static IFolder getCompileCommandsFolder( String profileName, String factoryId)
    {
        IFolder parentFolder = getCompileCommandsFolder(profileName);
        return getResourceHelpers().getOrCreateFolder(parentFolder, factoryId); // Do create it.
    }

    // -------------------
    // FOLDER ACTIONS...
    // -------------------

    /**
     * Rename a folder
     */
    public static void renameFolder(IFolder folder, String newName)
    {
    	getResourceHelpers().renameResource(folder, newName);
    }
    /**
     * Delete a folder
     */
    public static void deleteFolder(IFolder folder)
    {
    	getResourceHelpers().deleteResource(folder);
    }

    // -------------------
    // FILE ACTIONS...
    // -------------------

    /**
     * Rename a file
     */
    public static void renameFile(IFolder folder, String oldName, String newName)
    {
    	getResourceHelpers().renameResource(
    	   getResourceHelpers().getFile(folder,oldName), newName);
    }
    /**
     * Delete a file
     */
    public static void deleteFile(IFolder folder, String fileName)
    {
    	getResourceHelpers().deleteResource(
    	  getResourceHelpers().getFile(folder, fileName));
    }
        

    // -------------------
    // GENERIC HELPERS...
    // -------------------

    /**
     * Map a connection pool name to a profile name.
     * Current algorith is that pool name equals profile name, but we use
     *  this method to allow flexibility in the future.
     */
    public static String getProfileName(ISystemHostPool pool)
    {
    	return pool.getName();
    }

    /**
     * Map a filter pool manager name to a profile name
     * Current algorith is that manager name equals profile name, but we use
     *  this method to allow flexibility in the future.
     */
    public static String getProfileName(ISystemFilterPoolManager mgr)
    {
    	return mgr.getName();
    }

    /**
     * Map a filter pool name to a folder name
     * Current algorith is that pool name equals folder name, but we use
     *  this method to allow flexibility in the future.
     */
    public static String getFolderName(ISystemFilterPool pool)
    {
    	return pool.getName();
    }

    /**
     * Map a system connection object to a folder name
     */
    public static String getFolderName(IHost conn)
    {
    	return conn.getAliasName();
    }

    /**
     * Map a subsystem factory object to a folder name
     */
    public static String getFolderName(ISubSystemConfiguration ssFactory)
    {
    	return ssFactory.getId(); // Should we use name instead?? Can we assume the name is unique?
    }

    /**
     * Given any folder, return its path as a string.
     */
    public static String getFolderPath(IFolder folder)
    {
    	return getResourceHelpers().getFolderPath(folder);
    }

    /**
     * Given any folder, return its path as a string, and an ending '\'
     */
    public static String getFolderPathWithTerminator(IFolder folder)
    {
    	return addPathTerminator(getResourceHelpers().getFolderPath(folder));
    }


    /**
     * Return singleton of resource helpers object
     */
    protected static SystemResourceHelpers getResourceHelpers()
    {
        if (helpers == null)    	
        {
    	  helpers = SystemResourceHelpers.getResourceHelpers();
    	  //helpers.setLogFile(RSEUIPlugin.getDefault().getLogFile());
        }
    	return helpers;
    }

    /**
     * Ensure given path ends with path separator.
     */
    public static String addPathTerminator(String path)
    {
        if (!path.endsWith(File.separator))
          path = path + File.separatorChar;
        //else
        //  path = path;
        return path;
    }
    
    /**
     * Test if a resource is in use, prior to attempting to rename or delete it.
     * @return true if it is in use or read only, false if it is not.
     */
    public static boolean testIfResourceInUse(IResource resource)
    {
    	return SystemResourceHelpers.testIfResourceInUse(resource);
    }
    
    /*
     * --------------------------------------------------------------------------------------------------------------------------------
     * TYPE FILTERS SUBTREE FOLDER METHODS...
     * ======================================
     *  .--- TypeFilters (folder)    - getTypeFiltersFolder()
     *     .--- SubSystemConfigurationID1 (folder) - getTypeFiltersFolder(SubSystemConfiguration)
     *     |  .--- typefilters.xmi (file)
     * --------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * Get the typeFilters root folder
     */
    public static IFolder getTypeFiltersFolder()
    {
        return getResourceHelpers().getFolder(getRemoteSystemsProject(),RESOURCE_TYPE_FILTERS_FOLDER_NAME);      	
    }
    /**
     * Get the typeFilters sub-folder per subsystem factory object
     */
    public static IFolder getTypeFiltersFolder(ISubSystemConfiguration ssFactory)
    {
        IFolder parentFolder = getTypeFiltersFolder();
        String folderName = getFolderName(ssFactory);
        return getResourceHelpers().getOrCreateFolder(parentFolder, folderName); // DO create it.            	
    }
    /**
     * Get the typeFilters sub-folder per subsystem factory id
     */
    public static IFolder getTypeFiltersFolder(String ssFactoryId)
    {
        IFolder parentFolder = getTypeFiltersFolder();
        return getResourceHelpers().getOrCreateFolder(parentFolder, ssFactoryId); // DO create it.            	
    }
}