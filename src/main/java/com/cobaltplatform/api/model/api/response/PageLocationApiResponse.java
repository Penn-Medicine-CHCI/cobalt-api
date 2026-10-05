package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.PageLocation;
import com.cobaltplatform.api.model.db.PageLocation.PageLocationId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Immutable
public class PageLocationApiResponse {
	@Nonnull
	private final PageLocationId pageLocationId;
	@Nonnull
	private final String description;
	@Nullable
	private final String directoryRelativeUrl;

	public PageLocationApiResponse(@Nonnull PageLocation pageLocation) {
		requireNonNull(pageLocation);
		this.pageLocationId = pageLocation.getPageLocationId();
		this.description = pageLocation.getDescription();
		this.directoryRelativeUrl = pageLocation.getDirectoryRelativeUrl();
	}

	@Nonnull
	public PageLocationId getPageLocationId() {
		return this.pageLocationId;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public Optional<String> getDirectoryRelativeUrl() {
		return Optional.ofNullable(this.directoryRelativeUrl);
	}
}
