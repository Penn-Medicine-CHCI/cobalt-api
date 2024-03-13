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

package com.cobaltplatform.api.integration.hl7.model.segment;

import ca.uhn.hl7v2.model.v251.segment.NTE;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/Segments/NTE
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7NotesAndCommentsSegment extends Hl7Object {
	@Nullable
	private Integer setId; // NTE.1 - Set ID
	@Nullable
	private String sourceOfComment; // NTE.2 - Source of Comment
	@Nullable
	private List<String> comment; // NTE.3 - Comment
	@Nullable
	private Hl7CodedElement commentType; // NTE.4 - Comment Type

	@Nonnull
	public static Boolean isPresent(@Nullable NTE nte) {
		if (nte == null)
			return false;

		return trimToNull(nte.getSetIDNTE().getValue()) != null
				|| trimToNull(nte.getSourceOfComment().getValueOrEmpty()) != null
				|| (nte.getComment() != null && nte.getComment().length > 0)
				|| Hl7CodedElement.isPresent(nte.getCommentType());
	}

	public Hl7NotesAndCommentsSegment() {
		// Nothing to do
	}

	public Hl7NotesAndCommentsSegment(@Nullable NTE nte) {
		if (nte != null) {
			String setIdAsString = trimToNull(nte.getSetIDNTE().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			this.sourceOfComment = trimToNull(nte.getSourceOfComment().getValueOrEmpty());

			if (nte.getComment() != null && nte.getComment().length > 0)
				setComment(Arrays.stream(nte.getComment())
						.map(comment -> trimToNull(comment.getValueOrEmpty()))
						.filter(comment -> comment != null)
						.collect(Collectors.toList()));
			else
				setComment(List.of());

			if (Hl7CodedElement.isPresent(nte.getCommentType()))
				this.commentType = new Hl7CodedElement(nte.getCommentType());
		}
	}

	@Nullable
	public Integer getSetId() {
		return this.setId;
	}

	public void setSetId(@Nullable Integer setId) {
		this.setId = setId;
	}

	@Nullable
	public String getSourceOfComment() {
		return this.sourceOfComment;
	}

	public void setSourceOfComment(@Nullable String sourceOfComment) {
		this.sourceOfComment = sourceOfComment;
	}

	@Nullable
	public List<String> getComment() {
		return this.comment;
	}

	public void setComment(@Nullable List<String> comment) {
		this.comment = comment;
	}

	@Nullable
	public Hl7CodedElement getCommentType() {
		return this.commentType;
	}

	public void setCommentType(@Nullable Hl7CodedElement commentType) {
		this.commentType = commentType;
	}
}