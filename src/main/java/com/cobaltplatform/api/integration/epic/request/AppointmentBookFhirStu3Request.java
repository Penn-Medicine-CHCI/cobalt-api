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
public class AppointmentBookFhirStu3Request {
	@Nullable
	private String patient;
	@Nullable
	private String appointment;
	@Nullable
	private String appointmentNote;

	// {
	//   "resourceType":"Parameters",
	//   "parameter":[
	//      {
	//         "name":"patient",
	//         "valueIdentifier":{
	//            "value":"efvHwbc1k1CQ9XjM1zvvefQ3"
	//         }
	//      },
	//      {
	//         "name":"appointment",
	//         "valueIdentifier":{
	//            "value":"ezu6MfS.FpOXrHAn1eJHczv4LlH.fMIwtdkA8rsm-Yfu96eUh91EBd0UN9BZx7kbB3"
	//         }
	//      },
	//      {
	//         "name":"appointmentNote",
	//         "valueString":"Note text containing info related to the appointment."
	//      }
	//   ]
	// }

	@Nullable
	public String getPatient() {
		return this.patient;
	}

	public void setPatient(@Nullable String patient) {
		this.patient = patient;
	}

	@Nullable
	public String getAppointment() {
		return this.appointment;
	}

	public void setAppointment(@Nullable String appointment) {
		this.appointment = appointment;
	}

	@Nullable
	public String getAppointmentNote() {
		return this.appointmentNote;
	}

	public void setAppointmentNote(@Nullable String appointmentNote) {
		this.appointmentNote = appointmentNote;
	}
}
