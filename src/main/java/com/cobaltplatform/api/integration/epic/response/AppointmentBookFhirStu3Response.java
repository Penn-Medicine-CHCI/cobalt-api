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

package com.cobaltplatform.api.integration.epic.response;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentBookFhirStu3Response {
	@Nullable
	private String rawJson;

	// TODO
	//
	// {
	//   "resourceType":"Appointment",
	//   "id":"eU32HphPn1VaOoKuco8Q56SVsfdrB3fsVeoLAjhpvLY43",
	//   "identifier":[
	//      {
	//         "system":"urn:oid:1.2.840.114350.1.13.861.1.7.3.698084.8",
	//         "value":"10158405185"
	//      }
	//   ],
	//   "status":"booked",
	//   "serviceType":[
	//      {
	//         "coding":[
	//            {
	//               "system":"urn:oid:1.2.840.114350.1.13.861.1.7.2.808267",
	//               "code":"225",
	//               "display":"Physical Therapy Visit"
	//            }
	//         ]
	//      }
	//   ],
	//   "start":"2018-08-04T00:00:00Z",
	//   "end":"2018-08-04T00:30:00Z",
	//   "minutesDuration":30,
	//   "participant":[
	//      {
	//         "actor":{
	//            "reference":"https://hostname/instance/api/FHIR/STU3/Patient/efvHwbc1k1CQ9XjM1zvvefQ3",
	//            "display":"CDS,Fhir"
	//         },
	//         "status":"accepted"
	//      },
	//      {
	//         "actor":{
	//            "reference":"https://hostname/instance/api/FHIR/STU3/Practitioner/edNKJE8VjTF.I8BInZ7jSn5nAYzw4T3TaxTbsCoM.lgY3",
	//            "display":"Jones, Alex, MD"
	//         },
	//         "status":"accepted"
	//      },
	//      {
	//         "actor":{
	//            "reference":"https://hostname/instance/api/FHIR/STU3/Location/e6mKSqgx8ytPcbu-A4F0TVA3",
	//            "display":"INITIAL DEPARTMENT"
	//         },
	//         "status":"accepted"
	//      }
	//   ]
	// }

	@Nullable
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}
}
