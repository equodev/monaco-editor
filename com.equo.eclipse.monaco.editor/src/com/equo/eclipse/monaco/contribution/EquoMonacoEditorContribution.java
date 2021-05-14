package com.equo.eclipse.monaco.contribution;

import static com.equo.eclipse.monaco.contribution.IMonacoConstants.BASE_HTML_FILE;
import static com.equo.eclipse.monaco.contribution.IMonacoConstants.EQUO_MONACO_CONTRIBUTION_NAME;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.equo.contribution.api.EquoContributionBuilder;
import com.equo.contribution.api.resolvers.EquoGenericURLResolver;

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
				.withURLResolver(new EquoGenericURLResolver(EquoMonacoEditorContribution.class.getClassLoader())) //
				.build();
	}
}