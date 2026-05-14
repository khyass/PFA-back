package com.example.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that binds Keycloak-related properties from application.yml
 * (prefix "keycloak") and exposes a fully configured Keycloak admin client bean.
 *
 * <p>Two clients are involved:
 * <ul>
 *   <li><b>Admin client</b> – authenticated against the "master" realm using the
 *       Keycloak admin credentials. Used to manage users, roles, and credentials
 *       programmatically via the Keycloak Admin REST API.</li>
 *   <li><b>API client</b> (job-platform-api) – confidential client used for
 *       token introspection. Its id/secret are stored in {@code apiClientId} /
 *       {@code apiClientSecret}.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakConfig {

    /** Base URL of the Keycloak server, e.g. http://localhost:8180 */
    private String authServerUrl;

    /** Realm name where users, roles and clients are managed (e.g. "job-platform") */
    private String realm;

    /** Public client ID used by the Angular SPA (job-platform-app) */
    private String clientId;

    /** Confidential client ID used for backend-to-backend calls (token introspection) */
    private String apiClientId;

    /** Secret of the confidential API client – never exposed to the frontend */
    private String apiClientSecret;

    /** Keycloak admin username (used to authenticate the admin client) */
    private String adminUsername;

    /** Keycloak admin password (used to authenticate the admin client) */
    private String adminPassword;

    /**
     * Creates and registers a {@link Keycloak} admin client as a Spring bean.
     *
     * <p>The client authenticates against the "master" realm using the Resource Owner
     * Password Credentials grant ({@code grant_type=password}) with the built-in
     * "admin-cli" client, which requires no client secret in Keycloak's default setup.
     *
     * <p>This bean is used by {@code KeycloakService} to perform administrative
     * operations such as creating users and assigning realm roles.
     */
    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm("master")                        // Admin operations target the master realm
                .grantType(OAuth2Constants.PASSWORD)    // Resource Owner Password Credentials grant
                .clientId("admin-cli")                  // Built-in Keycloak admin CLI client
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    /**
     * Builds the token endpoint URL for the configured realm.
     * Used by {@code KeycloakService} to perform login and refresh token requests.
     *
     * @return full URL to the OpenID Connect token endpoint
     */
    public String getTokenUrl() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /**
     * Builds the logout endpoint URL for the configured realm.
     * A POST to this endpoint with the refresh_token invalidates the user session.
     *
     * @return full URL to the OpenID Connect logout endpoint
     */
    public String getLogoutUrl() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    /**
     * Builds the userinfo endpoint URL for the configured realm.
     * Can be used to retrieve claims about the authenticated user
     * directly from Keycloak using a valid access token.
     *
     * @return full URL to the OpenID Connect userinfo endpoint
     */
    public String getUserInfoUrl() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
    }

    /**
     * Builds the token introspection endpoint URL for the configured realm.
     * The introspect endpoint is used by confidential clients to validate
     * tokens and retrieve their metadata (active, scope, sub, etc.).
     *
     * @return full URL to the OpenID Connect token introspection endpoint
     */
    public String getIntrospectUrl() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
    }
}
