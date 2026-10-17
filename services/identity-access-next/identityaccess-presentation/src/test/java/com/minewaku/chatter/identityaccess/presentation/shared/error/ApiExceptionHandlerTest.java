package com.minewaku.chatter.identityaccess.presentation.shared.error;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.presentation.SliceSecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Every failure, wherever it comes from, has the one standard shape with a stable code and a fixed message. */
@WebMvcTest(ErrorProbeController.class)
@Import({SliceSecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class ApiExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    private static RequestPostProcessor caller() {
        return jwt().jwt(token -> token.subject("42").claim("sid", "s"));
    }

    private static void assertStandardShape(ResultActions result, ErrorCode code) throws Exception {
        result.andExpect(status().is(code.status().value()))
                .andExpect(jsonPath("$.status").value(code.status().value()))
                .andExpect(jsonPath("$.code").value(code.code()))
                .andExpect(jsonPath("$.message").value(code.message()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.containsString("secret"),
                                org.hamcrest.Matchers.containsString("Exception"),
                                org.hamcrest.Matchers.containsString("com.example"),
                                org.hamcrest.Matchers.containsString("at com.")))));
    }

    @Test
    void validationFailuresListFieldsAndNoValues() throws Exception {
        ResultActions result = mvc.perform(post("/probe/validation")
                .with(caller())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"));

        assertStandardShape(result, ErrorCode.VALIDATION_FAILED);
        result.andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    void coreValueObjectRulesNeverShowTheirMessage() throws Exception {
        assertStandardShape(mvc.perform(get("/probe/illegal-argument").with(caller())), ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void anUnexpectedErrorIsAGenericServerError() throws Exception {
        assertStandardShape(mvc.perform(get("/probe/unexpected").with(caller())), ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void coreFailuresMapToTheirDocumentedCodes() throws Exception {
        assertStandardShape(
                mvc.perform(get("/probe/authentication-failed").with(caller())), ErrorCode.AUTHENTICATION_FAILED);
        assertStandardShape(
                mvc.perform(get("/probe/reauthentication-failed").with(caller())), ErrorCode.REAUTHENTICATION_FAILED);
        assertStandardShape(
                mvc.perform(get("/probe/invalid-credentials").with(caller())), ErrorCode.REAUTHENTICATION_FAILED);
        assertStandardShape(mvc.perform(get("/probe/inaccessible").with(caller())), ErrorCode.ACCOUNT_INACCESSIBLE);
        assertStandardShape(
                mvc.perform(get("/probe/account-inaccessible").with(caller())), ErrorCode.ACCOUNT_INACCESSIBLE);
        assertStandardShape(mvc.perform(get("/probe/duplicate").with(caller())), ErrorCode.ACCOUNT_ALREADY_EXISTS);
    }

    @Test
    void frameworkErrorsAreRewrittenIntoTheStandardShape() throws Exception {
        assertStandardShape(
                mvc.perform(post("/probe/validation")
                        .with(caller())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json")),
                ErrorCode.MALFORMED_REQUEST);
        assertStandardShape(
                mvc.perform(post("/probe/validation")
                        .with(caller())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name")),
                ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        assertStandardShape(mvc.perform(post("/probe/unexpected").with(caller())), ErrorCode.METHOD_NOT_ALLOWED);
        assertStandardShape(mvc.perform(get("/probe/nothing-here").with(caller())), ErrorCode.NOT_FOUND);
    }

    @Test
    void securityChainFailuresUseTheSameShape() throws Exception {
        ResultActions unauthenticated = mvc.perform(get("/probe/unexpected"));

        assertStandardShape(unauthenticated, ErrorCode.UNAUTHENTICATED);
        unauthenticated.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void errorFieldsAreExactlyTheDocumentedOnesOutsideValidation() throws Exception {
        mvc.perform(get("/probe/duplicate").with(caller()))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.length()").value(4));
        List<String> codes =
                java.util.Arrays.stream(ErrorCode.values()).map(ErrorCode::code).toList();
        org.assertj.core.api.Assertions.assertThat(codes)
                .doesNotHaveDuplicates()
                .allMatch(c -> c.equals(c.toLowerCase()));
    }
}
