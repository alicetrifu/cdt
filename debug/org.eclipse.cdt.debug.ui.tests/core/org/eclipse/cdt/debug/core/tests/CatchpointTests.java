/*******************************************************************************
 * Copyright (c) 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.core.tests;

import java.io.IOException;

import junit.framework.Test;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDIFunctionLocation;
import org.eclipse.cdt.debug.core.cdi.ICDILocation;
import org.eclipse.cdt.debug.core.cdi.ICDILocator;
import org.eclipse.cdt.debug.core.cdi.model.ICDIBreakpoint;
import org.eclipse.cdt.debug.core.cdi.model.ICDIBreakpointManagement3;
import org.eclipse.cdt.debug.core.cdi.model.ICDICatchpoint;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.cdi.model.Catchpoint;

public class CatchpointTests extends AbstractDebugTest {
	public static Test suite() {
		return new DebugTestWrapper(CatchpointTests.class){};
	}

	protected String getProjectName() {
		return "catchpoints";
	}

	protected String getProjectZip() {
		return "resources/debugCxxTest.zip";
	}

	protected String getProjectBinary() {
		return "catchpoints";
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		createDebugSession();
		assertNotNull(currentTarget);
		currentTarget.deleteAllBreakpoints();
		pause();
	}

	void setBreakOnMain() throws CDIException {
		ICDILocation location = null;
		location = currentTarget.createFunctionLocation("", "main"); //$NON-NLS-1$	
		currentTarget.setFunctionBreakpoint(ICDIBreakpoint.TEMPORARY, (ICDIFunctionLocation) location, null, false);

	}

	public void testCatch() throws CModelException, IOException, MIException, CDIException {
		catchpoints(Catchpoint.CATCH, "");
	}

	public void testThrow() throws CModelException, IOException, MIException, CDIException {
		catchpoints(Catchpoint.THROW, "");
	}
	
	private void catchpoints(String type, String arg) throws CModelException, IOException, MIException, CDIException {
		ICDIBreakpoint[] breakpoints;
		ICDICatchpoint curbreak;

		setBreakOnMain();
		currentTarget.restart();
		waitSuspend(currentTarget);
		ICDILocator locator = currentTarget.getThreads()[0].getStackFrames()[0].getLocator();
		assertEquals("Debug should be stopped in function 'main' but it is stopped in: " + locator.getFunction(),
				"main", locator.getFunction());

		currentTarget.deleteAllBreakpoints();
		pause();
		assertTrue(currentTarget instanceof ICDIBreakpointManagement3);
		((ICDIBreakpointManagement3) currentTarget).setCatchpoint(type, arg, ICDIBreakpoint.REGULAR, null, false, true);
		pause();
		breakpoints = currentTarget.getBreakpoints();
		assertNotNull(breakpoints);
		assertTrue(breakpoints.length == 1);
		if (breakpoints[0] instanceof ICDICatchpoint) {
			curbreak = (ICDICatchpoint) breakpoints[0];
		} else
			curbreak = null;
		assertNotNull(curbreak);
		currentTarget.resume(false);
		waitSuspend(currentTarget);
		// it is stopped we are fine, it did hit breakpoint
	}

}
