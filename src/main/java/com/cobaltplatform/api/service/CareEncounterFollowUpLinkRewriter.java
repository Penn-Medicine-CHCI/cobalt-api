package com.cobaltplatform.api.service;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;

/** Rewrites sanitized follow-up links after the message is scheduled. */
final class CareEncounterFollowUpLinkRewriter {
	private static final Pattern LINK_HREF_PATTERN = Pattern.compile("(?i)(<a\\b[^>]*?\\bhref=\")([^\"]+)(\")");

	private CareEncounterFollowUpLinkRewriter() {}

	@Nonnull
	static String rewrite(@Nonnull String sanitizedHtml, @Nonnull String patientWebappBaseUrl,
								@Nonnull Function<String, UUID> recordLink) {
		requireNonNull(sanitizedHtml);
		requireNonNull(patientWebappBaseUrl);
		requireNonNull(recordLink);
		Matcher matcher = LINK_HREF_PATTERN.matcher(sanitizedHtml);
		StringBuffer rewritten = new StringBuffer();
		while (matcher.find()) {
			String destinationUrl = unescapeHtml4(matcher.group(2));
			if (!destinationUrl.startsWith("https://")) {
				matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group()));
				continue;
			}
			UUID linkId = recordLink.apply(destinationUrl);
			String trackedUrl = patientWebappBaseUrl.replaceAll("/+$", "")
					+ "/care-encounter-links/" + linkId + "/redirect";
			matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group(1) + trackedUrl + matcher.group(3)));
		}
		matcher.appendTail(rewritten);
		return rewritten.toString();
	}
}
