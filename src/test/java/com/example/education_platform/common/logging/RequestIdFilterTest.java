package com.example.education_platform.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdFilterTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void everyResponseCarriesARequestId() {
        String id = idOf(mvc.get().uri("/actuator/health").exchange());

        assertThat(id).isNotNull().isNotBlank();
    }

    @Test
    void anUnauthenticatedRequestIsTraceableToo() {
        MvcTestResult result = mvc.get().uri("/api/v1/courses").exchange();

        assertThat(result).hasStatus(401);
        assertThat(idOf(result))
                .describedAs("a 401 nobody can find in the logs is a 401 nobody can explain")
                .isNotNull().isNotBlank();
    }

    @Test
    void twoRequestsGetDifferentIds() {
        String first = idOf(mvc.get().uri("/actuator/health").exchange());
        String second = idOf(mvc.get().uri("/actuator/health").exchange());

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void aCallersOwnIdIsKeptSoATraceCanSpanServices() {
        String supplied = "frontend-7f3a2b91";

        assertThat(idOf(mvc.get().uri("/actuator/health")
                .header(RequestIdFilter.HEADER, supplied).exchange()))
                .isEqualTo(supplied);
    }

    @Test
    void aHeaderCarryingNewlinesCannotForgeLogLines() {
        String forged = "abc\r\n2026-05-01 ERROR [admin] deleted everything";

        String id = idOf(mvc.get().uri("/actuator/health")
                .header(RequestIdFilter.HEADER, forged).exchange());

        assertThat(id)
                .describedAs("the line break is what would forge a log entry")
                .doesNotContain("\n").doesNotContain("\r");
        assertThat(id)
                .describedAs("hyphens and dots are legitimate in an id, so they survive; spaces "
                        + "and brackets do not")
                .isEqualTo("abc2026-05-01ERRORadmindeletedeverything");
    }

    @Test
    void anOverlongHeaderIsTruncatedRatherThanFloodingEveryLine() {
        String id = idOf(mvc.get().uri("/actuator/health")
                .header(RequestIdFilter.HEADER, "x".repeat(5000)).exchange());

        assertThat(id).hasSize(64);
    }

    @Test
    void aHeaderOfNothingButPunctuationFallsBackToAGeneratedId() {
        String id = idOf(mvc.get().uri("/actuator/health")
                .header(RequestIdFilter.HEADER, "!!!@@@###").exchange());

        assertThat(id).isNotBlank().hasSize(36);
    }

    private static String idOf(MvcTestResult result) {
        return result.getResponse().getHeader(RequestIdFilter.HEADER);
    }
}
