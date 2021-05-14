package com.equo.eclipse.monaco.editor;

import static com.equo.eclipse.monaco.contribution.IMonacoConstants.EQUO_MONACO_CONTRIBUTION_NAME;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonObject;
import com.equo.filesystem.api.IEquoFileSystem;
import com.equo.monaco.AbstractEquoMonacoEditorBuilder;
import com.equo.monaco.EquoMonacoEditor;
import com.equo.monaco.lsp.LspProxy;
import com.equo.ws.api.IEquoEventHandler;
import com.equo.ws.api.IEquoWebSocketService;

@Component(service = EquoMonacoEditorWidgetBuilder.class, scope = ServiceScope.PROTOTYPE)
public class EquoMonacoEditorWidgetBuilder extends AbstractEquoMonacoEditorBuilder {

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
				equoFileSystem, EQUO_MONACO_CONTRIBUTION_NAME);
		editor.setRootPath(rootPath);
		createEditor(editor, contents, filePath, lsp);
		return editor;
	}

	@Activate
	public void activate() {
		equoEventHandler.on("_openCodeEditor", (JsonObject payload) -> createNew(payload));
	}

	private void createNew(JsonObject payload) {
		final File fileToOpen = new File(payload.get("path").getAsString());
		final JsonObject selection = payload.get("selection").getAsJsonObject();
		final int startLine = selection.get("startLineNumber").getAsInt() - 1;
		final int startColumn = selection.get("startColumn").getAsInt() - 1;
		final int endLine = selection.get("endLineNumber").getAsInt() - 1;
		final int endColumn = selection.get("endColumn").getAsInt() - 1;

		Display.getDefault().asyncExec(() -> {
			if (fileToOpen.exists() && fileToOpen.isFile()) {
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IEditorPart openEditor = IDE.openEditor(page, fileStore.toURI(),
							"com.equo.eclipse.monaco.editor.EquoEditor", true);
					if (openEditor instanceof ITextEditor) {
						ITextEditor textEditor = (ITextEditor) openEditor;
						IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
						final int offset = document.getLineOffset(startLine) + startColumn;
						final int length = document.getLineOffset(endLine) + endColumn - offset;
						textEditor.selectAndReveal(offset, length);
					}
				} catch (PartInitException | BadLocationException e) {
					// Put your exception handler here if you wish to
				}
			} else {
				// Do something if the file does not exist
			}
		});
	}

}
