package com.cobaltplatform.api.model.security;

/**
 * @author Transmogrify, LLC.
 */
public enum ContentSecurityLevel {
	LOW, // e.g. a list of available content, not account-specific
	HIGH // e.g. an appointment with a healthcare provider
}