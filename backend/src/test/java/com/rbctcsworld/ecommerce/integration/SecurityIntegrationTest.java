package com.rbctcsworld.ecommerce.integration;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 5: security behaviour of the whole application (filters, headers, lockout, JWT, docs). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.context.ApplicationContext context;

    private static String email() {
        return "sec-" + UUID.randomUUID() + "@test.com";
    }

    private ResultActions register(String email, String extraJson) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password1!\"" + extraJson + "}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void securityHeadersOnPublicAndErrorResponses() throws Exception {
        for (ResultActions r : new ResultActions[]{mvc.perform(get("/api/products")), mvc.perform(get("/api/cart"))}) {
            r.andExpect(header().string("X-Content-Type-Options", "nosniff"))
             .andExpect(header().string("X-Frame-Options", "DENY"))
             .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
             .andExpect(header().string("Referrer-Policy", "no-referrer"))
             .andExpect(header().string("Cache-Control", containsString("no-store")));
        }
    }

    @Test
    void fiveWrongPasswordsLockTheAccountEvenForTheRightPassword() throws Exception {
        String victim = email();
        register(victim, "").andExpect(status().isCreated());
        for (int i = 0; i < 5; i++) login(victim, "Wrong-Pass-" + i).andExpect(status().isUnauthorized());

        login(victim, "Password1!")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("^(89\\d|900)$")))
                .andExpect(jsonPath("$.message").value(containsString("Too many failed login attempts")));

        String bystander = email();
        register(bystander, "").andExpect(status().isCreated());
        login(bystander, "Password1!").andExpect(status().isOk());       // other accounts unaffected
    }

    @Test
    void unknownEmailsAreLockedTooSo429RevealsNothing() throws Exception {
        String ghost = email();
        for (int i = 0; i < 5; i++) login(ghost, "Wrong-Pass-1").andExpect(status().isUnauthorized());
        login(ghost, "Wrong-Pass-1").andExpect(status().isTooManyRequests());
    }

    @Test
    void roleInTheRegistrationBodyIsIgnored() throws Exception {
        String sneaky = email();
        String token = mapper.readTree(register(sneaky, ",\"role\":\"ADMIN\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString()).get("token").asString();
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void forgedTokensAreTreatedAsAnonymous() throws Exception {
        String b64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"rbctcsworld-ecommerce\",\"sub\":\"admin@rbctcsworld.com\"}".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + b64 + "." + payload + "."))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders").header("Authorization", "Basic YWRtaW46YWRtaW4="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheHealthEndpointOfActuatorIsReachable() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("diskSpace"))));   // no internal details
        for (String path : new String[]{"/actuator/env", "/actuator/beans", "/actuator/configprops", "/actuator/heapdump"}) {
            mvc.perform(get(path)).andExpect(result -> {
                int s = result.getResponse().getStatus();
                if (s != 401 && s != 404) throw new AssertionError(path + " answered " + s);
            });
        }
    }

    @Test
    void unexpectedErrorsDoNotLeakInternals() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at com."))));
        mvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("NumberFormatException"))));
    }

    @Test
    void foreignWebsitesGetNoCorsPermission() throws Exception {
        mvc.perform(options("/api/products")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/api/products").header("Origin", "https://evil.example"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void openApiSpecIsPublishedForSecurityScanners() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }

    /**
     * Guard for the accepted risk in backend/osv-scanner.toml: CVE-2026-47884 (spring-webmvc, CVSS 9.8)
     * is exploitable only through XsltView rendering. This API renders no views at all; if anyone adds
     * an XSLT view (or any view resolver that renders templates), this test fails and the exception must be revisited.
     */
    @Test
    void noXsltViewRenderingIsConfigured_CVE_2026_47884() {
        assertThat(context.getBeanNamesForType(org.springframework.web.servlet.view.xslt.XsltViewResolver.class)).isEmpty();
        assertThat(context.getBeanNamesForType(org.springframework.web.servlet.view.xslt.XsltView.class)).isEmpty();
        assertThat(context.getBeansWithAnnotation(org.springframework.stereotype.Controller.class).values().stream()
                .filter(b -> org.springframework.aop.support.AopUtils.getTargetClass(b).getPackageName().startsWith("com.rbctcsworld"))
                .toList())
                .as("every controller of ours must be a @RestController (JSON only, no view rendering)")
                .isNotEmpty()
                .allSatisfy(bean -> assertThat(org.springframework.core.annotation.AnnotationUtils.findAnnotation(
                        org.springframework.aop.support.AopUtils.getTargetClass(bean),
                        org.springframework.web.bind.annotation.RestController.class)).isNotNull());
    }

    // GHSA-j9f9-w8pj-32f8 (spring-webmvc 6.2.19, CVSS 9.8, no 6.2.x fix): Server-Sent Events. Exploitable only when the
    // application streams SSE (WebMvc.fn ServerResponse.sse(...) / SseEmitter / view fragments over SSE) with
    // attacker-controlled text. Accepted in osv-scanner.toml as long as this test proves we stream no SSE at all.
    @Test
    void noServerSentEventsOrFunctionalEndpoints_GHSA_j9f9_w8pj_32f8() {
        assertThat(context.getBeanNamesForType(org.springframework.web.servlet.function.RouterFunction.class))
                .as("no WebMvc.fn endpoints (ServerResponse.sse)").isEmpty();
        var mapping = context.getBean("requestMappingHandlerMapping",
                org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);
        assertThat(mapping.getHandlerMethods().values())
                .filteredOn(h -> h.getBeanType().getPackageName().startsWith("com.rbctcsworld"))
                .as("our endpoints").isNotEmpty()
                .allSatisfy(h -> assertThat(h.getReturnType().getGenericParameterType().getTypeName())
                        .as("%s must not stream SSE or render view fragments", h)
                        .doesNotContain("Emitter").doesNotContain("FragmentsRendering").doesNotContain("ServerSentEvent")
                        .doesNotContain("ModelAndView").doesNotContain("Flux"));
    }
}
