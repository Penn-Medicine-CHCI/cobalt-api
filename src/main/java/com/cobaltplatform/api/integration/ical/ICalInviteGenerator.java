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

package com.cobaltplatform.api.integration.ical;

import com.cobaltplatform.api.Configuration;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.SentBy;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Singleton
public class ICalInviteGenerator {
	static {
		// If we don't specify, fails at runtime with this:
		// java.lang.RuntimeException: Error loading default cache implementation. Please ensure the JCache API dependency is included in the classpath, or override the cache implementation (e.g. via configuration: net.fortuna.ical4j.timezone.cache.impl=net.fortuna.ical4j.util.MapTimeZoneCache)
		//
		// ...we don't want a JCache dependency for no good reason, so just use the built-in MapTimeZoneCache instead
		System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
	}

	// See https://ical4j.github.io/ical4j-user-guide/examples/

	// For reference, this is how an ical event looks when generated via Acuity
	//
	// BEGIN:VCALENDAR
	// PRODID:-//54.186.191.193//NONSGML iCalcreator 2.6//
	// VERSION:2.0
	// BEGIN:VEVENT
	// UID:429776497@scheduling
	// DTSTAMP:20200827T113908Z
	// DESCRIPTION:Client time zone: America/New_York\nName: Anonymous User\nPhone
	//  : \nEmail: agrayson@xmog.com\n\nLocation\n============\nhttps://bluejeans.
	//  com/487477269\n\n\n\n============\nVideoconference Link: https://bluejeans
	//  .com/487477269\n\n\nChange Appointment: https://secure.acuityscheduling.co
	//  m/appointments/view/429776497\n
	// DTSTART:20200826T183000Z
	// DTEND:20200826T191500Z
	// LOCATION:https://bluejeans.com/487477269
	// SUMMARY:Anonymous User: Xmog Class (Dr. Mark Allen)
	// END:VEVENT
	// END:VCALENDAR
	//
	// This implementation generates events like the following:
	//
	// BEGIN:VCALENDAR
	// PRODID:-//Events Calendar//iCal4j 1.0//EN
	// CALSCALE:GREGORIAN
	// BEGIN:VEVENT
	// DTSTAMP:20200827T125520Z
	// DTSTART:20200917T170000
	// DTEND:20200917T194500
	// SUMMARY:Test Session
	// TZID:America/New_York
	// UID:ba54e3be-bbc7-4f24-8292-e40d1e8918bb
	// LOCATION:https://www.xmog.com
	// DESCRIPTION:This is an example session\n\nJoin videoconference: https://www.xmog.com
	// END:VEVENT
	// END:VCALENDAR

	@Nonnull
	private final Configuration configuration;

	@Inject
	public ICalInviteGenerator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);
		this.configuration = configuration;
	}

	@Nonnull
	public String generateInvite(@Nonnull String uniqueIdentifier,
															 @Nonnull String title,
															 @Nonnull String description,
															 @Nonnull LocalDateTime startDateTime,
															 @Nonnull LocalDateTime endDateTime,
															 @Nonnull ZoneId timeZone,
															 @Nonnull String location,
															 @Nonnull InviteMethod inviteMethod,
															 @Nonnull InviteOrganizer inviteOrganizer,
															 @Nonnull InviteAttendee inviteAttendee) {
		requireNonNull(uniqueIdentifier);
		requireNonNull(title);
		requireNonNull(description);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(timeZone);
		requireNonNull(location);
		requireNonNull(inviteMethod);
		requireNonNull(inviteOrganizer);
		requireNonNull(inviteAttendee);

		TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone icalTimeZone = timeZoneRegistry.getTimeZone(timeZone.getId());
		VTimeZone icalVTimeZone = icalTimeZone.getVTimeZone();

		DateTime start = new DateTime(startDateTime.atZone(timeZone).toInstant().toEpochMilli());
		start.setTimeZone(icalTimeZone);
		DateTime end = new DateTime(endDateTime.atZone(timeZone).toInstant().toEpochMilli());
		end.setTimeZone(icalTimeZone);
		VEvent meeting = new VEvent(start, end, title);
		meeting.getProperties().add(icalVTimeZone.getTimeZoneId());

		Uid uid = new Uid(uniqueIdentifier);
		meeting.getProperties().add(uid);

		meeting.getProperties().add(new Location(location));
		meeting.getProperties().add(new Description(description));

		Organizer organizer = new Organizer(URI.create(format("mailto:%s", inviteOrganizer.getEmailAddress())));
		organizer.getParameters().add(new SentBy(URI.create(format("mailto:%s", getConfiguration().getEmailDefaultFromAddress()))));

		String organizerName = trimToNull(inviteOrganizer.getName().orElse(null));

		if (organizerName != null)
			organizer.getParameters().add(new Cn(organizerName));

		meeting.getProperties().add(organizer);

		Attendee attendee = new Attendee(URI.create(format("mailto:%s", inviteAttendee.getEmailAddress())));

		String attendeeName = trimToNull(inviteAttendee.getName().orElse(null));
		if (attendeeName != null)
			attendee.getParameters().add(new Cn(attendeeName));

		attendee.getParameters().add(Role.REQ_PARTICIPANT);
		attendee.getParameters().add(PartStat.ACCEPTED);
		meeting.getProperties().add(attendee);

		// Both provider and patient are attendees, no official organizer...
//		Attendee organizerAttendee = new Attendee(URI.create(format("mailto:%s", inviteOrganizer.getEmailAddress())));
//
//		String organizerAttendeeName = trimToNull(inviteOrganizer.getName().orElse(null));
//		if (organizerAttendeeName != null)
//			organizerAttendee.getParameters().add(new Cn(organizerAttendeeName));
//
//		organizerAttendee.getParameters().add(Role.REQ_PARTICIPANT);
//		organizerAttendee.getParameters().add(PartStat.ACCEPTED);
//		meeting.getProperties().add(organizerAttendee);

		net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
		icsCalendar.getProperties().add(CalScale.GREGORIAN);

		if (inviteMethod == InviteMethod.CANCEL) {
			icsCalendar.getProperties().add(Method.CANCEL);
			icsCalendar.getProperties().add(new Sequence(1));
		} else {
			icsCalendar.getProperties().add(Method.REQUEST);
			icsCalendar.getProperties().add(new Sequence(0));
		}

		icsCalendar.getComponents().add(meeting);

		return icsCalendar.toString();
	}

	public enum InviteMethod {
		REQUEST,
		CANCEL
	}

	@Immutable
	public static class InviteOrganizer {
		@Nonnull
		private final String name;
		@Nullable
		private final String emailAddress;

		@Nonnull
		public static InviteOrganizer forEmailAddress(@Nonnull String emailAddress) {
			return new InviteOrganizer(emailAddress, null);
		}

		@Nonnull
		public static InviteOrganizer forNameAndEmailAddress(@Nullable String name,
																												 @Nonnull String emailAddress) {
			requireNonNull(emailAddress);
			return new InviteOrganizer(emailAddress, name);
		}

		protected InviteOrganizer(@Nonnull String emailAddress,
															@Nullable String name) {
			requireNonNull(emailAddress);

			this.emailAddress = emailAddress;
			this.name = name;
		}

		@Nonnull
		public String getEmailAddress() {
			return this.emailAddress;
		}

		@Nonnull
		public Optional<String> getName() {
			return Optional.ofNullable(this.name);
		}
	}

	@Immutable
	public static class InviteAttendee {
		@Nonnull
		private final String name;
		@Nullable
		private final String emailAddress;

		@Nonnull
		public static InviteAttendee forEmailAddress(@Nonnull String emailAddress) {
			return new InviteAttendee(emailAddress, null);
		}

		@Nonnull
		public static InviteAttendee forNameAndEmailAddress(@Nullable String name,
																												@Nonnull String emailAddress) {
			requireNonNull(emailAddress);
			return new InviteAttendee(emailAddress, name);
		}

		protected InviteAttendee(@Nonnull String emailAddress,
														 @Nullable String name) {
			requireNonNull(emailAddress);

			this.emailAddress = emailAddress;
			this.name = name;
		}

		@Nonnull
		public String getEmailAddress() {
			return this.emailAddress;
		}

		@Nonnull
		public Optional<String> getName() {
			return Optional.ofNullable(this.name);
		}
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}
}
