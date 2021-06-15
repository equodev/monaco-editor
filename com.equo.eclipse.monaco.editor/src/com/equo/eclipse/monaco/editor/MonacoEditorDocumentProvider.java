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

package com.equo.eclipse.monaco.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;

import com.equo.monaco.EquoMonacoEditor;

/**
 * Implementation of a DocumentProvider to provide editor content in an Eclipse
 * environment.
 */
public class MonacoEditorDocumentProvider implements IDocumentProvider {

  protected EquoMonacoEditor editor;

  public MonacoEditorDocumentProvider(EquoMonacoEditor editor) {
    this.editor = editor;
  }

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
    return new Document(editor.getContentsSync());
  }

  @Override
  public void resetDocument(Object element) throws CoreException {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveDocument(IProgressMonitor monitor, Object element, IDocument document,
      boolean overwrite) throws CoreException {
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

}
