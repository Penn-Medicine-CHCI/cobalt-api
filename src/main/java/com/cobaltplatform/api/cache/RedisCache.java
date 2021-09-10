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

package com.cobaltplatform.api.cache;

import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.JsonMapper.MappingFormat;
import com.cobaltplatform.api.util.JsonMapper.MappingNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class RedisCache implements Cache, AutoCloseable {
	@Nonnull
	private static final Integer DEFAULT_PORT;

	@Nonnull
	private final String host;
	@Nonnull
	private final Integer port;
	@Nonnull
	private final Object lock;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private JedisPool jedisPool;

	static {
		DEFAULT_PORT = 6379;
	}

	public RedisCache(@Nonnull String host) {
		this(host, getDefaultPort());
	}

	public RedisCache(@Nonnull String host,
										@Nonnull Integer port) {
		requireNonNull(host);
		requireNonNull(port);

		this.host = host;
		this.port = port;
		this.jsonMapper = createJsonMapper();
		this.lock = new Object();
		this.started = false;
		this.logger = LoggerFactory.getLogger(getClass());

		start();
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	@Nonnull
	public Boolean start() {
		synchronized (getLock()) {
			if (isStarted())
				return false;

			getLogger().trace("Creating Redis pool for {}:{}...", getHost(), getPort());

			jedisPool = createJedisPool(getHost(), getPort());
			started = true;

			getLogger().trace("Redis pool created.");

			return true;
		}
	}

	@Nonnull
	public Boolean stop() {
		synchronized (getLock()) {
			if (!isStarted())
				return false;

			getLogger().trace("Shutting down Redis pool...");

			getJedisPool().get().close();
			jedisPool = null;
			started = false;

			getLogger().trace("Redis pool shut down.");

			return true;
		}
	}

	@Nonnull
	@Override
	public <T> Optional<T> get(@Nonnull String key,
														 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(type);

		ensureStarted();

		String json;

		try (Jedis jedis = getJedisPool().get().getResource()) {
			json = jedis.get(key);
		}

		if (json == null)
			return Optional.empty();

		return Optional.of(getJsonMapper().fromJson(json, type));
	}

	@Nonnull
	@Override
	public <T> Optional<List<T>> getList(@Nonnull String key,
																			 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(type);

		ensureStarted();

		String json;

		try (Jedis jedis = getJedisPool().get().getResource()) {
			json = jedis.get(key);
		}

		if (json == null)
			return Optional.empty();

		return Optional.of(getJsonMapper().toList(json, type));
	}

	@Nullable
	@Override
	public <T> T get(@Nonnull String key,
									 @Nonnull Supplier<T> supplier,
									 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(supplier);
		requireNonNull(type);

		ensureStarted();

		// Note: this is a naive implementation, can cause stampedes under contention.
		// Should be fine for our uses, however
		T result = get(key, type).orElse(null);

		if (result != null)
			return result;

		result = supplier.get();
		put(key, result);

		return result;
	}

	@Nonnull
	@Override
	public <T> List<T> getList(@Nonnull String key,
														 @Nonnull Supplier<List<T>> supplier,
														 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(supplier);
		requireNonNull(type);

		ensureStarted();

		// Note: this is a naive implementation, can cause stampedes under contention.
		// Should be fine for our uses, however
		List<T> result = getList(key, type).orElse(null);

		if (result != null)
			return result;

		result = supplier.get();
		put(key, result);

		return result;
	}

	@Override
	public void put(@Nonnull String key,
									@Nonnull Object value) {
		requireNonNull(key);
		requireNonNull(value);

		ensureStarted();

		String json = getJsonMapper().toJson(value);

		try (Jedis jedis = getJedisPool().get().getResource()) {
			jedis.set(key, json);
		}
	}

	@Override
	public void invalidate(@Nonnull String key) {
		requireNonNull(key);

		ensureStarted();

		try (Jedis jedis = getJedisPool().get().getResource()) {
			jedis.del(key);
		}
	}

	@Override
	public void invalidateAll() {
		ensureStarted();

		try (Jedis jedis = getJedisPool().get().getResource()) {
			jedis.flushAll();
		}
	}

	@Nonnull
	@Override
	public Set<String> getKeys() {
		ensureStarted();

		try (Jedis jedis = getJedisPool().get().getResource()) {
			return jedis.keys("*");
		}
	}

	protected void ensureStarted() {
		if (!isStarted())
			throw new IllegalStateException("Redis pool is not started");
	}

	@Nonnull
	public Boolean isStarted() {
		synchronized (getLock()) {
			return started;
		}
	}

	@Nonnull
	protected JedisPool createJedisPool(@Nonnull String host,
																			@Nonnull Integer port) {
		requireNonNull(host);
		requireNonNull(port);

		return new JedisPool(new JedisPoolConfig(), host, port);
	}

	@Nonnull
	protected JsonMapper createJsonMapper() {
		return new JsonMapper.Builder()
				.mappingFormat(MappingFormat.COMPACT)
				.mappingNullability(MappingNullability.EXCLUDE_NULLS)
				.build();
	}

	@Nonnull
	protected static Integer getDefaultPort() {
		return DEFAULT_PORT;
	}

	@Nonnull
	protected String getHost() {
		return host;
	}

	@Nonnull
	protected Integer getPort() {
		return port;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Object getLock() {
		return lock;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected Optional<JedisPool> getJedisPool() {
		return Optional.ofNullable(jedisPool);
	}
}
