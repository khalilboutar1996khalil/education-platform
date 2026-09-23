package com.example.education_platform.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.common.exception.GlobalExceptionHandlerTest.TestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Verifies every exception path returns the standard RFC 9457 ProblemDetail shape and status. */
@WebMvcTest(controllers = TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void resourceNotFoundReturns404WithDomainMessage() {
        assertThat(mvc.get().uri("/test/not-found"))
                .hasStatus(404)
                .bodyJson()
                .satisfies(json -> json.assertThat().extractingPath("$.detail").isEqualTo("Course not found: 42"));
    }

    @Test
    void conflictReturns409WithDomainMessage() {
        assertThat(mvc.get().uri("/test/conflict"))
                .hasStatus(409)
                .bodyJson()
                .satisfies(json -> json.assertThat().extractingPath("$.detail").isEqualTo("Email already used"));
    }

    @Test
    void businessRuleViolationReturns422() {
        assertThat(mvc.get().uri("/test/business")).hasStatus(422);
    }

    @Test
    void unexpectedErrorReturns500WithGenericFrenchMessage() {
        assertThat(mvc.get().uri("/test/boom"))
                .hasStatus(500)
                .bodyJson()
                .satisfies(json -> json.assertThat().extractingPath("$.detail")
                        .isEqualTo("Une erreur inattendue est survenue"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Course", 42);
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Email already used");
        }

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException("Quiz already closed");
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("kaboom");
        }
    }
}
