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

import java.util.concurrent.Callable;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * An action to be executed through handlers.
 */
public class EditorAction extends Action {

  private Runnable execution;
  private Callable<Boolean> callableIsEnabled;

  public EditorAction(Runnable execution) {
    this(execution, null, null);
  }

  public EditorAction(Runnable execution, ISelectionProvider selectionProvider) {
    this(execution, selectionProvider, null);
  }

  /**
   * Parameterized constructor.
   */
  public EditorAction(Runnable execution, ISelectionProvider selectionProvider,
      Callable<Boolean> callableIsEnabled) {
    this.execution = execution;
    this.callableIsEnabled = callableIsEnabled;
    setEnabled(false);
    if (selectionProvider != null) {
      selectionProvider.addSelectionChangedListener(new SelectionListener());
    }
  }

  @Override
  public boolean isEnabled() {
    if (callableIsEnabled != null) {
      try {
        return callableIsEnabled.call();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return super.isEnabled();
  }

  @Override
  public void run() {
    execution.run();
  }

  private class SelectionListener implements ISelectionChangedListener {

    @Override
    public void selectionChanged(SelectionChangedEvent e) {
      setEnabled(!e.getSelection().isEmpty());
    }

  }

}
