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

package com.equo.monaco;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.comm.api.IEquoEventHandler;
import com.equo.filesystem.api.IEquoFileSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Allows the editor to be created in a standalone way, just as a javascript
 * component.
 */
@Component
public class EquoMonacoStandaloneEditor {

  private IEquoEventHandler equoEventHandler;

  @Reference
  private IEquoFileSystem equoFileSystem;

  public EquoMonacoStandaloneEditor() {
    super();
  }

  @Activate
  public void activate() {
    attendEditorCreation();
    attendLspConfig();
  }

  private void attendLspConfig() {
    equoEventHandler.on("_addLspServer", JsonObject.class, payload -> {
      List<String> executionParameters =
          getListFromJsonArray(payload.getAsJsonArray("executionParameters"));
      List<String> extensions = getListFromJsonArray(payload.getAsJsonArray("extensions"));
      EquoMonacoEditor.addLspServer(executionParameters, extensions);
    });

    equoEventHandler.on("_addLspWsServer", JsonObject.class, payload -> {
      String fullServerPath = payload.get("fullServerPath").getAsString();
      List<String> extensions = getListFromJsonArray(payload.getAsJsonArray("extensions"));
      EquoMonacoEditor.addLspWsServer(fullServerPath, extensions);
    });

    equoEventHandler.on("_removeLspServer", JsonObject.class, payload -> {
      List<String> extensions = getListFromJsonArray(payload.getAsJsonArray("extensions"));
      EquoMonacoEditor.removeLspServer(extensions);
    });
  }

  protected List<String> getListFromJsonArray(JsonArray array) {
    List<String> list = new ArrayList<>();
    for (JsonElement elem : array) {
      list.add(elem.getAsString());
    }
    return list;
  }

  private void attendEditorCreation() {
    equoEventHandler.on("_createEditor", JsonObject.class, payload -> {
      JsonElement jsonFilePath = payload.get("filePath");
      if (jsonFilePath != null) {
        String filePath = jsonFilePath.getAsString();
        File file = new File(filePath);
        String content = equoFileSystem.readFile(new File(filePath));
        if (content != null) {
          new EquoMonacoEditor(equoEventHandler, equoFileSystem).initialize(content, file.getName(),
              filePath);
        } else {
          if (!file.exists()) {
            new EquoMonacoEditor(equoEventHandler, equoFileSystem).initialize("", file.getName(),
                filePath);
          }
        }
      } else {
        new EquoMonacoEditor(equoEventHandler, equoFileSystem).initialize("", "", "");
      }
    });
  }

  @Reference
  public void setEquoEventHandler(IEquoEventHandler handler) {
    equoEventHandler = handler;
  }

  public void unsetEquoEventHandler(IEquoEventHandler handler) {
    equoEventHandler = null;
  }

}
