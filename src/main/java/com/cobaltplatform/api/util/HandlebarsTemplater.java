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

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.cobaltplatform.api.Configuration;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class HandlebarsTemplater {
	@Nonnull
	private final Path templateRootDirectory;
	@Nonnull
	private final Configuration configuration;
	@Nullable
	private final String viewsDirectoryName;
	@Nonnull
	private final Handlebars handlebars;
	@Nonnull
	private final TemplateCache handlebarsTemplateCache;
	@Nonnull
	private final Boolean shouldCacheHandlebarsTemplates;
	@Nonnull
	private final Logger logger;

	public HandlebarsTemplater(@Nonnull Path templateRootDirectory,
														 @Nonnull Configuration configuration) {
		this(templateRootDirectory, configuration, null);
	}

	public HandlebarsTemplater(@Nonnull Path templateRootDirectory,
														 @Nonnull Configuration configuration,
														 @Nullable String viewsDirectoryName) {
		requireNonNull(templateRootDirectory);
		requireNonNull(configuration);

		this.templateRootDirectory = templateRootDirectory;
		this.configuration = configuration;
		this.viewsDirectoryName = viewsDirectoryName;
		this.handlebarsTemplateCache = new ConcurrentMapTemplateCache();
		this.shouldCacheHandlebarsTemplates = configuration.getShouldCacheHandlebarsTemplates();
		this.handlebars = createHandlebars(templateRootDirectory, handlebarsTemplateCache);
		this.logger = LoggerFactory.getLogger(getClass());
	}

	/**
	 * Merges a Handlebars template in a locale-aware way.
	 * <p>
	 * Given a {@code templateRootDirectory} like {@code "messages/email"}, a {@code templateParentName} of
	 * {@code "QMR_STATUS_CHANGE"} and a {@code templateChildName} of @{code "body"}, the on-disk representation would look like this:
	 * <p>
	 * {@code messages/email/QMR_STATUS_CHANGE/en/body.hbs}
	 *
	 * @param templateParentName e.g. @{code "QMR_STATUS_CHANGE"}
	 * @param templateChildName  e.g. @{code "body"}
	 * @param locale             used to resolve localized template
	 * @return the merged template, or an empty result if no template was found
	 */
	@Nonnull
	public Optional<String> mergeTemplate(@Nonnull String templateParentName,
																				@Nonnull String templateChildName,
																				@Nonnull Locale locale) {
		return mergeTemplate(templateParentName, templateChildName, locale, Collections.emptyMap());
	}

	/**
	 * Merges a Handlebars template in a locale-aware way.
	 * <p>
	 * Given a {@code templateRootDirectory} like {@code "messages/email"}, a {@code templateParentName} of
	 * {@code "QMR_STATUS_CHANGE"} and a {@code templateChildName} of @{code "body"}, the on-disk representation would look like this:
	 * <p>
	 * {@code messages/email/QMR_STATUS_CHANGE/en/body.hbs}
	 *
	 * @param templateParentName e.g. @{code "QMR_STATUS_CHANGE"}
	 * @param templateChildName  e.g. @{code "body"}
	 * @param locale             used to resolve localized template
	 * @param context            data to merge into the template, if any
	 * @return the merged template, or an empty result if no template was found
	 */
	@Nonnull
	public Optional<String> mergeTemplate(@Nonnull String templateParentName,
																				@Nonnull String templateChildName,
																				@Nonnull Locale locale,
																				@Nonnull Map<String, Object> context) {
		// templateParentName like QMR_STATUS_CHANGE
		// templateChildName like body
		requireNonNull(templateParentName);
		requireNonNull(templateChildName);
		requireNonNull(locale);
		requireNonNull(context);

		if (!getShouldCacheHandlebarsTemplates())
			getHandlebarsTemplateCache().clear();

		String templateName = resolveTemplateName(getTemplateRootDirectory(), templateParentName, templateChildName, locale).orElse(null);

		if (templateName == null)
			return Optional.empty();

		try {
			// Should be templateParentName/locale/templateChildName
			Template template = getHandlebars().compile(templateName);

			Context handlebarsContext = Context.newBuilder(context).resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE,
					FieldValueResolver.INSTANCE, MethodValueResolver.INSTANCE)
					.build();

			return Optional.of(template.apply(handlebarsContext));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected Optional<String> resolveTemplateName(@Nonnull Path templateRootDirectory,
																								 @Nonnull String templateParentName,
																								 @Nonnull String templateChildName,
																								 @Nonnull Locale locale) {
		requireNonNull(templateRootDirectory);
		requireNonNull(templateParentName);
		requireNonNull(templateChildName);
		requireNonNull(locale);

		// e.g. "en", not "en-US"
		String language = locale.getLanguage();

		// Prepend "views" directory if it's present
		if (getViewsDirectoryName().isPresent())
			templateRootDirectory = templateRootDirectory.resolve(getViewsDirectoryName().get());

		Path languageSpecificPath = templateRootDirectory.resolve(templateParentName).resolve(language).resolve(format("%s.hbs", templateChildName));

		// See if a template exists on-disk for the preferred language, e.g. "QMR_STATUS_CHANGE/ja/body.hbs" - if not found, fall back to default
		if (!Files.exists(languageSpecificPath)) {
			getLogger().trace("Language-specific template at {} does not exist, trying fallback...", languageSpecificPath.toAbsolutePath());

			// Default would be "en" - so something like "QMR_STATUS_CHANGE/en/body.hbs"
			// If we can't find the default, return an empty result so callers can figure out how to proceed
			language = getConfiguration().getDefaultLocale().getLanguage();
			languageSpecificPath = templateRootDirectory.resolve(templateParentName).resolve(language).resolve(format("%s.hbs", templateChildName));

			if (!Files.exists(languageSpecificPath)) {
				getLogger().warn("Unable to locate Handlebars template at {}", languageSpecificPath.toAbsolutePath());
				return Optional.empty();
			}
		}

		String finalTemplateName = null;

		if (getViewsDirectoryName().isPresent())
			finalTemplateName = format("%s/%s/%s/%s", getViewsDirectoryName().get(), templateParentName, language, templateChildName);
		else
			finalTemplateName = format("%s/%s/%s", templateParentName, language, templateChildName);

		return Optional.of(finalTemplateName);
	}

	@Nonnull
	protected Handlebars createHandlebars(@Nonnull Path templateRootDirectory,
																				@Nonnull TemplateCache templateCache) {
		requireNonNull(templateRootDirectory);
		requireNonNull(templateCache);

		if (!Files.exists(templateRootDirectory))
			throw new RuntimeException(format("Handlebars template root directory %s does not exist",
					templateRootDirectory.toAbsolutePath()));

		if (!Files.isDirectory(templateRootDirectory))
			throw new RuntimeException(format("Handlebars template root directory %s is not a directory",
					templateRootDirectory.toAbsolutePath()));

		TemplateLoader templateLoader = new FileTemplateLoader(templateRootDirectory.toFile());
		templateLoader.setSuffix(".hbs");
		Handlebars handlebars = new Handlebars(templateLoader).with(templateCache);
		handlebars.registerHelpers(new HelperSource(handlebars));
		return handlebars;
	}

	@ThreadSafe
	protected static class HelperSource {
		@Nonnull
		private final Handlebars handlebars;

		public HelperSource(@Nonnull Handlebars handlebars) {
			requireNonNull(handlebars);
			this.handlebars = handlebars;
		}

		// Joins a collection into a string with the given delimiter
		@Nonnull
		public CharSequence join(@Nullable Collection<?> collection, @Nullable String delimiter, @Nonnull Options options) {
			requireNonNull(options);

			if (collection == null)
				return "";

			if (delimiter == null)
				delimiter = " ";

			return new Handlebars.SafeString(collection.stream()
					.map(element -> element == null ? null : element.toString())
					.collect(Collectors.joining(delimiter)));
		}

		// Ensures a string can be embedded in an HTML attribute
		@Nonnull
		public CharSequence escapeHtmlAttribute(@Nullable Object object, @Nonnull Options options) {
			requireNonNull(options);

			if (object == null)
				return "";

			String objectAsString = object.toString();

			if (objectAsString == null)
				return "";

			objectAsString = StringEscapeUtils.escapeHtml4(objectAsString);
			objectAsString = objectAsString.replace("'", "&apos;");

			return objectAsString;
		}

		// Tests two arguments for equality
		@Nullable
		public CharSequence ifEqual(@Nullable Object object1, @Nullable Object object2, @Nonnull Options options) {
			requireNonNull(options);

			try {
				// Normalize enums to Strings
				if (object1 != null && object1.getClass().isEnum())
					object1 = ((Enum<?>) object1).name();

				if (object2 != null && object2.getClass().isEnum())
					object2 = ((Enum<?>) object2).name();

				if (Objects.equals(object1, object2))
					return options.fn(this);
				return options.inverse(this);
			} catch (IOException e) {
				throw new UncheckedIOException(
						format("Unable to perform equality comparison between %s and %s", object1, object2), e);
			}
		}

		// Tests two arguments for inequality
		@Nullable
		public CharSequence ifNotEqual(@Nullable Object object1, @Nullable Object object2, @Nonnull Options options) {
			requireNonNull(options);

			try {
				// Normalize enums to Strings
				if (object1 != null && object1.getClass().isEnum())
					object1 = ((Enum<?>) object1).name();

				if (object2 != null && object2.getClass().isEnum())
					object2 = ((Enum<?>) object2).name();

				if (!Objects.equals(object1, object2))
					return options.fn(this);
				return options.inverse(this);
			} catch (IOException e) {
				throw new UncheckedIOException(
						format("Unable to perform equality comparison between %s and %s", object1, object2), e);
			}
		}

		// Determines if an element exists within a collection
		@Nullable
		public CharSequence ifContains(@Nullable Collection<?> object1, @Nullable Object object2, @Nonnull Options options) {
			requireNonNull(options);

			try {
				if (object1.contains(object2))
					return options.fn(this);
				return options.inverse(this);
			} catch (IOException e) {
				throw new UncheckedIOException(
						format("Unable to perform contains check over %s and %s", object1, object2), e);
			}
		}
	}

	@Nonnull
	protected Path getTemplateRootDirectory() {
		return templateRootDirectory;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nullable
	protected Optional<String> getViewsDirectoryName() {
		return Optional.ofNullable(viewsDirectoryName);
	}

	@Nonnull
	protected Handlebars getHandlebars() {
		return handlebars;
	}

	@Nonnull
	protected TemplateCache getHandlebarsTemplateCache() {
		return handlebarsTemplateCache;
	}

	@Nonnull
	protected Boolean getShouldCacheHandlebarsTemplates() {
		return shouldCacheHandlebarsTemplates;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}