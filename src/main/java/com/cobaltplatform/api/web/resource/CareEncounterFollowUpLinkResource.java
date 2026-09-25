package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.RedirectResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Resource
@Singleton
@ThreadSafe
public class CareEncounterFollowUpLinkResource {
	@Nonnull private final DatabaseProvider databaseProvider;

	@Inject
	public CareEncounterFollowUpLinkResource(@Nonnull DatabaseProvider databaseProvider) {
		this.databaseProvider = requireNonNull(databaseProvider);
	}

	@Nonnull
	@GET("/care-encounter-links/{linkId}/redirect")
	public RedirectResponse redirect(@Nonnull @PathParameter UUID linkId) {
		requireNonNull(linkId);
		String destinationUrl = databaseProvider.get().executeReturning("""
			UPDATE care_encounter_follow_up_link link
			SET click_count=click_count+1,
			    first_clicked_at=COALESCE(first_clicked_at, now()),
			    last_clicked_at=now()
			FROM message_log log
			WHERE link.care_encounter_follow_up_link_id=?
			  AND log.message_id=link.message_id
			  AND log.processed IS NOT NULL
			RETURNING link.destination_url
			""", String.class, linkId).orElse(null);
		if (destinationUrl == null)
			destinationUrl = databaseProvider.get().queryForObject("""
				SELECT destination_url
				FROM care_encounter_follow_up_link
				WHERE care_encounter_follow_up_link_id=?
				""", String.class, linkId).orElse(null);
		if (destinationUrl == null)
			throw new NotFoundException();
		return new RedirectResponse(destinationUrl, RedirectResponse.Type.TEMPORARY);
	}
}
