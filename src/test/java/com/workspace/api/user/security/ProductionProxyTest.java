package com.workspace.api.user.security;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionProxyTest {
    @TempDir Path directory;

    @Test
    void trustedProxyPreservesHttpsAndSecureCookies() throws Exception {
        checkProxy(true);
    }

    @Test
    void untrustedPeerCannotClaimHttps() throws Exception {
        checkProxy(false);
    }

    private void checkProxy(boolean trusted) throws Exception {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-production.yml"));
        var properties = yaml.getObject();
        assertThat(properties.getProperty("server.forward-headers-strategy")).isEqualTo("native");
        var tomcat = new Tomcat();
        tomcat.setBaseDir(directory.toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", properties.getProperty("server.address"));
        var valve = new RemoteIpValve();
        valve.setInternalProxies(trusted
                ? properties.getProperty("server.tomcat.remoteip.internal-proxies") : "192\\.0\\.2\\.1");
        valve.setProtocolHeader(properties.getProperty("server.tomcat.remoteip.protocol-header"));
        valve.setRemoteIpHeader(properties.getProperty("server.tomcat.remoteip.remote-ip-header"));
        tomcat.getEngine().getPipeline().addValve(valve);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        var cookie = new RefreshCookie(environment);
        var filter = new SessionRequestFilter(cookie);
        var context = tomcat.addContext("", directory.toString());
        Tomcat.addServlet(context, "session", new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                filter.doFilter(request, response, (req, res) -> {
                    cookie.write(response, "test-only", Duration.ofDays(1));
                    response.setStatus(204);
                });
            }
        });
        context.addServletMappingDecoded("/users/login", "session");
        try {
            tomcat.start();
            var uri = URI.create("http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/users/login");
            try (var client = HttpClient.newHttpClient()) {
                for (var protocol : new String[]{"", "http", "https"}) {
                    var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                            .header("X-PlannerFy-Session", "1").POST(HttpRequest.BodyPublishers.noBody());
                    if (!protocol.isEmpty()) builder.header("X-Forwarded-Proto", protocol);
                    // RFC Forwarded alone must not override the native trusted-peer check.
                    builder.header("Forwarded", "proto=https");
                    var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                    boolean secure = trusted && protocol.equals("https");
                    assertThat(response.statusCode()).isEqualTo(secure ? 204 : 426);
                    if (secure) {
                        assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                                .contains("Secure", "HttpOnly", "SameSite=Strict");
                    } else {
                        assertThat(response.body()).contains("HTTPS_REQUIRED");
                    }
                }
            }
        } finally {
            tomcat.stop();
            tomcat.destroy();
        }
    }
}
