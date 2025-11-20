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

package com.cobaltplatform.api.util;

import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.CurrentContextCache;
import com.cobaltplatform.api.cache.LocalCache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.lokalized.Strings;
import org.apache.commons.text.WordUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class Formatter {
	@Nonnull
	private final Cache localCache;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Provider<Cache> currentContextCacheProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public Formatter(@Nonnull @LocalCache Cache localCache,
									 @Nonnull Provider<CurrentContext> currentContextProvider,
									 @Nonnull @CurrentContextCache Provider<Cache> currentContextCacheProvider,
									 @Nonnull Strings strings) {
		requireNonNull(localCache);
		requireNonNull(currentContextProvider);
		requireNonNull(currentContextCacheProvider);
		requireNonNull(strings);

		this.localCache = localCache;
		this.currentContextProvider = currentContextProvider;
		this.currentContextCacheProvider = currentContextCacheProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public String formatDate(@Nonnull LocalDate date) {
		requireNonNull(date);
		return formatDate(date, FormatStyle.LONG);
	}

	@Nonnull
	public String formatDate(@Nonnull LocalDate date,
													 @Nonnull FormatStyle formatStyle) {
		requireNonNull(date);
		requireNonNull(formatStyle);

		return formatDate(date, formatStyle, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatDate(@Nonnull LocalDate date,
													 @Nonnull FormatStyle formatStyle,
													 @Nonnull Locale locale) {
		requireNonNull(date);
		requireNonNull(formatStyle);
		requireNonNull(locale);

		String cacheKey = format("%s.date.%s.%s", getClass().getName(), formatStyle.name(), locale.toLanguageTag());

		DateTimeFormatter dateFormatter = getLocalCache().get(cacheKey, () ->
				DateTimeFormatter.ofLocalizedDate(formatStyle).withLocale(locale), DateTimeFormatter.class);

		return dateFormatter.format(date);
	}

	@Nonnull
	public String formatTime(@Nonnull LocalTime time) {
		requireNonNull(time);
		return formatTime(time, FormatStyle.SHORT);
	}

	@Nonnull
	public String formatTime(@Nonnull LocalTime time,
													 @Nonnull FormatStyle formatStyle) {
		requireNonNull(time);
		requireNonNull(formatStyle);

		return formatTime(time, formatStyle, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatTime(@Nonnull LocalTime time,
													 @Nonnull FormatStyle formatStyle,
													 @Nonnull Locale locale) {
		requireNonNull(time);
		requireNonNull(formatStyle);
		requireNonNull(locale);

		String cacheKey = format("%s.time.%s.%s", getClass().getName(), formatStyle.name(), locale.toLanguageTag());

		DateTimeFormatter dateFormatter = getLocalCache().get(cacheKey, () ->
				DateTimeFormatter.ofLocalizedTime(formatStyle).withLocale(locale), DateTimeFormatter.class);

		return dateFormatter.format(time);
	}

	@Nonnull
	public String formatDateTime(@Nonnull LocalDateTime dateTime) {
		requireNonNull(dateTime);
		return formatDateTime(dateTime, FormatStyle.LONG, FormatStyle.MEDIUM);
	}

	@Nonnull
	public String formatDateTime(@Nonnull LocalDateTime dateTime,
															 @Nonnull FormatStyle dateFormatStyle,
															 @Nonnull FormatStyle timeFormatStyle) {
		requireNonNull(dateTime);
		requireNonNull(dateFormatStyle);
		requireNonNull(timeFormatStyle);

		return formatDateTime(dateTime, dateFormatStyle, timeFormatStyle, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatDateTime(@Nonnull LocalDateTime dateTime,
															 @Nonnull FormatStyle dateFormatStyle,
															 @Nonnull FormatStyle timeFormatStyle,
															 @Nonnull Locale locale) {
		requireNonNull(dateTime);
		requireNonNull(dateFormatStyle);
		requireNonNull(timeFormatStyle);
		requireNonNull(locale);

		String cacheKey = format("%s.datetime.%s.%s.%s", getClass().getName(), dateFormatStyle.name(), timeFormatStyle.name(), locale.toLanguageTag());

		DateTimeFormatter dateFormatter = getLocalCache().get(cacheKey, () ->
				DateTimeFormatter.ofLocalizedDateTime(dateFormatStyle, timeFormatStyle).withLocale(locale), DateTimeFormatter.class);

		return dateFormatter.format(dateTime);
	}

	@Nonnull
	public String formatTimestamp(@Nonnull Instant timestamp) {
		requireNonNull(timestamp);
		return formatTimestamp(timestamp, FormatStyle.LONG, FormatStyle.SHORT);
	}

	@Nonnull
	public String formatTimestamp(@Nonnull Instant timestamp,
																@Nonnull FormatStyle dateFormatStyle,
																@Nonnull FormatStyle timeFormatStyle) {
		requireNonNull(timestamp);
		requireNonNull(dateFormatStyle);
		requireNonNull(timeFormatStyle);

		return formatTimestamp(timestamp, dateFormatStyle, timeFormatStyle, getCurrentContext().getTimeZone());
	}

	@Nonnull
	public String formatTimestamp(@Nonnull Instant timestamp,
																@Nonnull FormatStyle dateFormatStyle,
																@Nonnull FormatStyle timeFormatStyle,
																@Nonnull ZoneId timeZone) {
		requireNonNull(timestamp);
		requireNonNull(dateFormatStyle);
		requireNonNull(timeFormatStyle);
		requireNonNull(timeZone);

		return formatTimestamp(timestamp, dateFormatStyle, timeFormatStyle, timeZone, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatTimestamp(@Nonnull Instant timestamp,
																@Nonnull FormatStyle dateFormatStyle,
																@Nonnull FormatStyle timeFormatStyle,
																@Nonnull ZoneId timeZone,
																@Nonnull Locale locale) {
		requireNonNull(timestamp);
		requireNonNull(dateFormatStyle);
		requireNonNull(timeFormatStyle);
		requireNonNull(locale);
		requireNonNull(timeZone);

		String cacheKey = format("%s.timestamp.%s.%s.%s.%s", getClass().getName(),
				dateFormatStyle.name(),
				timeFormatStyle.name(),
				locale.toLanguageTag(),
				timeZone.getId());

		DateTimeFormatter dateFormatter = getLocalCache().get(cacheKey, () ->
				DateTimeFormatter.ofLocalizedDateTime(dateFormatStyle, timeFormatStyle).withLocale(locale).withZone(timeZone), DateTimeFormatter.class);

		return dateFormatter.format(timestamp);
	}

	@Nonnull
	public String formatTimestampNatural(@Nonnull Instant timestamp) {
		requireNonNull(timestamp);
		return formatTimestampNatural(timestamp, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatTimestampNatural(@Nonnull Instant timestamp,
																			 @Nonnull Locale locale) {
		requireNonNull(timestamp);
		requireNonNull(locale);

		// Note: capitalization is not locale-sensitive!
		return WordUtils.capitalize(new PrettyTime(locale).format(Date.from(timestamp)));
	}

	@Nonnull
	public String formatNumber(@Nonnull Number number) {
		requireNonNull(number);
		return formatNumber(number, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatNumber(@Nonnull Number number,
														 @Nonnull Locale locale) {
		requireNonNull(number);
		requireNonNull(locale);

		String cacheKey = format("%s.number.%s", getClass().getName(), locale.toLanguageTag());

		// Use current context cache since NumberFormat is not threadsafe
		NumberFormat numberFormatter = getCurrentContextCache().get(cacheKey, () ->
				NumberFormat.getNumberInstance(locale), NumberFormat.class);

		return numberFormatter.format(number);
	}

	@Nonnull
	public String formatNumber(@Nonnull Number number,
														 @Nonnull Integer minimumFractionDigits,
														 @Nonnull Integer maximumFractionDigits,
														 @Nonnull RoundingMode roundingMode) {
		requireNonNull(number);
		requireNonNull(minimumFractionDigits);
		requireNonNull(maximumFractionDigits);
		requireNonNull(roundingMode);

		return formatNumber(number, minimumFractionDigits, maximumFractionDigits, roundingMode, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatNumber(@Nonnull Number number,
														 @Nonnull Integer minimumFractionDigits,
														 @Nonnull Integer maximumFractionDigits,
														 @Nonnull RoundingMode roundingMode,
														 @Nonnull Locale locale) {
		requireNonNull(number);
		requireNonNull(minimumFractionDigits);
		requireNonNull(maximumFractionDigits);
		requireNonNull(roundingMode);
		requireNonNull(locale);

		String cacheKey = format("%s.number.%s.%s.%s.%s", getClass().getName(), minimumFractionDigits,
				maximumFractionDigits, roundingMode.name(), locale.toLanguageTag());

		// Use current context cache since NumberFormat is not threadsafe
		NumberFormat numberFormatter = getCurrentContextCache().get(cacheKey, () -> {
			NumberFormat cachedNumberFormatter = NumberFormat.getNumberInstance(locale);
			cachedNumberFormatter.setMinimumFractionDigits(minimumFractionDigits);
			cachedNumberFormatter.setMaximumFractionDigits(maximumFractionDigits);
			cachedNumberFormatter.setRoundingMode(roundingMode);
			return cachedNumberFormatter;
		}, NumberFormat.class);

		return numberFormatter.format(number);
	}

	@Nonnull
	public String formatInteger(@Nonnull Number number) {
		requireNonNull(number);
		return formatInteger(number, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatInteger(@Nonnull Number number,
															@Nonnull Locale locale) {
		requireNonNull(number);
		requireNonNull(locale);

		String cacheKey = format("%s.integer.%s", getClass().getName(), locale.toLanguageTag());

		// Use current context cache since NumberFormat is not threadsafe
		NumberFormat numberFormatter = getCurrentContextCache().get(cacheKey, () ->
				NumberFormat.getIntegerInstance(locale), NumberFormat.class);

		return numberFormatter.format(number);
	}

	@Nonnull
	public String formatPercent(@Nonnull Number number) {
		requireNonNull(number);
		return formatPercent(number, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatPercent(@Nonnull Number number,
															@Nonnull Locale locale) {
		requireNonNull(number);
		requireNonNull(locale);

		String cacheKey = format("%s.percent.%s", getClass().getName(), locale.toLanguageTag());

		// Use current context cache since NumberFormat is not threadsafe
		NumberFormat numberFormatter = getCurrentContextCache().get(cacheKey, () ->
				NumberFormat.getPercentInstance(locale), NumberFormat.class);

		return numberFormatter.format(number);
	}

	@Nonnull
	public String formatCurrency(@Nonnull Number number,
															 @Nonnull String currencyCode) {
		requireNonNull(number);
		requireNonNull(currencyCode);

		return formatCurrency(number, currencyCode, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatCurrency(@Nonnull Number number,
															 @Nonnull String currencyCode,
															 @Nonnull Locale locale) {
		requireNonNull(number);
		requireNonNull(currencyCode);
		requireNonNull(locale);

		return formatCurrency(number, Currency.getInstance(currencyCode), locale);
	}

	@Nonnull
	public String formatCurrency(@Nonnull Number number,
															 @Nonnull Currency currency,
															 @Nonnull Locale locale) {
		requireNonNull(number);
		requireNonNull(currency);
		requireNonNull(locale);

		String cacheKey = format("%s.currency.%s.%s", getClass().getName(), currency.getCurrencyCode(), locale.toLanguageTag());

		// Use current context cache since NumberFormat is not threadsafe
		NumberFormat numberFormatter = getCurrentContextCache().get(cacheKey, () -> {
			NumberFormat cachedNumberFormatter = NumberFormat.getCurrencyInstance(locale);
			cachedNumberFormatter.setCurrency(currency);
			return cachedNumberFormatter;
		}, NumberFormat.class);

		return numberFormatter.format(number);
	}

	@Nonnull
	public String formatCurrencyCanonical(@Nonnull Number number,
																				@Nonnull String currencyCode) {
		requireNonNull(number);
		requireNonNull(currencyCode);

		return formatCurrencyCanonical(number, Currency.getInstance(currencyCode));
	}

	@Nonnull
	public String formatCurrencyCanonical(@Nonnull Number number,
																				@Nonnull Currency currency) {
		requireNonNull(number);
		requireNonNull(currency);

		final Locale CANONICAL_LOCALE = Locale.US;

		String cacheKey = format("%s.currencyCanonical.%s", getClass().getName(), currency.getCurrencyCode());

		// Use current context cache since NumberFormat is not threadsafe
		NumberFormat numberFormatter = getCurrentContextCache().get(cacheKey, () -> {
			NumberFormat cachedNumberFormatter = NumberFormat.getNumberInstance(CANONICAL_LOCALE);
			cachedNumberFormatter.setGroupingUsed(false);
			return cachedNumberFormatter;
		}, NumberFormat.class);

		return numberFormatter.format(number);
	}

	@Nonnull
	public String formatCurrencyCode(@Nonnull String currencyCode) {
		requireNonNull(currencyCode);
		return formatCurrencyCode(currencyCode, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatCurrencyCode(@Nonnull Currency currency) {
		requireNonNull(currency);
		return formatCurrencyCode(currency, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatCurrencyCode(@Nonnull String currencyCode,
																	 @Nonnull Locale locale) {
		requireNonNull(currencyCode);
		requireNonNull(locale);

		return formatCurrencyCode(Currency.getInstance(currencyCode), locale);
	}

	@Nonnull
	public String formatCurrencyCode(@Nonnull Currency currency,
																	 @Nonnull Locale locale) {
		requireNonNull(currency);
		requireNonNull(locale);

		return currency.getDisplayName(locale);
	}

	@Nonnull
	public String formatFilesize(@Nonnull Number filesize) {
		requireNonNull(filesize);
		return formatFilesize(filesize, getCurrentContext().getLocale());
	}

	@Nonnull
	public String formatFilesize(@Nonnull Number filesize,
															 @Nonnull Locale locale) {
		requireNonNull(filesize);
		requireNonNull(locale);

		return formatFilesize(filesize, FilesizeUnit.DECIMAL, locale);
	}

	@Nonnull
	public String formatFilesize(@Nonnull Number filesize,
															 @Nonnull FilesizeUnit filesizeUnit,
															 @Nonnull Locale locale) {
		requireNonNull(filesize);
		requireNonNull(locale);

		double filesizeAsDouble = filesize.doubleValue();
		double divisor = filesizeUnit.getDivisor();

		if (filesizeAsDouble < divisor)
			return getStrings().get("{{bytes}} bytes", new HashMap<String, Object>() {{
				put("bytes", formatInteger(filesizeAsDouble, locale));
			}}, locale);

		double kiloFilesize = filesizeAsDouble / divisor;

		if (kiloFilesize < divisor)
			if (filesizeUnit == FilesizeUnit.BINARY)
				return getStrings().get("{{kibibytes}} KiB", new HashMap<String, Object>() {{
					put("kibibytes", formatNumber(kiloFilesize, 0, 1, RoundingMode.HALF_EVEN, locale));
				}}, locale);
			else
				return getStrings().get("{{kilobytes}} KB", new HashMap<String, Object>() {{
					put("kilobytes", formatNumber(kiloFilesize, 0, 1, RoundingMode.HALF_EVEN, locale));
				}}, locale);

		double megaFilesize = kiloFilesize / divisor;

		if (megaFilesize < divisor)
			if (filesizeUnit == FilesizeUnit.BINARY)
				return getStrings().get("{{mebibytes}} MiB", new HashMap<String, Object>() {{
					put("mebibytes", formatNumber(megaFilesize, 0, 1, RoundingMode.HALF_EVEN, locale));
				}}, locale);
			else
				return getStrings().get("{{megabytes}} MB", new HashMap<String, Object>() {{
					put("megabytes", formatNumber(megaFilesize, 0, 1, RoundingMode.HALF_EVEN, locale));
				}}, locale);

		double gigaFilesize = megaFilesize / divisor;

		if (filesizeUnit == FilesizeUnit.BINARY)
			return getStrings().get("{{gibibytes}} GiB", new HashMap<String, Object>() {{
				put("gibibytes", formatNumber(gigaFilesize, 0, 2, RoundingMode.HALF_EVEN, locale));
			}}, locale);

		return getStrings().get("{{gigabytes}} GB", new HashMap<String, Object>() {{
			put("gigabytes", formatNumber(gigaFilesize, 0, 2, RoundingMode.HALF_EVEN, locale));
		}}, locale);
	}

	@Nullable
	public String formatPixelDimension(@Nullable Integer pixelDimension) {
		return formatPixelDimension(pixelDimension, getCurrentContext().getLocale());
	}

	@Nullable
	public String formatPixelDimension(@Nullable Integer pixelDimension,
																		 @Nonnull Locale locale) {
		requireNonNull(locale);

		if (pixelDimension == null)
			return null;

		return getStrings().get("{{dimension}}px", new HashMap<String, Object>() {{
			put("dimension", formatNumber(pixelDimension, locale));
		}}, locale);
	}

	@Nullable
	public String formatDuration(@Nullable Duration duration) {
		return formatDuration(duration, getCurrentContext().getLocale());
	}

	@Nullable
	public String formatDuration(@Nullable Duration duration,
															 @Nonnull Locale locale) {
		requireNonNull(locale);
		return formatDuration(duration == null ? null : duration.getSeconds(), getCurrentContext().getLocale());
	}

	@Nullable
	public String formatDuration(@Nullable Number durationInSeconds) {
		return formatDuration(durationInSeconds, getCurrentContext().getLocale());
	}

	@Nullable
	public String formatDuration(@Nullable Number durationInSeconds,
															 @Nonnull Locale locale) {
		requireNonNull(locale);

		if (durationInSeconds == null)
			return null;

		double durationInSecondsAsDouble = durationInSeconds.doubleValue();
		List<String> durationComponents = new ArrayList<>(3);

		// 7201 = "2 hours, 1 second"
		// 3599 = "59 minutes, 59 seconds"
		// 59 = "59 seconds"

		if (durationInSecondsAsDouble >= 3600) {
			double hours = Math.floor(durationInSecondsAsDouble / 3600);
			durationComponents.add(getStrings().get("{{durationDescription}} hours", new HashMap<String, Object>() {{
				put("durationDescription", formatInteger(hours, locale));
				put("duration", hours);
			}}));

			durationInSecondsAsDouble = durationInSecondsAsDouble - 3600 * hours;
		}

		if (durationInSecondsAsDouble >= 60) {
			double minutes = Math.floor(durationInSecondsAsDouble / 60);
			durationComponents.add(getStrings().get("{{durationDescription}} minutes", new HashMap<String, Object>() {{
				put("durationDescription", formatInteger(minutes, locale));
				put("duration", minutes);
			}}));

			durationInSecondsAsDouble = durationInSecondsAsDouble - 60 * minutes;
		}

		double seconds = durationInSecondsAsDouble;

		if (seconds > 0) {
			durationComponents.add(getStrings().get("{{durationDescription}} seconds", new HashMap<String, Object>() {{
				put("durationDescription", formatInteger(seconds, locale));
				put("duration", seconds);
			}}));
		}

		return durationComponents.stream().collect(Collectors.joining(", "));
	}

	@Nullable
	public String formatPhoneNumber(@Nullable String phoneNumber) {
		return formatPhoneNumber(phoneNumber, getCurrentContext().getLocale());
	}

	@Nullable
	public String formatPhoneNumber(@Nullable String phoneNumber,
																	@Nonnull Locale locale) {
		requireNonNull(locale);

		phoneNumber = trimToNull(phoneNumber);

		if (phoneNumber == null)
			return null;

		PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
		PhoneNumber parsedPhoneNumber;

		final String FALLBACK_COUNTRY_CODE = "US";

		String countryCode = trimToNull(locale.getCountry());

		if (countryCode == null) {
			getLogger().trace("There is no country code available for locale '{}', falling back to {}...",
					getCurrentContext().getLocale(), FALLBACK_COUNTRY_CODE);
			countryCode = FALLBACK_COUNTRY_CODE;
		}

		try {
			parsedPhoneNumber = phoneNumberUtil.parse(phoneNumber, countryCode);
		} catch (Exception e) {
			return phoneNumber;
		}

		return phoneNumberUtil.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
	}

	@Nonnull
	public String formatStackTrace(@Nonnull Throwable throwable) {
		requireNonNull(throwable);

		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		return stringWriter.toString();
	}

	@Nonnull
	public String formatHexColor(@Nonnull Integer color) {
		// e.g. 3692510 (decimal) would be #3857de
		return format("#%s", Integer.toHexString(color)).toLowerCase(Locale.US);
	}

	@Nonnull
	public String formatEmailSalutation(@Nullable Account account) {
		if (account == null || account.getFirstName() == null || account.getFirstName().trim().length() == 0)
			return getStrings().get("Hello,");

		return getStrings().get("Hi {{firstName}},", new HashMap<>() {{
			put("firstName", account.getFirstName());
		}});
	}

	public enum FilesizeUnit {
		DECIMAL(1_000), BINARY(1_024);

		private final Integer divisor;

		FilesizeUnit(@Nonnull Integer divisor) {
			requireNonNull(divisor);
			this.divisor = divisor;
		}

		@Nonnull
		public Integer getDivisor() {
			return this.divisor;
		}
	}

	@Nonnull
	protected Cache getLocalCache() {
		return localCache;
	}

	@Nonnull
	protected Cache getCurrentContextCache() {
		return currentContextCacheProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}