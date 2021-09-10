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
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScheduleAppointmentWithInsuranceResponse {
	// {
	//   "Appointment":{
	//      "Time":"14:00:00",
	//      "DurationInMinutes":60,
	//      "Date":"2020-07-07",
	//      "PatientInstructions":[
	//         "You will receive instructions on how to connect via video by your provider's practice. "
	//      ],
	//      "Warnings":null,
	//      "Provider":{
	//         "DisplayName":"Cecilia Livesey, MD",
	//         "IDs":[
	//            {
	//               "ID":"R09651",
	//               "Type":"#29"
	//            },
	//            {
	//               "ID":"058636",
	//               "Type":"#23"
	//            },
	//            {
	//               "ID":"319940",
	//               "Type":"#22"
	//            },
	//            {
	//               "ID":"1629330709",
	//               "Type":"NPI"
	//            },
	//            {
	//               "ID":"51043384",
	//               "Type":"#32"
	//            },
	//            {
	//               "ID":"058636",
	//               "Type":"#26"
	//            },
	//            {
	//               "ID":"6584636050002",
	//               "Type":"SPI"
	//            },
	//            {
	//               "ID":"  R09651",
	//               "Type":"Internal"
	//            },
	//            {
	//               "ID":"R09651",
	//               "Type":"External"
	//            }
	//         ]
	//      },
	//      "Department":{
	//         "Name":"Penn Behavioral Health",
	//         "LocationInstructions":[
	//
	//         ],
	//         "IDs":[
	//            {
	//               "ID":"2084P0800X",
	//               "Type":"#4000"
	//            },
	//            {
	//               "ID":"PBH8MK+OPS",
	//               "Type":"#40"
	//            },
	//            {
	//               "ID":"603",
	//               "Type":"#116"
	//            },
	//            {
	//               "ID":"PBH8MK",
	//               "Type":"#36"
	//            },
	//            {
	//               "ID":"1821486846",
	//               "Type":"#3"
	//            },
	//            {
	//               "ID":"19104765",
	//               "Type":"#210402"
	//            },
	//            {
	//               "ID":"37140555",
	//               "Type":"#210403"
	//            },
	//            {
	//               "ID":"PBOP",
	//               "Type":"CernerOut"
	//            },
	//            {
	//               "ID":"PBOP",
	//               "Type":"CernerIn"
	//            },
	//            {
	//               "ID":"603",
	//               "Type":"Internal"
	//            },
	//            {
	//               "ID":"603",
	//               "Type":"External"
	//            }
	//         ],
	//         "Address":{
	//            "StreetAddress":[
	//               "3535 Market Street",
	//               "2nd Floor"
	//            ],
	//            "City":"Philadelphia",
	//            "PostalCode":"19104-3317",
	//            "HouseNumber":"",
	//            "State":{
	//               "Number":"39",
	//               "Title":"Pennsylvania",
	//               "Abbreviation":"PA"
	//            },
	//            "Country":{
	//               "Number":"",
	//               "Title":"",
	//               "Abbreviation":""
	//            },
	//            "District":{
	//               "Number":"",
	//               "Title":"",
	//               "Abbreviation":""
	//            },
	//            "County":{
	//               "Number":"",
	//               "Title":"",
	//               "Abbreviation":""
	//            }
	//         },
	//         "Specialty":{
	//            "Number":"37",
	//            "Title":"Psychiatry",
	//            "Abbreviation":"Psychiatry",
	//            "ExternalName":""
	//         },
	//         "OfficialTimeZone":{
	//            "Number":"7",
	//            "Title":"America/New_York",
	//            "Abbreviation":"America/NYC"
	//         },
	//         "Phones":[
	//            {
	//               "Type":"General",
	//               "Number":"215-746-6701"
	//            },
	//            {
	//               "Type":"Scheduling",
	//               "Number":""
	//            }
	//         ]
	//      },
	//      "VisitType":{
	//         "Name":"COBALT TELEHEALTH NEW",
	//         "DisplayName":"Telehealth Visit",
	//         "PatientInstructions":[
	//
	//         ],
	//         "IDs":[
	//            {
	//               "ID":"    3602",
	//               "Type":"Internal"
	//            },
	//            {
	//               "ID":"3602",
	//               "Type":"External"
	//            }
	//         ]
	//      },
	//      "Patient":{
	//         "Name":"Mark Allen",
	//         "IDs":[
	//            {
	//               "ID":"14103447",
	//               "Type":"EPI"
	//            },
	//            {
	//               "ID":"059977827",
	//               "Type":"HUP MRN"
	//            },
	//            {
	//               "ID":"8000181775",
	//               "Type":"UID"
	//            },
	//            {
	//               "ID":"059977827",
	//               "Type":"HUP SMS MRN"
	//            },
	//            {
	//               "ID":"330441825",
	//               "Type":"PPMC MRN"
	//            },
	//            {
	//               "ID":"330441825",
	//               "Type":"PAH MRN"
	//            },
	//            {
	//               "ID":"330441825",
	//               "Type":"DCR MRN"
	//            },
	//            {
	//               "ID":"371870009",
	//               "Type":"CCH MRN"
	//            },
	//            {
	//               "ID":"376778000",
	//               "Type":"MCP MRN"
	//            },
	//            {
	//               "ID":"    Z17120",
	//               "Type":"Internal"
	//            },
	//            {
	//               "ID":"Z17120",
	//               "Type":"External"
	//            }
	//         ]
	//      },
	//      "ContactIDs":[
	//         {
	//            "ID":"55964",
	//            "Type":"DAT"
	//         },
	//         {
	//            "ID":"253179709",
	//            "Type":"ASN"
	//         },
	//         {
	//            "ID":"253179709",
	//            "Type":"CSN"
	//         },
	//         {
	//            "ID":"",
	//            "Type":"UCI"
	//         }
	//      ]
	//   }
	// }

	@Nullable
	private Appointment Appointment;

	@NotThreadSafe
	public static class Appointment {
		// TODO: there are other fields we can map, but Contact IDs are all we care about for now

		@Nullable
		private List<ContactID> ContactIDs;

		@NotThreadSafe
		public static class ContactID {
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
		public List<ContactID> getContactIDs() {
			return ContactIDs;
		}

		public void setContactIDs(@Nullable List<ContactID> contactIDs) {
			ContactIDs = contactIDs;
		}
	}

	@Nullable
	public ScheduleAppointmentWithInsuranceResponse.Appointment getAppointment() {
		return Appointment;
	}

	public void setAppointment(@Nullable ScheduleAppointmentWithInsuranceResponse.Appointment appointment) {
		Appointment = appointment;
	}
}
