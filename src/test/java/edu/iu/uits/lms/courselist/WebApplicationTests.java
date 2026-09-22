package edu.iu.uits.lms.courselist;

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

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.ac.ox.ctl.lti13.nrps.NamesRoleService;

import javax.sql.DataSource;

/**
 * Full-context smoke test for the real {@link WebApplication} class - see viewem's identically
 * named/purposed test for the full rationale. Unlike the narrow test slices elsewhere in this
 * module, this boots {@link WebApplication} itself with no {@code classes=} override, so every
 * {@code @Enable*} annotation actually runs, exactly as it would in production. This is the test
 * that would have caught the real startup failure this class exists to guard against: courselist
 * has no JPA config/entities of its own (unlike viewem, which has a local {@code PostgresDBConfig}
 * with a {@code @Primary}-annotated {@code EntityManagerFactory}), so with both
 * {@code lti-framework}'s {@code LtiClientConfig} and {@code canvas-oauth2-client}'s
 * {@code CanvasOAuth2ClientConfig} on the classpath, Spring Boot's open-in-view interceptor
 * autoconfiguration found two {@code EntityManagerFactory} beans and no way to pick one - fixed by
 * setting {@code spring.jpa.open-in-view: false} (see {@code application.yml}), which removes the
 * ambiguous injection point entirely rather than requiring courselist to grow a {@code @Primary}
 * bean it doesn't otherwise need.
 * <p>
 * {@code LtiClientConfig}'s and {@code CanvasOAuth2ClientConfig}'s datasource beans are each
 * {@code @ConditionalOnMissingBean(DataSource.class)}, so only one actually gets created (whichever
 * config Spring processes first) and the other silently reuses it - courselist has no bean of its
 * own to override by name, so the single resulting {@code DataSource} bean is mocked here by type
 * rather than by a specific bean name.
 */
@SpringBootTest(properties = {
        "oauth.tokenprovider.url=http://foo",
        "lms.db.poolType=",
        "canvas.token=test-only-placeholder-token",
        "catalog.token=test-only-placeholder-token",
        "lti.errorcontact.name=foo",
        "lti.errorcontact.link=foo",
        "canvas.oauth2.encryptionPassword=test-only-placeholder-password",
        "canvas.oauth2.encryptionSalt=deadbeef"
})
class WebApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private BufferingApplicationStartup bufferingApplicationStartup;

    // LtiClientConfig.namesRoleService() eagerly calls Lti13Service.getJKS(), which runs a real
    // JPA query (findFirstByOrderByIdAsc()) against the (mocked) datasource during singleton
    // pre-instantiation - unlike every other JPA repository bean here, it can't just ride along
    // on lazy Hibernate bootstrapping against a mocked DataSource. Overriding the bean entirely
    // skips that factory method (and its eager query) altogether. Same fix as viewem's identical
    // test needed for the same reason.
    @MockitoBean
    private NamesRoleService namesRoleService;

    @Test
    void contextLoads() {
        // Intentionally empty - just needs the ApplicationContext to refresh successfully.
    }

}
