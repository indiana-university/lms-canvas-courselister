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

import edu.iu.uits.lms.canvas.config.CanvasConfiguration;
import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Registration;
import edu.iu.uits.lms.canvasoauth2.controller.CanvasOAuth2ConsentText;
import edu.iu.uits.lms.canvasoauth2.controller.OAuth2CallbackController;
import edu.iu.uits.lms.canvasoauth2.controller.OAuth2ConsentControllerAdvice;
import edu.iu.uits.lms.canvasoauth2.security.CanvasOAuth2AuthorizedClientRepository;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.courselist.config.SecurityConfig;
import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@link OAuth2CallbackController} through the real
 * {@code /login/oauth2/code/lms_canvas_oauth2_courselist} mapping and the real {@link SecurityConfig}
 * filter chain, so a mistake in the {@code @GetMapping} path or in the security matcher configuration
 * would actually be caught - mirrors viewem's own {@code OAuth2CallbackControllerMvcTest}, which courselist
 * never had an equivalent of.
 */
@WebMvcTest(value = OAuth2CallbackController.class,
        properties = {"oauth.tokenprovider.url=http://foo", "canvas.baseUrl=https://canvas.test"})
@ContextConfiguration(classes = {ToolConfig.class, CanvasConfiguration.class, OAuth2CallbackController.class,
        OAuth2ConsentControllerAdvice.class, SecurityConfig.class, CanvasOAuth2ConsentText.class,
        OAuth2CallbackControllerMvcTest.TestConfig.class})
public class OAuth2CallbackControllerMvcTest {

    private static final String REGISTRATION_ID = "lms_canvas_oauth2_courselist";

    /**
     * See {@code AppLaunchSecurityTest.TestConfig}'s javadoc (viewem) for why this narrow slice
     * supplies {@code CanvasOAuth2Registration} directly instead of relying on component-scan/ImportAware.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public CanvasOAuth2Registration canvasOAuth2Registration() {
            return new CanvasOAuth2Registration("courselist", "/jsrivet");
        }

        /**
         * A plain {@code @Bean} rather than {@code @MockitoBean} - see
         * {@code CourselistControllerConsentTest.TestConfig}'s javadoc for why: {@code CanvasOAuth2AuthorizedClientRepository}
         * also implements {@code OAuth2AuthorizedClientRepository}, which
         * {@code OAuth2ClientWebSecurityAutoConfiguration} auto-configures its own default bean for, and
         * a same-named {@code @MockitoBean} of the narrower concrete type doesn't suppress that.
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
    @MockitoBean(name = ServerInfo.BEAN_NAME)
    private ServerInfo serverInfo;
    // SecurityConfig now @Autowired-injects this from CanvasOAuth2ClientConfig, which this narrow
    // @WebMvcTest slice deliberately doesn't pull in (see TestConfig above) - it's never invoked by
    // any of these tests, only needed to satisfy the filter chain's dependency at context-build time.
    @MockitoBean
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient;

    @Test
    public void callbackWithoutErrorRedirectsStraightToBaseUrl() throws Exception {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "1234", LTIConstants.BASE_USER_AUTHORITY);

        mvc.perform(get("/login/oauth2/code/" + REGISTRATION_ID)
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("https://canvas.test"));
    }

    @Test
    public void callbackWithErrorParamRendersConnectInterstitial() throws Exception {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "1234", LTIConstants.BASE_USER_AUTHORITY);

        // Simulates OAuth2AuthorizationCodeGrantFilter's real redirect-on-failure behavior: Canvas
        // rejected the authorization code, and the filter bounced the browser back to this exact URL
        // with error/error_description query parameters rather than completing the connection.
        mvc.perform(get("/login/oauth2/code/" + REGISTRATION_ID)
                        .param("error", "invalid_grant")
                        .param("error_description", "Authorization code expired")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("connectCanvas"))
                .andExpect(MockMvcResultMatchers.model().attribute("authorizationUri",
                        "/oauth2/authorization/" + REGISTRATION_ID));
    }

    @Test
    public void callbackWithoutAuthenticationIsForbidden() throws Exception {
        // This mapping sits behind SecurityConfig's catchall filter chain (hasAuthority(BASE_USER_AUTHORITY)),
        // same as every other courselist URL - a mid-flow OAuth2 callback still arrives on the same
        // browser session that already carries the LTI-launch authentication, so it must not be
        // reachable anonymously.
        mvc.perform(get("/login/oauth2/code/" + REGISTRATION_ID)
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
