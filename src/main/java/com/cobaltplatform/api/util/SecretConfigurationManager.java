package com.cobaltplatform.api.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Transmogrify LLC.
 */
public interface SecretConfigurationManager {
    @Nonnull
    String SECRETS_PREFIX = "$SECRET";
    @Nonnull
    String valueFor(@Nonnull String key);
}
