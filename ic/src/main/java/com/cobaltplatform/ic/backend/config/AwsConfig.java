package com.cobaltplatform.ic.backend.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public final class AwsConfig {
	@Nonnull
	private final AWSCredentialsProvider awsCredentialsProvider;
	@Nonnull
	private final Regions region;
	@Nonnull
	private final Boolean useLocalstack;
	@Nullable
	private final Integer localstackPort;

	private AwsConfig(@Nonnull AWSCredentialsProvider awsCredentialsProvider,
										@Nonnull Regions region,
										@Nonnull Boolean useLocalstack,
										@Nullable Integer localstackPort) {
		requireNonNull(awsCredentialsProvider);
		requireNonNull(region);
		requireNonNull(useLocalstack);

		this.awsCredentialsProvider = awsCredentialsProvider;
		this.region = region;
		this.useLocalstack = useLocalstack;
		this.localstackPort = localstackPort;
	}

	@Nonnull
	public static AwsConfig forAws(@Nonnull AWSCredentialsProvider awsCredentialsProvider,
																 @Nonnull Regions region) {
		requireNonNull(awsCredentialsProvider);
		requireNonNull(region);

		return new AwsConfig(awsCredentialsProvider, region, false, null);
	}

	@Nonnull
	public static AwsConfig forLocalstack(@Nonnull Regions region,
																				@Nonnull Integer localstackPort) {
		requireNonNull(region);
		requireNonNull(localstackPort);

		// These don't matter
		String accessKey = "fake";
		String secretKey = "fake";
		AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));

		return new AwsConfig(awsCredentialsProvider, region, true, localstackPort);
	}

	@Nonnull
	public AWSCredentialsProvider getAwsCredentialsProvider() {
		return awsCredentialsProvider;
	}

	@Nonnull
	public Regions getRegion() {
		return region;
	}

	@Nonnull
	public Boolean getUseLocalstack() {
		return useLocalstack;
	}

	@Nonnull
	public Optional<Integer> getLocalstackPort() {
		return Optional.ofNullable(localstackPort);
	}
}
