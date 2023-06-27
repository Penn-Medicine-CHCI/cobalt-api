package com.cobaltplatform.api.util;


import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
public class AwsSecretConfigurationManager implements SecretConfigurationManager {

    @Nonnull
    private final String secretStore;
    @Nonnull
    private Map<String, String> secrets;

    public AwsSecretConfigurationManager(@Nonnull AwsSecretManagerClient awsSecretManagerClient,
                                         @Nonnull String secretContext) {
        this.secrets = new HashMap<>();
        this.secretStore = secretContext + "-configuration";
        this.secrets = awsSecretManagerClient.getSecretMap(secretStore);
    }

    @Nonnull
    @Override
    public String valueFor(@Nonnull String key) {
        String value = secrets.get(key.replace(SECRETS_PREFIX, ""));
        if(value == null){
            throw new ConfigurationException(format("Unable to find value for %s in secret store %s", key, secretStore));
        }
        return value;
    }

}
