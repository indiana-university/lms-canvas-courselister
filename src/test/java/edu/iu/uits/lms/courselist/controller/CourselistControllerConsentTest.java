package edu.iu.uits.lms.courselist.controller;

/*-
 * #%L
 * lms-lti-courselist
 * %%
 * Copyright (C) 2015 - 2026 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Registration;
import edu.iu.uits.lms.canvasoauth2.security.CanvasOAuth2AuthorizedClientRepository;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.courselist.config.SecurityConfig;
import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.courselist.service.CourseListService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CourselistController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {ToolConfig.class, CourselistController.class, SecurityConfig.class,
        edu.iu.uits.lms.canvasoauth2.controller.OAuth2ConsentControllerAdvice.class,
        edu.iu.uits.lms.canvasoauth2.controller.CanvasOAuth2ConsentText.class,
        CourselistControllerConsentTest.TestConfig.class})
public class CourselistControllerConsentTest {

    private static final String REGISTRATION_ID = "lms_canvas_oauth2_courselist";

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CanvasOAuth2Registration canvasOAuth2Registration() {
            return new CanvasOAuth2Registration("courselist", "/jsrivet");
        }

        /**
         * A plain {@code @Bean} rather than {@code @MockitoBean}: {@code CanvasOAuth2AuthorizedClientRepository}
         * also implements {@code OAuth2AuthorizedClientRepository}, which
         * {@code OAuth2ClientWebSecurityAutoConfiguration} auto-configures its own default bean for via
         * {@code @ConditionalOnMissingBean}. That condition check doesn't recognize a same-named
         * {@code @MockitoBean} of the narrower concrete type as already satisfying it, so both beans get
         * created and autowiring the interface type elsewhere becomes ambiguous. A regular {@code @Bean}
         * factory method participates in that condition check correctly and satisfies both the concrete
         * type ({@code OAuth2ConsentControllerAdvice}'s dependency) and the interface type
         * ({@code SecurityConfig}'s filter chain) from a single instance.
         */
        @Bean
        public CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository() {
            return Mockito.mock(CanvasOAuth2AuthorizedClientRepository.class);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LmsDefaultGrantedAuthoritiesMapper lmsDefaultGrantedAuthoritiesMapper;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private CourseListService courseListService;
    @MockitoBean
    private CanvasService canvasService;
    @MockitoBean(name = ServerInfo.BEAN_NAME)
    private ServerInfo serverInfo;
    @MockitoBean(name = "CanvasRestTemplateAsUser")
    private RestTemplate canvasRestTemplateAsUser;
    // Provided by TestConfig's @Bean (not @MockitoBean - see its javadoc). Satisfies both
    // OAuth2ConsentControllerAdvice's concrete-type dependency and SecurityConfig's interface-type one.
    @Autowired
    private CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository;
    @MockitoBean
    private org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient<org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient;

    @BeforeEach
    void resetCanvasOAuth2AuthorizedClientRepositoryMock() {
        // TestConfig's @Bean isn't a @MockitoBean, so it doesn't get Mockito's automatic reset-between-
        // tests behavior - do it manually, since the ApplicationContext (and this same mock instance) is
        // cached and reused across every test method in this class.
        reset(canvasOAuth2AuthorizedClientRepository);
        // Defaults to "resolvable" so both tests below - built from fully-populated
        // OidcAuthenticationTokens - are unaffected by OAuth2ConsentControllerAdvice's fail-fast check.
        when(canvasOAuth2AuthorizedClientRepository.hasResolvableCanvasUserId(any())).thenReturn(true);
    }

    @Test
    public void listRequiresCanvasOAuth2ConsentWhenNoAuthorizedClient() throws Exception {
        when(canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(eq(REGISTRATION_ID), any(), any())).thenReturn(null);

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.BASE_USER_AUTHORITY,
                new HashMap<>(), new HashMap<>());

        mvc.perform(get("/list")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("connectCanvas"))
                .andExpect(MockMvcResultMatchers.model().attribute("authorizationUri", "/oauth2/authorization/" + REGISTRATION_ID));

        verifyNoInteractions(canvasService);
    }

    @Test
    public void listRendersWhenAuthorizedClientExists() throws Exception {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client")
                .clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://canvas.test/login/oauth2/auth")
                .tokenUri("https://canvas.test/login/oauth2/token")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-access-token", Instant.now(), Instant.now().plusSeconds(3600));
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, "userId", accessToken);
        when(canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(eq(REGISTRATION_ID), any(), any()))
                .thenReturn(authorizedClient);
        when(canvasService.getBaseUrl()).thenReturn("https://canvas.test");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.BASE_USER_AUTHORITY,
                new HashMap<>(), new HashMap<>());

        mvc.perform(get("/list")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("react"));
    }
}
