package com.make.equo.eclipse.monaco.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
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
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
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
		setPartName(file.getName());
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

	@Override
	public void createPartControl(Composite parent) {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
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
							.withFileName(fileInput.getURI().toString()).withLSP(lspProxy).create();

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
		findAction = new EditorAction(() -> editor.find());
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
		// TODO Auto-generated method stub
		return new IDocumentProvider() {

			@Override
			public void connect(Object element) throws CoreException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void disconnect(Object element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public IDocument getDocument(Object element) {
				// TODO Auto-generated method stub
				return new IDocument() {
					private String text = editor.getContentsSync();

					@Override
					public char getChar(int offset) throws BadLocationException {
						return text.charAt(offset);
					}

					@Override
					public int getLength() {
						return text.length();
					}

					@Override
					public String get() {
						return text;
					}

					@Override
					public String get(int offset, int length) throws BadLocationException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void set(String text) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void replace(int offset, int length, String text) throws BadLocationException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void addDocumentListener(IDocumentListener listener) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removeDocumentListener(IDocumentListener listener) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removePrenotifiedDocumentListener(IDocumentListener documentAdapter) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void addPositionCategory(String category) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removePositionCategory(String category) throws BadPositionCategoryException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public String[] getPositionCategories() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public boolean containsPositionCategory(String category) {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void addPosition(Position position) throws BadLocationException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removePosition(Position position) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void addPosition(String category, Position position)
							throws BadLocationException, BadPositionCategoryException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removePosition(String category, Position position) throws BadPositionCategoryException {
						// TODO Auto-generated method stub
						
					}

					@Override
					public Position[] getPositions(String category) throws BadPositionCategoryException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public boolean containsPosition(String category, int offset, int length) {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public int computeIndexInCategory(String category, int offset)
							throws BadLocationException, BadPositionCategoryException {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public void addPositionUpdater(IPositionUpdater updater) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removePositionUpdater(IPositionUpdater updater) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void insertPositionUpdater(IPositionUpdater updater, int index) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public IPositionUpdater[] getPositionUpdaters() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public String[] getLegalContentTypes() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public String getContentType(int offset) throws BadLocationException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public ITypedRegion getPartition(int offset) throws BadLocationException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public ITypedRegion[] computePartitioning(int offset, int length) throws BadLocationException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void addDocumentPartitioningListener(IDocumentPartitioningListener listener) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void removeDocumentPartitioningListener(IDocumentPartitioningListener listener) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public IDocumentPartitioner getDocumentPartitioner() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public int getLineLength(int line) throws BadLocationException {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public int getLineOfOffset(int offset) throws BadLocationException {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public int getLineOffset(int line) throws BadLocationException {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public IRegion getLineInformation(int line) throws BadLocationException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public int getNumberOfLines() {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public int getNumberOfLines(int offset, int length) throws BadLocationException {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public int computeNumberOfLines(String text) {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public String[] getLegalLineDelimiters() {
						// TODO Auto-generated method stub
						return new String[]{"\n"};
					}

					@Override
					public String getLineDelimiter(int line) throws BadLocationException {
						// TODO Auto-generated method stub
						return "\n";
					}

					@Override
					public int search(int startOffset, String findString, boolean forwardSearch, boolean caseSensitive,
							boolean wholeWord) throws BadLocationException {
						return text.indexOf(findString);
					}

				};
			}

			@Override
			public void resetDocument(Object element) throws CoreException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void saveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite)
					throws CoreException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public long getModificationStamp(Object element) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public long getSynchronizationStamp(Object element) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean isDeleted(Object element) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean mustSaveDocument(Object element) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean canSaveDocument(Object element) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public IAnnotationModel getAnnotationModel(Object element) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void aboutToChange(Object element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void changed(Object element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void addElementStateListener(IElementStateListener listener) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removeElementStateListener(IElementStateListener listener) {
				// TODO Auto-generated method stub
				
			}
			
		};
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
