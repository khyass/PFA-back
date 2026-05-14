package com.example.authservice.service;

import com.example.authservice.config.KeycloakConfig;
import com.example.authservice.dto.*;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @InjectMocks
    private KeycloakService keycloakService;

    @Mock
    private Keycloak keycloakAdmin;

    @Mock
    private KeycloakConfig keycloakConfig;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @BeforeEach
    void setUp() {
        lenient().when(keycloakConfig.getRealm()).thenReturn("job-platform");
        lenient().when(keycloakConfig.getClientId()).thenReturn("job-platform-app");
        lenient().when(keycloakConfig.getTokenUrl()).thenReturn("http://localhost:8180/realms/job-platform/protocol/openid-connect/token");
        lenient().when(keycloakConfig.getLogoutUrl()).thenReturn("http://localhost:8180/realms/job-platform/protocol/openid-connect/logout");
        lenient().when(keycloakAdmin.realm("job-platform")).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
        lenient().when(realmResource.roles()).thenReturn(rolesResource);
    }

    @Nested
    @DisplayName("Register Candidate")
    class RegisterCandidateTests {

        @Test
        @DisplayName("Should register candidate successfully")
        void shouldRegisterCandidateSuccessfully() {
            // Given
            RegisterCandidateRequest request = RegisterCandidateRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .password("securePass123")
                    .skills(List.of("Java", "Spring"))
                    .phone("+1234567890")
                    .build();

            when(usersResource.searchByEmail("john@example.com", true)).thenReturn(Collections.emptyList());

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(201);
            when(mockResponse.getHeaderString("Location")).thenReturn("http://localhost:8180/admin/realms/job-platform/users/user-123");
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);

            RoleRepresentation candidateRole = new RoleRepresentation();
            candidateRole.setName("CANDIDATE");
            when(rolesResource.get("CANDIDATE")).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(candidateRole);
            when(usersResource.get("user-123")).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

            // When
            UserProfile result = keycloakService.registerCandidate(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("user-123");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getRoles()).contains("CANDIDATE");

            verify(usersResource).create(any(UserRepresentation.class));
            verify(roleScopeResource).add(anyList());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email already registered")
        void shouldThrowWhenUserAlreadyExists() {
            // Given
            RegisterCandidateRequest request = RegisterCandidateRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("existing@example.com")
                    .password("securePass123")
                    .build();

            UserRepresentation existingUser = new UserRepresentation();
            existingUser.setEmail("existing@example.com");
            when(usersResource.searchByEmail("existing@example.com", true)).thenReturn(List.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> keycloakService.registerCandidate(request))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("Register Enterprise")
    class RegisterEnterpriseTests {

        @Test
        @DisplayName("Should register enterprise successfully")
        void shouldRegisterEnterpriseSuccessfully() {
            // Given
            RegisterEnterpriseRequest request = RegisterEnterpriseRequest.builder()
                    .companyName("Tech Corp")
                    .email("contact@techcorp.com")
                    .password("securePass123")
                    .industry("Technology")
                    .website("https://techcorp.com")
                    .build();

            when(usersResource.searchByEmail("contact@techcorp.com", true)).thenReturn(Collections.emptyList());

            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(201);
            when(mockResponse.getHeaderString("Location")).thenReturn("http://localhost:8180/admin/realms/job-platform/users/enterprise-456");
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);

            RoleRepresentation enterpriseRole = new RoleRepresentation();
            enterpriseRole.setName("ENTERPRISE");
            when(rolesResource.get("ENTERPRISE")).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(enterpriseRole);
            when(usersResource.get("enterprise-456")).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

            // When
            UserProfile result = keycloakService.registerEnterprise(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("enterprise-456");
            assertThat(result.getEmail()).isEqualTo("contact@techcorp.com");
            assertThat(result.getRoles()).contains("ENTERPRISE");
            assertThat(result.getAttributes()).containsKey("companyName");
        }
    }

    @Nested
    @DisplayName("Get User Profile From Token")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should extract profile from JWT with realm_access roles")
        void shouldExtractProfileFromJwt() {
            // Given
            Map<String, Object> realmAccess = Map.of("roles", List.of("CANDIDATE", "default-roles-job-platform"));
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "RS256")
                    .subject("user-123")
                    .claim("email", "john@example.com")
                    .claim("given_name", "John")
                    .claim("family_name", "Doe")
                    .claim("realm_access", realmAccess)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(900))
                    .build();

            // When
            UserProfile profile = keycloakService.getUserProfileFromToken(jwt);

            // Then
            assertThat(profile.getId()).isEqualTo("user-123");
            assertThat(profile.getEmail()).isEqualTo("john@example.com");
            assertThat(profile.getFirstName()).isEqualTo("John");
            assertThat(profile.getLastName()).isEqualTo("Doe");
            assertThat(profile.getRoles()).contains("CANDIDATE");
            assertThat(profile.getRoles()).doesNotContain("default-roles-job-platform");
        }

        @Test
        @DisplayName("Should extract profile from JWT with top-level roles claim")
        void shouldExtractProfileFromTopLevelRoles() {
            // Given
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "RS256")
                    .subject("user-456")
                    .claim("email", "enterprise@example.com")
                    .claim("given_name", "Corp")
                    .claim("family_name", "")
                    .claim("roles", List.of("ENTERPRISE"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(900))
                    .build();

            // When
            UserProfile profile = keycloakService.getUserProfileFromToken(jwt);

            // Then
            assertThat(profile.getRoles()).contains("ENTERPRISE");
        }
    }
}
