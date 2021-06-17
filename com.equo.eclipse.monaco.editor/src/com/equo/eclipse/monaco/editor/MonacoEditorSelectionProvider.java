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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Implementation of SelectionProvider to be able to know when there is selected
 * text in the editor.
 */
public class MonacoEditorSelectionProvider implements ISelectionProvider {

  private List<ISelectionChangedListener> listeners = new ArrayList<>();

  private TextSelection selection = new TextSelection(0, -1);

  @Override
  public void addSelectionChangedListener(ISelectionChangedListener arg0) {
    listeners.add(arg0);
  }

  @Override
  public ISelection getSelection() {
    return selection;
  }

  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener arg0) {
    listeners.remove(arg0);
  }

  @Override
  public void setSelection(ISelection arg0) {
    if (arg0 instanceof TextSelection) {
      setSelectionVariable((TextSelection) arg0);
      for (ISelectionChangedListener l : listeners) {
        l.selectionChanged(new SelectionChangedEvent(this, selection));
      }
    }
  }

  private void setSelectionVariable(TextSelection selection) {
    if (selection.getLength() <= 0) {
      this.selection = new TextSelection(0, -1);
    } else {
      this.selection = selection;
    }
  }

}
