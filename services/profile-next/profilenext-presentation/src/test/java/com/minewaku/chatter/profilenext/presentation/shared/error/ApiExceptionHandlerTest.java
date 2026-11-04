package com.minewaku.chatter.profilenext.presentation.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    record Body(String name) {}

    @RestController
    static class StubController {

        @PostMapping("/stub")
        void post(@RequestBody Body body) {}

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret detail that must not leak");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void malformedJsonIsAMalformedRequest() throws Exception {
        mvc.perform(post("/stub").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("malformed_request"))
                .andExpect(jsonPath("$.message").value("The request could not be read."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void aWrongMethodIsMethodNotAllowed() throws Exception {
        mvc.perform(get("/stub"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("method_not_allowed"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void anUnexpectedFailureIsAnInternalErrorThatLeaksNothing() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }
}
