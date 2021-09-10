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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientCreateFhirRequest {

	// {
	//   "resourceType":"Patient",
	//   "identifier":[
	//      {
	//         "use":"usual",
	//         "system":"urn:oid:2.16.840.1.113883.4.1",
	//         "value":"111-11-1111"
	//      }
	//   ],
	//   "name":[
	//      {
	//         "use":"usual",
	//         "text":"First Lastname",
	//         "family":"Lastname",
	//         "given":[
	//            "Create"
	//         ]
	//      }
	//   ],
	//   "gender":"male",
	//   "birthDate":"1970-01-31",
	//   "generalPractitioner":[
	//      {
	//         "reference":"https://example.org/api/FHIR/R4/Practitioner/e89p8GCwg7wvke-vxglQ0WA3",
	//         "display":"Amber Hxl, MD"
	//      }
	//   ]
	// }
}
