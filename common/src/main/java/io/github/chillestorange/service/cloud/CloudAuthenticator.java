package io.github.chillestorange.service.cloud;

import java.io.IOException;

/**
 * Minimal contract every provider's auth needs to satisfy — deliberately just
 * this one method. OAuth2-style providers (Drive, Dropbox, OneDrive) share
 * enough shape to extend OAuth2Authenticator below and get the loopback-server
 * flow for free. Something access-key based (e.g. S3) wouldn't fit an OAuth
 * shape at all and can implement this directly instead, with no browser
 * dance required.
 */
public interface CloudAuthenticator {
    String getValidAccessToken() throws IOException, InterruptedException;
}