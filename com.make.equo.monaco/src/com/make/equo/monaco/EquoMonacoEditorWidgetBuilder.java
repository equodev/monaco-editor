package com.make.equo.monaco;

import org.eclipse.swt.widgets.Composite;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;

import com.make.equo.monaco.lsp.LspProxy;
import com.make.equo.filesystem.api.IEquoFileSystem;
import com.make.equo.ws.api.IEquoEventHandler;
import com.make.equo.ws.api.IEquoWebSocketService;

@Component(service = EquoMonacoEditorWidgetBuilder.class, scope = ServiceScope.PROTOTYPE)
public class EquoMonacoEditorWidgetBuilder {

	@Reference
	private IEquoEventHandler equoEventHandler;

	@Reference
	private IEquoFileSystem equoFileSystem;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
	private IEquoWebSocketService websocketService;

	private Composite parent;
	private int style;
	private String contents;
	private String filePath;
	private LspProxy lsp;
	private String rootPath;

	public EquoMonacoEditorWidgetBuilder() {
		this.style = -1;
		this.contents = "";
		this.filePath = "";
	}

	public EquoMonacoEditorWidgetBuilder withParent(Composite parent) {
		this.parent = parent;
		return this;
	}

	public EquoMonacoEditorWidgetBuilder withContents(String contents) {
		this.contents = contents;
		return this;
	}

	public EquoMonacoEditorWidgetBuilder withFilePath(String filePath) {
		this.filePath = filePath;
		return this;
	}

	public EquoMonacoEditorWidgetBuilder withStyle(int style) {
		this.style = style;
		return this;
	}

	public EquoMonacoEditorWidgetBuilder withLSP(LspProxy lsp) {
		this.lsp = lsp;
		return this;
	}

	public EquoMonacoEditorWidgetBuilder withRootPath(String rootPath) {
		this.rootPath = rootPath;
		return this;
	}

	public EquoMonacoEditor create() {
		if (style == -1) {
			style = parent.getStyle();
		}
		EquoMonacoEditor editor = new EquoMonacoEditor(parent, style, equoEventHandler, websocketService,
				equoFileSystem);
		editor.setRootPath(rootPath);
		editor.createEditor(contents, filePath, lsp);
		return editor;
	}

}
