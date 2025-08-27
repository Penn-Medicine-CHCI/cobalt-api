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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.VideoVendor.VideoVendorId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Video {
	@Nullable
	private UUID videoId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private VideoVendorId videoVendorId;
	@Nullable
	private String youtubeId; // Only applies to VideoVendorId.YOUTUBE
	@Nullable
	private String kalturaPartnerId; // Only applies to VideoVendorId.KALTURA
	@Nullable
	private String kalturaUiconfId; // Only applies to VideoVendorId.KALTURA
	@Nullable
	private String kalturaWid; // Only applies to VideoVendorId.KALTURA
	@Nullable
	private String kalturaEntryId; // Only applies to VideoVendorId.KALTURA
	@Nullable
	private String kalturaPlaylistId; // Only applies to VideoVendorId.KALTURA
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getVideoId() {
		return this.videoId;
	}

	public void setVideoId(@Nullable UUID videoId) {
		this.videoId = videoId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public VideoVendorId getVideoVendorId() {
		return this.videoVendorId;
	}

	public void setVideoVendorId(@Nullable VideoVendorId videoVendorId) {
		this.videoVendorId = videoVendorId;
	}

	@Nullable
	public String getYoutubeId() {
		return this.youtubeId;
	}

	public void setYoutubeId(@Nullable String youtubeId) {
		this.youtubeId = youtubeId;
	}

	@Nullable
	public String getKalturaPartnerId() {
		return this.kalturaPartnerId;
	}

	public void setKalturaPartnerId(@Nullable String kalturaPartnerId) {
		this.kalturaPartnerId = kalturaPartnerId;
	}

	@Nullable
	public String getKalturaUiconfId() {
		return this.kalturaUiconfId;
	}

	public void setKalturaUiconfId(@Nullable String kalturaUiconfId) {
		this.kalturaUiconfId = kalturaUiconfId;
	}

	@Nullable
	public String getKalturaWid() {
		return this.kalturaWid;
	}

	public void setKalturaWid(@Nullable String kalturaWid) {
		this.kalturaWid = kalturaWid;
	}

	@Nullable
	public String getKalturaEntryId() {
		return this.kalturaEntryId;
	}

	public void setKalturaEntryId(@Nullable String kalturaEntryId) {
		this.kalturaEntryId = kalturaEntryId;
	}

	@Nullable
	public String getKalturaPlaylistId() {
		return this.kalturaPlaylistId;
	}

	public void setKalturaPlaylistId(@Nullable String kalturaPlaylistId) {
		this.kalturaPlaylistId = kalturaPlaylistId;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}