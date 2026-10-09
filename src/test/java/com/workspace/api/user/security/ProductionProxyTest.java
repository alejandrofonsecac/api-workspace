package com.workspace.api.user.security;

import com.workspace.api.user.security.filter.SessionRequestFilter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

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

    @Test
    void renderProxyPreservesHttpsAndSecureCookies() throws Exception {
        checkProxy(true, "application-production.yml", "application-render.yml");
    }

    @Test
    void renderTrustsOnlyPrivateProxyAddresses() {
        var properties = load("application-production.yml", "application-render.yml");
        assertThat(properties.getProperty("server.address")).isEqualTo("0.0.0.0");
        var proxies = Pattern.compile(properties.getProperty("server.tomcat.remoteip.internal-proxies"));
        for (var address : new String[]{"10.214.3.7", "172.16.0.1", "172.31.255.2", "192.168.1.9",
                "100.64.0.1", "100.127.9.9", "127.0.0.1", "::1"}) {
            assertThat(proxies.matcher(address).matches()).as(address).isTrue();
        }
        for (var address : new String[]{"8.8.8.8", "172.32.0.1", "100.128.0.1", "192.169.0.1", "11.0.0.1"}) {
            assertThat(proxies.matcher(address).matches()).as(address).isFalse();
        }
    }

    private Properties load(String... resources) {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(Arrays.stream(resources).map(ClassPathResource::new).toArray(Resource[]::new));
        return yaml.getObject();
    }

    private void checkProxy(boolean trusted) throws Exception {
        checkProxy(trusted, "application-production.yml");
    }

    private void checkProxy(boolean trusted, String... resources) throws Exception {
        var properties = load(resources);
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
