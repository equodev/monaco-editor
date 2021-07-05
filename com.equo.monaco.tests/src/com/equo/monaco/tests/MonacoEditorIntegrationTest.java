/****************************************************************************
**
** Copyright (C) 2021 Equo
**
** This file is part of Equo Framework.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equoplatform.com/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/

package com.equo.monaco.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_MINUTE;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.equo.server.api.IEquoServer;
import com.equo.testing.common.util.EquoRule;
import com.equo.comm.api.IEquoEventHandler;
import com.equo.comm.api.IEquoWebCommService;
import com.equo.comm.api.JsonPayloadEquoRunnable;

public class MonacoEditorIntegrationTest {

	@Inject
	protected IEquoServer equoServer;

	@Inject
	protected IEquoCommService commService;

	@Inject
	protected IEquoEventHandler handler;

	protected Browser chromium;

	private Display display;

	@Rule
	public EquoRule rule = new EquoRule(this).runInNonUiThread();

	@Before
	public void before() {
		System.setProperty("swt.chromium.debug", "true");
		display = rule.getDisplay();
		display.syncExec(() -> {
			Shell shell = rule.createShell();
			chromium = new Browser(shell, SWT.NONE);
			GridData data = new GridData();
			data.minimumWidth = 1;
			data.minimumHeight = 1;
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			chromium.setLayoutData(data);
			chromium.setUrl("http://testbundles/" + String.format("?equocommport=%d", commService.getPort()));
			shell.open();
		});
	}

	@Test
	public void monacoEditorIsCreatedCorrectly() {
		AtomicReference<Boolean> wasCreated = new AtomicReference<>(false);
		handler.on("_doGetIsEditorCreated", (JsonPayloadEquoRunnable) payload -> {
			wasCreated.set(true);
		});
		await().timeout(ONE_MINUTE).untilAsserted(() -> {
			handler.send("_getIsEditorCreated");
			assertThat(wasCreated.get()).isTrue();
		});
	}

}
