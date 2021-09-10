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

package com.cobaltplatform.api.integration.epic.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CancelAppointmentRequest {
	@Nullable
	private String Reason;
	@Nullable
	private Patient Patient;
	@Nullable
	private Contact Contact;

	@NotThreadSafe
	public static class Patient {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
	}

	@NotThreadSafe
	public static class Contact {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
	}

	@Nullable
	public String getReason() {
		return Reason;
	}

	public void setReason(@Nullable String reason) {
		Reason = reason;
	}

	@Nullable
	public CancelAppointmentRequest.Patient getPatient() {
		return Patient;
	}

	public void setPatient(@Nullable CancelAppointmentRequest.Patient patient) {
		Patient = patient;
	}

	@Nullable
	public CancelAppointmentRequest.Contact getContact() {
		return Contact;
	}

	public void setContact(@Nullable CancelAppointmentRequest.Contact contact) {
		Contact = contact;
	}
}
