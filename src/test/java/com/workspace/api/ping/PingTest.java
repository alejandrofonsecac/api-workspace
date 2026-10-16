package com.workspace.api.ping;

import com.workspace.api.user.security.RefreshCookie;
import com.workspace.api.user.security.filter.SessionRequestFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PingTest {
    @Autowired MockMvc mvc;

    @Test
    void anonymousGetAndHeadReturnNoContentWithoutBodyOrSession() throws Exception {
        for (var request : new MockHttpServletRequestBuilder[]{get("/ping"), head("/ping")}) {
            mvc.perform(request)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""))
                    .andExpect(header().doesNotExist("Content-Type"))
                    .andExpect(header().doesNotExist("Set-Cookie"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    @Test
    void otherMethodsOnPingStayProtected() throws Exception {
        for (var method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            mvc.perform(request(method, "/ping")).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ping/", "/ping/x", "/ping.json", "/pings", "/x/ping"})
    void onlyTheExactPathIsPublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test
    void productionStillRequiresForwardedHttps() throws Exception {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        var filter = new SessionRequestFilter(new RefreshCookie(environment));
        var response = new MockHttpServletResponse();
        var reached = new AtomicBoolean(false);
        filter.doFilter(new MockHttpServletRequest("GET", "/ping"), response, (req, res) -> reached.set(true));
        assertThat(response.getStatus()).isEqualTo(426);
        assertThat(reached).isFalse();
    }
}
