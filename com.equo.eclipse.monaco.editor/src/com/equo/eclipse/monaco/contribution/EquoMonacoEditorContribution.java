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

package com.equo.eclipse.monaco.contribution;

import static com.equo.eclipse.monaco.contribution.IMonacoConstants.BASE_HTML_FILE;
import static com.equo.eclipse.monaco.contribution.IMonacoConstants.EQUO_MONACO_CONTRIBUTION_NAME;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.contribution.api.EquoContributionBuilder;
import com.equo.contribution.api.resolvers.EquoGenericUrlResolver;

/**
 * Contributes a generated page with the editor inside.
 */
@Component
public class EquoMonacoEditorContribution {

  @Reference
  private EquoContributionBuilder builder;

  @Activate
  protected void activate() {
    builder //
        .withBaseHtmlResource(BASE_HTML_FILE) //
        .withContributionName(EQUO_MONACO_CONTRIBUTION_NAME) //
        .withPathWithScript("", "index.bundle.js") //
        .withUrlResolver(
            new EquoGenericUrlResolver(EquoMonacoEditorContribution.class.getClassLoader())) //
        .build();
  }
}
