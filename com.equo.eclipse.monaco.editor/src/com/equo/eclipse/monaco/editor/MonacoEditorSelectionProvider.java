package com.equo.eclipse.monaco.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class MonacoEditorSelectionProvider implements ISelectionProvider {
	
	private List<ISelectionChangedListener> listeners = new ArrayList<>();
	
	private TextSelection selection = new TextSelection(0,-1);

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
