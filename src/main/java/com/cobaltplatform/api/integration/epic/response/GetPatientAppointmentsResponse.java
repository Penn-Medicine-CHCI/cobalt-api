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
public class GetPatientAppointmentsResponse {
	@Nullable
	private String Error;
	@Nullable
	private List<Appointment> Appointments;

	// {
	//   "Error":null,
	//   "Appointments":[
	//      {
	//         "PatientName":"ALLEN,MARK A",
	//         "Date":"7/17/2020",
	//         "VisitTypeName":"COBALT TELEHEALTH RETURN",
	//         "AppointmentNotes":[
	//            "Videoconference URL: https://bluejeans.com/716071919"
	//         ],
	//         "AppointmentStartTime":" 9:15 AM",
	//         "AppointmentDuration":"30",
	//         "AppointmentStatus":"Scheduled",
	//         "PatientIDs":[
	//            {
	//               "ID":"  Z4495516",
	//               "Type":"Internal"
	//            },
	//            {
	//               "ID":"18581922",
	//               "Type":"ExternalKey"
	//            }
	//         ],
	//         "ContactIDs":[
	//            {
	//               "ID":"256594373",
	//               "Type":"CSN"
	//            },
	//            {
	//               "ID":"256594373",
	//               "Type":"ASN"
	//            }
	//         ],
	//         "VisitTypeIDs":[
	//            {
	//               "ID":"    3604",
	//               "Type":"Internal"
	//            },
	//            {
	//               "ID":"15415407",
	//               "Type":"CID"
	//            }
	//         ],
	//         "Providers":[
	//            {
	//               "ProviderName":"Livesey, Cecilia, MD",
	//               "DepartmentName":"PBH OPC 3535 MARKET ST 2ND FLOOR",
	//               "Time":" 9:15 AM",
	//               "Duration":"30",
	//               "ProviderIDs":[
	//                  {
	//                     "ID":"  R09651",
	//                     "Type":"Internal"
	//                  },
	//                  {
	//                     "ID":"15459732",
	//                     "Type":"CID"
	//                  }
	//               ],
	//               "DepartmentIDs":[
	//                  {
	//                     "ID":"603",
	//                     "Type":"Internal"
	//                  },
	//                  {
	//                     "ID":"151799",
	//                     "Type":"CID"
	//                  }
	//               ]
	//            }
	//         ],
	//         "ExtraItems":[
	//            {
	//               "ItemNumber":"7040",
	//               "Value":null,
	//               "Lines":[
	//                  {
	//                     "LineNumber":0,
	//                     "Value":"1",
	//                     "Sublines":null
	//                  },
	//                  {
	//                     "LineNumber":1,
	//                     "Value":"R09651",
	//                     "Sublines":null
	//                  }
	//               ]
	//            },
	//            {
	//               "ItemNumber":"7050",
	//               "Value":null,
	//               "Lines":[
	//                  {
	//                     "LineNumber":0,
	//                     "Value":"1",
	//                     "Sublines":null
	//                  },
	//                  {
	//                     "LineNumber":1,
	//                     "Value":"Videoconference URL: https://bluejeans.com/716071919",
	//                     "Sublines":null
	//                  }
	//               ]
	//            },
	//            {
	//               "ItemNumber":"28006",
	//               "Value":"Non Standard MyChart Status",
	//               "Lines":null
	//            }
	//         ],
	//         "ExtraExtensions":[
	//
	//         ]
	//      }
	//   ]
	//}

	@NotThreadSafe
	public static class Appointment {
		@Nullable
		private String PatientName;
		@Nullable
		private String Date;
		@Nullable
		private String VisitTypeName;
		@Nullable
		private List<String> AppointmentNotes;
		@Nullable
		private String AppointmentStartTime;
		@Nullable
		private String AppointmentDuration;
		@Nullable
		private String AppointmentStatus;
		@Nullable
		private List<PatientID> PatientIDs;
		@Nullable
		private List<ContactID> ContactIDs;
		@Nullable
		private List<VisitTypeID> VisitTypeIDs;
		@Nullable
		private List<Provider> Providers;

		@NotThreadSafe
		public static class Provider {
			@Nullable
			private String ProviderName;
			@Nullable
			private String DepartmentName;
			@Nullable
			private String Time; // e.g. "11:30 AM"
			@Nullable
			private String Duration; // e.g. "30"
			@Nullable
			private List<ProviderID> ProviderIDs;
			@Nullable
			private List<DepartmentID> DepartmentIDs;

			@NotThreadSafe
			public static class ProviderID {
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
			public static class DepartmentID {
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
			public String getProviderName() {
				return ProviderName;
			}

			public void setProviderName(@Nullable String providerName) {
				ProviderName = providerName;
			}

			@Nullable
			public String getDepartmentName() {
				return DepartmentName;
			}

			public void setDepartmentName(@Nullable String departmentName) {
				DepartmentName = departmentName;
			}

			@Nullable
			public String getTime() {
				return Time;
			}

			public void setTime(@Nullable String time) {
				Time = time;
			}

			@Nullable
			public String getDuration() {
				return Duration;
			}

			public void setDuration(@Nullable String duration) {
				Duration = duration;
			}

			@Nullable
			public List<ProviderID> getProviderIDs() {
				return ProviderIDs;
			}

			public void setProviderIDs(@Nullable List<ProviderID> providerIDs) {
				ProviderIDs = providerIDs;
			}

			@Nullable
			public List<DepartmentID> getDepartmentIDs() {
				return DepartmentIDs;
			}

			public void setDepartmentIDs(@Nullable List<DepartmentID> departmentIDs) {
				DepartmentIDs = departmentIDs;
			}
		}

		@NotThreadSafe
		public static class PatientID {
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

		@NotThreadSafe
		public static class VisitTypeID {
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
		public String getPatientName() {
			return PatientName;
		}

		public void setPatientName(@Nullable String patientName) {
			PatientName = patientName;
		}

		@Nullable
		public String getDate() {
			return Date;
		}

		public void setDate(@Nullable String date) {
			Date = date;
		}

		@Nullable
		public String getVisitTypeName() {
			return VisitTypeName;
		}

		public void setVisitTypeName(@Nullable String visitTypeName) {
			VisitTypeName = visitTypeName;
		}

		@Nullable
		public List<String> getAppointmentNotes() {
			return AppointmentNotes;
		}

		public void setAppointmentNotes(@Nullable List<String> appointmentNotes) {
			AppointmentNotes = appointmentNotes;
		}

		@Nullable
		public String getAppointmentStartTime() {
			return AppointmentStartTime;
		}

		public void setAppointmentStartTime(@Nullable String appointmentStartTime) {
			AppointmentStartTime = appointmentStartTime;
		}

		@Nullable
		public String getAppointmentDuration() {
			return AppointmentDuration;
		}

		public void setAppointmentDuration(@Nullable String appointmentDuration) {
			AppointmentDuration = appointmentDuration;
		}

		@Nullable
		public String getAppointmentStatus() {
			return AppointmentStatus;
		}

		public void setAppointmentStatus(@Nullable String appointmentStatus) {
			AppointmentStatus = appointmentStatus;
		}

		@Nullable
		public List<PatientID> getPatientIDs() {
			return PatientIDs;
		}

		public void setPatientIDs(@Nullable List<PatientID> patientIDs) {
			PatientIDs = patientIDs;
		}

		@Nullable
		public List<ContactID> getContactIDs() {
			return ContactIDs;
		}

		public void setContactIDs(@Nullable List<ContactID> contactIDs) {
			ContactIDs = contactIDs;
		}

		@Nullable
		public List<VisitTypeID> getVisitTypeIDs() {
			return VisitTypeIDs;
		}

		public void setVisitTypeIDs(@Nullable List<VisitTypeID> visitTypeIDs) {
			VisitTypeIDs = visitTypeIDs;
		}

		@Nullable
		public List<Provider> getProviders() {
			return Providers;
		}

		public void setProviders(@Nullable List<Provider> providers) {
			Providers = providers;
		}
	}

	@Nullable
	public String getError() {
		return Error;
	}

	public void setError(@Nullable String error) {
		Error = error;
	}

	@Nullable
	public List<Appointment> getAppointments() {
		return Appointments;
	}

	public void setAppointments(@Nullable List<Appointment> appointments) {
		Appointments = appointments;
	}
}
