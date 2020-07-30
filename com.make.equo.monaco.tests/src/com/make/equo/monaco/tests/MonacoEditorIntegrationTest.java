package com.make.equo.monaco.tests;

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

import com.make.equo.server.api.IEquoServer;
import com.make.equo.testing.common.util.EquoRule;
import com.make.equo.ws.api.IEquoEventHandler;
import com.make.equo.ws.api.IEquoWebSocketService;
import com.make.equo.ws.api.JsonPayloadEquoRunnable;

public class MonacoEditorIntegrationTest {

	@Inject
	protected IEquoServer equoServer;

	@Inject
	protected IEquoWebSocketService websocketService;

	@Inject
	protected IEquoEventHandler handler;

	protected Browser chromium;

	private Display display;

	@Rule
	public EquoRule rule = new EquoRule(this).runInNonUIThread();

	@Before
	public void before() {
		System.setProperty("swt.chromium.debug", "true");
		System.setProperty("swt.chromium.args",
				"--proxy-server=localhost:9896;--ignore-certificate-errors;--allow-file-access-from-files;--disable-web-security;--enable-widevine-cdm;--proxy-bypass-list=127.0.0.1");
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
			chromium.setUrl("http://testbundles/" + String.format("?equowsport=%d", websocketService.getPort()));
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
