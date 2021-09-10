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

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
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
	public String generateInvite(@Nonnull String uniqueIdentifier,
															 @Nonnull String title,
															 @Nonnull String description,
															 @Nonnull LocalDateTime startDateTime,
															 @Nonnull LocalDateTime endDateTime,
															 @Nonnull ZoneId timeZone,
															 @Nonnull String location) {
		requireNonNull(uniqueIdentifier);
		requireNonNull(title);
		requireNonNull(description);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(timeZone);
		requireNonNull(location);

		TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone icalTimeZone = timeZoneRegistry.getTimeZone(timeZone.getId());
		VTimeZone icalVTimeZone = icalTimeZone.getVTimeZone();

		DateTime start = new DateTime(startDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
		DateTime end = new DateTime(endDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
		VEvent meeting = new VEvent(start, end, title);
		meeting.getProperties().add(icalVTimeZone.getTimeZoneId());

		Uid uid = new Uid(uniqueIdentifier);
		meeting.getProperties().add(uid);

		meeting.getProperties().add(new Location(location));
		meeting.getProperties().add(new Description(description));

		net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
		icsCalendar.getProperties().add(CalScale.GREGORIAN);

		icsCalendar.getComponents().add(meeting);

		return icsCalendar.toString();
	}
}
