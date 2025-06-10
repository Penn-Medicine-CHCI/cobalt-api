/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.Video;
import com.cobaltplatform.api.model.db.VideoVendor.VideoVendorId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class VideoApiResponse {
	@Nonnull
	private final UUID videoId;
	@Nonnull
	private final VideoVendorId videoVendorId;
	@Nullable
	private final String youtubeId; // only applicable to YOUTUBE vendor type
	@Nullable
	private final String kalturaPartnerId; // only applicable to KALTURA vendor type
	@Nullable
	private final String kalturaUiconfId; // only applicable to KALTURA vendor type
	@Nullable
	private final String kalturaWid; // only applicable to KALTURA vendor type
	@Nullable
	private final String kalturaEntryId; // only applicable to KALTURA vendor type
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface VideoApiResponseFactory {
		@Nonnull
		VideoApiResponse create(@Nonnull Video video);
	}

	@AssistedInject
	public VideoApiResponse(@Nonnull Formatter formatter,
													@Nonnull Strings strings,
													@Assisted @Nonnull Video video) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(video);

		this.videoId = video.getVideoId();
		this.videoVendorId = video.getVideoVendorId();
		this.youtubeId = video.getYoutubeId();
		this.kalturaPartnerId = video.getKalturaPartnerId();
		this.kalturaUiconfId = video.getKalturaUiconfId();
		this.kalturaWid = video.getKalturaWid();
		this.kalturaEntryId = video.getKalturaEntryId();
		this.created = video.getCreated();
		this.createdDescription = formatter.formatTimestamp(video.getCreated());
		this.lastUpdated = video.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(video.getLastUpdated());
	}

	@Nonnull
	public UUID getVideoId() {
		return this.videoId;
	}

	@Nonnull
	public VideoVendorId getVideoVendorId() {
		return this.videoVendorId;
	}

	@Nonnull
	public Optional<String> getYoutubeId() {
		return Optional.ofNullable(this.youtubeId);
	}

	@Nonnull
	public Optional<String> getKalturaPartnerId() {
		return Optional.ofNullable(this.kalturaPartnerId);
	}

	@Nonnull
	public Optional<String> getKalturaUiconfId() {
		return Optional.ofNullable(this.kalturaUiconfId);
	}

	@Nonnull
	public Optional<String> getKalturaWid() {
		return Optional.ofNullable(this.kalturaWid);
	}

	@Nonnull
	public Optional<String> getKalturaEntryId() {
		return Optional.ofNullable(this.kalturaEntryId);
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}
}