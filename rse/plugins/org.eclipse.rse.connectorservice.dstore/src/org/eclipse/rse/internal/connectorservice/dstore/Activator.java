/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * Martin Oberhuber (Wind River) - [168870] refactor org.eclipse.rse.core package of the UI plugin
 * David McKnight   (IBM)        - [216252] [api][nls] Resource Strings specific to subsystems should be moved from rse.ui into files.ui / shells.ui / processes.ui where possible
 * David McKnight   (IBM)        - [220123] [api][dstore] Configurable timeout on irresponsiveness
 * David McKnight   (IBM)        - [227406] [dstore] DStoreFileService must listen to buffer size preference changes
 *******************************************************************************/

package org.eclipse.rse.internal.connectorservice.dstore;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.rse.connectorservice.dstore.IUniversalDStoreConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.osgi.framework.BundleContext;


/**
 * The main plugin class to be used in the desktop.
 */
public class Activator extends SystemBasePlugin {

	//The shared instance.
	private static Activator plugin;
	
	public final static String PLUGIN_ID = "org.eclipse.rse.connectorservice.dstore"; //$NON-NLS-1$
	
	/**
	 * The constructor.
	 */
	public Activator() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception 
	{
		super.start(context);

		initializeDefaultPreferences();
	}
	
	
	public void initializeDefaultPreferences() {
		IPreferenceStore store = RSEUIPlugin.getDefault().getPreferenceStore();

		store.setDefault(IUniversalDStoreConstants.RESID_PREF_SOCKET_TIMEOUT, 5000);	
		
		// do keepalive
		store.setValue(IUniversalDStoreConstants.RESID_PREF_DO_KEEPALIVE, true);
		
		// socket read timeout 
		store.setDefault(IUniversalDStoreConstants.RESID_PREF_SOCKET_READ_TIMEOUT,  3600000);
		
		// keepalive response timeout
		store.setDefault(IUniversalDStoreConstants.RESID_PREF_KEEPALIVE_RESPONSE_TIMEOUT,  60000);		
		
		// show mismatched server warning
		store.setDefault(IUniversalDStoreConstants.ALERT_MISMATCHED_SERVER, true);

		// cache remote classes
		store.setDefault(IUniversalDStoreConstants.RESID_PREF_CACHE_REMOTE_CLASSES, true);				
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}



	protected void initializeImageRegistry()
	{
		// TODO Auto-generated method stub
		
	}
}
