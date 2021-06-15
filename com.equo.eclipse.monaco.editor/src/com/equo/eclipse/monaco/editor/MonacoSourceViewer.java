package com.equo.eclipse.monaco.editor;

import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

/**
 * Implementation of SourceViewer to provide a custom Document for the editor.
 */
public class MonacoSourceViewer implements ISourceViewer {
  private MonacoEditorPart editor;

  public MonacoSourceViewer(MonacoEditorPart editor) {
    this.editor = editor;
  }

  @Override
  public StyledText getTextWidget() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setUndoManager(IUndoManager undoManager) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy, String contentType) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAutoIndentStrategy(IAutoIndentStrategy strategy, String contentType) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setTextHover(ITextHover textViewerHover, String contentType) {
    // TODO Auto-generated method stub

  }

  @Override
  public void activatePlugins() {
    // TODO Auto-generated method stub

  }

  @Override
  public void resetPlugins() {
    // TODO Auto-generated method stub

  }

  @Override
  public void addViewportListener(IViewportListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeViewportListener(IViewportListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addTextListener(ITextListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeTextListener(ITextListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addTextInputListener(ITextInputListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeTextInputListener(ITextInputListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDocument(IDocument document) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDocument(IDocument document, int modelRangeOffset, int modelRangeLength) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDocument(IDocument document, IAnnotationModel annotationModel) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDocument(IDocument document, IAnnotationModel annotationModel,
      int modelRangeOffset, int modelRangeLength) {
    // TODO Auto-generated method stub

  }

  @Override
  public IDocument getDocument() {
    return LSPEclipseUtils.getDocument(editor.getEditorInput());
  }

  @Override
  public void setEventConsumer(IEventConsumer consumer) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setEditable(boolean editable) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isEditable() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setVisibleRegion(int offset, int length) {
    // TODO Auto-generated method stub

  }

  @Override
  public void resetVisibleRegion() {
    // TODO Auto-generated method stub

  }

  @Override
  public IRegion getVisibleRegion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean overlapsWithVisibleRegion(int offset, int length) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void changeTextPresentation(TextPresentation presentation, boolean controlRedraw) {
    // TODO Auto-generated method stub

  }

  @Override
  public void invalidateTextPresentation() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setTextColor(Color color) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setTextColor(Color color, int offset, int length, boolean controlRedraw) {
    // TODO Auto-generated method stub

  }

  @Override
  public ITextOperationTarget getTextOperationTarget() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IFindReplaceTarget getFindReplaceTarget() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setSelectedRange(int offset, int length) {
    // TODO Auto-generated method stub

  }

  @Override
  public Point getSelectedRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ISelectionProvider getSelectionProvider() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void revealRange(int offset, int length) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setTopIndex(int index) {
    // TODO Auto-generated method stub

  }

  @Override
  public int getTopIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getTopIndexStartOffset() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getBottomIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getBottomIndexEndOffset() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getTopInset() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void configure(SourceViewerConfiguration configuration) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAnnotationHover(IAnnotationHover annotationHover) {
    // TODO Auto-generated method stub

  }

  @Override
  public IAnnotationModel getAnnotationModel() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setRangeIndicator(Annotation rangeIndicator) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setRangeIndication(int offset, int length, boolean moveCursor) {
    // TODO Auto-generated method stub

  }

  @Override
  public IRegion getRangeIndication() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeRangeIndication() {
    // TODO Auto-generated method stub

  }

  @Override
  public void showAnnotations(boolean show) {
    // TODO Auto-generated method stub

  }

}
