package com.make.equo.eclipse.monaco.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Reference;

import com.make.equo.eclipse.monaco.lsp.EclipseLspProxy;
import com.make.equo.monaco.EquoMonacoEditor;
import com.make.equo.monaco.EquoMonacoEditorWidgetBuilder;
import com.make.equo.monaco.lsp.LspProxy;
import com.make.equo.server.api.IEquoServer;
import com.make.equo.ws.api.IEquoRunnable;

public class MonacoEditorPart extends EditorPart implements ITextEditor {

	@Reference
	private EquoMonacoEditorWidgetBuilder monacoBuilder;

	private volatile boolean isDirty = false;

	private IEquoRunnable<Boolean> dirtyListener = (isDirty) -> {
		this.isDirty = isDirty;
		firePropertyChange(PROP_DIRTY);
	};

	private EquoMonacoEditor editor;
	private IDocumentProvider documentProvider;

	private ISelectionProvider selectionProvider = new MonacoEditorSelectionProvider();

	private EditorAction undoAction;
	private EditorAction redoAction;
	private EditorAction selectAllAction;
	private EditorAction copyAction;
	private EditorAction cutAction;
	private EditorAction pasteAction;
	private EditorAction findAction;

	@Override
	public void doSave(IProgressMonitor monitor) {
		String editorContents = editor.getContentsSync();
		IEditorInput input = getEditorInput();
		if (input instanceof FileEditorInput) {
			Display.getDefault().asyncExec(() -> {
				try {
					((FileEditorInput) input).getFile().setContents(
							new ByteArrayInputStream(editorContents.getBytes(Charset.forName("UTF-8"))), true, false,
							monitor);
					editor.handleAfterSave();
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}

	}

	@Override
	public void doSaveAs() {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		final IEditorInput input = getEditorInput();
		final IEditorInput newInput;

		SaveAsDialog dialog = new SaveAsDialog(shell);

		IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);
		else
			dialog.setOriginalName(input.getName());

		dialog.create();

		if (dialog.open() == Window.CANCEL) {
			return;
		}

		IPath filePath = dialog.getResult();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IFile file = workspace.getRoot().getFile(filePath);
		newInput = new FileEditorInput(file);
		try {
			file.getLocation().toFile().createNewFile();
			file.getParent().refreshLocal(1, new NullProgressMonitor());
		} catch (IOException | CoreException e) {
			e.printStackTrace();
			return;
		}

		setInput(newInput);
		initializeNewInput(newInput);
		editor.setFilePath(((FileEditorInput) newInput).getPath().toString());
		doSave(new NullProgressMonitor());
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);

	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	private void initializeNewInput(IEditorInput input) {
		setPartName(input.getName());
		if (input instanceof FileEditorInput) {
			final FileEditorInput fileInput = (FileEditorInput) input;
			setTitleToolTip(fileInput.getPath().toString());
			ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
				@Override
				public void resourceChanged(final IResourceChangeEvent event) {
					IResourceDelta delta = event.getDelta();

					delta = delta.findMember(fileInput.getFile().getFullPath());
					if (delta == null)
						return;

					if (delta.getKind() == IResourceDelta.REMOVED) {
						if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
							IWorkspace workspace = ResourcesPlugin.getWorkspace();
							workspace.removeResourceChangeListener(this);
							IPath newPath = delta.getMovedToPath();
							IFile file = workspace.getRoot().getFile(newPath);
							FileEditorInput newInput = new FileEditorInput(file);
							setInput(newInput);
							initializeNewInput(newInput);
							editor.setFilePath(newInput.getPath().toString());
						}
					}
				}
			});
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		IEditorInput input = getEditorInput();
		initializeNewInput(input);
		if (input instanceof FileEditorInput) {
			FileEditorInput fileInput = (FileEditorInput) input;
			setTitleToolTip(fileInput.getPath().toString());
			LspProxy lspProxy = getLspProxy(fileInput.getFile());

			try (InputStream contents = fileInput.getFile().getContents()) {
				int singleByte;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((singleByte = contents.read()) != -1) {
					baos.write(singleByte);
					;
				}

				String textContent = new String(baos.toByteArray());

				try {
					BundleContext bndContext = FrameworkUtil.getBundle(EquoMonacoEditorWidgetBuilder.class)
							.getBundleContext();
					activateNeededServices(bndContext);

					ServiceReference<EquoMonacoEditorWidgetBuilder> svcReference = bndContext
							.getServiceReference(EquoMonacoEditorWidgetBuilder.class);

					EquoMonacoEditorWidgetBuilder builder = bndContext.getService(svcReference);
					editor = builder.withParent(parent).withStyle(parent.getStyle()).withContents(textContent)
							.withFilePath(fileInput.getURI().toString()).withLSP(lspProxy).create();
					documentProvider = new MonacoEditorDocumentProvider(editor);

					editorConfigs();

					getSite().setSelectionProvider(selectionProvider);

					createActions();
					activateActions();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Couldn't retrieve Monaco Editor service");
				}
			} catch (CoreException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	@SuppressWarnings("unchecked")
	private void activateNeededServices(BundleContext bndContext) {
		ServiceReference<IEquoServer> serviceReference = (ServiceReference<IEquoServer>) bndContext
				.getServiceReference(IEquoServer.class.getName());
		if (serviceReference != null) {
			bndContext.getService(serviceReference);
		}
	}

	private LspProxy getLspProxy(IFile file) {
		try {
			Collection<LanguageServerWrapper> wrappers = LanguageServiceAccessor.getLSWrappers(file, null);
			if (!wrappers.isEmpty()) {
				LanguageServerWrapper lspServer = wrappers.iterator().next();
				return new EclipseLspProxy(lspServer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void editorConfigs() {
		IEquoRunnable<Boolean> redoListener = (canRedo) -> {
			redoAction.setEnabled(canRedo);
		};
		IEquoRunnable<Boolean> undoListener = (canUndo) -> {
			undoAction.setEnabled(canUndo);
		};
		editor.subscribeChanges(dirtyListener, undoListener, redoListener);

		editor.configSelection((selection) -> {
			Display.getDefault().asyncExec(() -> {
				ISelection iSelection = (selection) ? new TextSelection(0, 1) : new TextSelection(0, -1);
				selectionProvider.setSelection(iSelection);
			});
		});
	}

	private void createActions() {
		undoAction = new EditorAction(() -> editor.undo());
		redoAction = new EditorAction(() -> editor.redo());
		copyAction = new EditorAction(() -> editor.copy(), selectionProvider);
		cutAction = new EditorAction(() -> editor.cut(), selectionProvider);
		pasteAction = new EditorAction(() -> editor.paste(), null, () -> {
			Clipboard clipboard = new Clipboard(Display.getCurrent());
			TextTransfer textTransfer = TextTransfer.getInstance();
			String textData = (String) clipboard.getContents(textTransfer);
			clipboard.dispose();
			return (textData != null);
		});
		findAction = new EditorAction(() -> {
			try {
				IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
				handlerService.executeCommand("com.make.equo.eclipse.monaco.editor.LSFindReferences", null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		findAction.setEnabled(true);
		selectAllAction = new EditorAction(() -> editor.selectAll());
		selectAllAction.setEnabled(true);
	}

	private void activateActions() {
		IActionBars actionBars = getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
		actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), findAction);
		actionBars.updateActionBars();
	}

	@Override
	public void setFocus() {
		getSite().setSelectionProvider(selectionProvider);
		activateActions();
	}

	@Override
	public void dispose() {
		super.dispose();
		editor.dispose();
	}

	@Override
	public IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}

	@Override
	public void close(boolean save) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isEditable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void doRevertToSaved() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAction(String actionID, IAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public IAction getAction(String actionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setActionActivationCode(String actionId, char activationCharacter, int activationKeyCode,
			int activationStateMask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeActionActivationCode(String actionId) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean showsHighlightRangeOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHighlightRange(int offset, int length, boolean moveCursor) {
		// TODO Auto-generated method stub

	}

	@Override
	public IRegion getHighlightRange() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetHighlightRange() {
		// TODO Auto-generated method stub

	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	@Override
	public void selectAndReveal(int offset, int length) {
		editor.selectAndReveal(offset, length);
	}

}
