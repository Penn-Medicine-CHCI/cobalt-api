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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Encounter {
	// Example:
	//
	// {
	//     "resource": {
	//         "resourceType": "Encounter",
	//         "id": "eesgHbkMSaKN2lvzd1huD7Q3",
	//         "extension": [
	//             {
	//                 "valueBoolean": false,
	//                 "url": "http://open.epic.com/FHIR/StructureDefinition/extension/accidentrelated"
	//             }
	//         ],
	//         "identifier": [
	//             {
	//                 "use": "usual",
	//                 "system": "urn:oid:1.2.840.114350.1.13.87.3.7.3.698084.8",
	//                 "value": "200099550"
	//             }
	//         ],
	//         "status": "finished",
	//         "class": {
	//             "system": "urn:oid:1.2.840.114350.1.72.1.7.7.10.696784.13260",
	//             "code": "5",
	//             "display": "Appointment"
	//         },
	//         "type": [
	//             {
	//                 "coding": [
	//                     {
	//                         "system": "urn:oid:1.2.840.114350.1.13.87.3.7.10.698084.10110",
	//                         "code": "145",
	//                         "display": "MAPS"
	//                     }
	//                 ],
	//                 "text": "MAPS"
	//             },
	//             {
	//                 "coding": [
	//                     {
	//                         "system": "urn:oid:1.2.840.114350.1.13.87.3.7.10.698084.30",
	//                         "code": "50",
	//                         "display": "Appointment"
	//                     }
	//                 ],
	//                 "text": "Appointment"
	//             },
	//             {
	//                 "coding": [
	//                     {
	//                         "system": "urn:oid:1.2.840.114350.1.13.87.3.7.2.808267",
	//                         "code": "1016",
	//                         "display": "Return Patient Visit"
	//                     }
	//                 ],
	//                 "text": "Return Patient Visit"
	//             },
	//             {
	//                 "coding": [
	//                     {
	//                         "system": "urn:oid:1.2.840.114350.1.13.87.3.7.10.698084.18875",
	//                         "code": "13",
	//                         "display": "Routine Elective Admission"
	//                     }
	//                 ],
	//                 "text": "Routine Elective Admission"
	//             }
	//         ],
	//         "serviceType": {
	//             "coding": [
	//                 {
	//                     "system": "urn:oid:1.2.840.114350.1.13.87.3.7.10.698084.18886",
	//                     "code": "5",
	//                     "display": "MEDICINE"
	//                 }
	//             ],
	//             "text": "MEDICINE"
	//         },
	//         "subject": {
	//             "reference": "Patient/eEMS.-CbIiYrvQbAjuUYABA3",
	//             "display": "Pbtest, Aetna"
	//         },
	//         "participant": [
	//             {
	//                 "type": [
	//                     {
	//                         "coding": [
	//                             {
	//                                 "system": "http://hl7.org/fhir/v3/ParticipationType",
	//                                 "code": "REF",
	//                                 "display": "referrer"
	//                             }
	//                         ],
	//                         "text": "referrer"
	//                     }
	//                 ],
	//                 "individual": {
	//                     "reference": "Practitioner/eXcPZsv6PvPVzl5xclP5rcA3",
	//                     "type": "Practitioner",
	//                     "display": "Gary Crooks, MD"
	//                 }
	//             },
	//             {
	//                 "period": {
	//                     "start": "2022-12-12T15:00:00Z",
	//                     "end": "2022-12-12T15:15:00Z"
	//                 },
	//                 "individual": {
	//                     "reference": "Practitioner/eQlKSts7gWofql1k53G-DUg3",
	//                     "display": "Matthew C Miller, MD"
	//                 }
	//             }
	//         ],
	//         "period": {
	//             "start": "2022-12-12T15:00:00Z",
	//             "end": "2022-12-12T15:15:00Z"
	//         },
	//         "account": [
	//             {
	//                 "identifier": {
	//                     "system": "urn:oid:1.2.840.114350.1.13.87.3.7.2.726582",
	//                     "value": "800000058610"
	//                 },
	//                 "display": "PBTEST,AETNA"
	//             }
	//         ],
	//         "hospitalization": {
	//             "admitSource": {
	//                 "coding": [
	//                     {
	//                         "system": "urn:oid:1.2.840.114350.1.13.87.3.7.10.698084.10310",
	//                         "code": "3",
	//                         "display": "RA-Routine Admission Sched/Booked"
	//                     }
	//                 ],
	//                 "text": "RA-Routine Admission Sched/Booked"
	//             }
	//         },
	//         "location": [
	//             {
	//                 "location": {
	//                     "reference": "Location/ehwSFOP0P9Rq64OW2QxycWg3",
	//                     "display": "GENERAL INTERNAL MEDICINE PMR"
	//                 }
	//             }
	//         ]
	//     }
	// }

	@Nullable
	private String csn;
	@Nullable
	private String status;
	@Nullable
	private String subjectDisplay;
	@Nullable
	private String classDisplay;
	@Nullable
	private String serviceTypeText;
	@Nullable
	private LocalDateTime periodStart;
	@Nullable
	private LocalDateTime periodEnd;

	@Nullable
	public String getCsn() {
		return this.csn;
	}

	public void setCsn(@Nullable String csn) {
		this.csn = csn;
	}

	@Nullable
	public String getStatus() {
		return this.status;
	}

	public void setStatus(@Nullable String status) {
		this.status = status;
	}

	@Nullable
	public String getSubjectDisplay() {
		return this.subjectDisplay;
	}

	public void setSubjectDisplay(@Nullable String subjectDisplay) {
		this.subjectDisplay = subjectDisplay;
	}

	@Nullable
	public String getClassDisplay() {
		return this.classDisplay;
	}

	public void setClassDisplay(@Nullable String classDisplay) {
		this.classDisplay = classDisplay;
	}

	@Nullable
	public String getServiceTypeText() {
		return this.serviceTypeText;
	}

	public void setServiceTypeText(@Nullable String serviceTypeText) {
		this.serviceTypeText = serviceTypeText;
	}

	@Nullable
	public LocalDateTime getPeriodStart() {
		return this.periodStart;
	}

	public void setPeriodStart(@Nullable LocalDateTime periodStart) {
		this.periodStart = periodStart;
	}

	@Nullable
	public LocalDateTime getPeriodEnd() {
		return this.periodEnd;
	}

	public void setPeriodEnd(@Nullable LocalDateTime periodEnd) {
		this.periodEnd = periodEnd;
	}
}
