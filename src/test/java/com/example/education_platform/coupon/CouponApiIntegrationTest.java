package com.example.education_platform.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.coupon.entity.Coupon;
import com.example.education_platform.coupon.repository.CouponRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Book coupons: the admin printing a batch, and a student activating their book with one. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// The test profile all but disables throttling; this class checks it, so it restores a real limit
@TestPropertySource(properties = {
        "app.security.login.max-failures=5",
        "app.security.login.lockout=15m"
})
class CouponApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private CouponRepository coupons;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN, passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
    }

    // ---------- printing ----------

    @Test
    void anAdminGeneratesOneCouponPerCopyEachWithItsOwnSerialAndCode() throws Exception {
        JsonNode batch = createBatch(ADMIN, "SEVENTH_BASE", 5, "Livre 7e");

        assertThat(batch.at("/quantity").asInt()).isEqualTo(5);
        assertThat(batch.at("/level").asText()).isEqualTo("SEVENTH_BASE");
        assertThat(batch.at("/redeemed").asLong()).isZero();

        JsonNode list = listCoupons(batch.at("/id").asLong());
        assertThat(list.size()).isEqualTo(5);
        Set<String> codes = new HashSet<>();
        list.forEach(coupon -> codes.add(coupon.at("/code").asText()));
        assertThat(codes).describedAs("every copy has its own secret").hasSize(5);
        assertThat(list.get(0).at("/serial").asText()).isEqualTo(batch.at("/id").asLong() + "-0001");
        assertThat(list.get(0).at("/code").asText()).matches("[A-Z2-9]{8}");
    }

    @Test
    void batchesAreListedWithHowManyCouponsWereUsed() throws Exception {
        JsonNode batch = createBatch(ADMIN, "SEVENTH_BASE", 2, null);
        JsonNode first = listCoupons(batch.at("/id").asLong()).get(0);
        bodyOf(redeem(first.at("/serial").asText(), first.at("/code").asText(), "Amina", "Bensalem"), 201);

        JsonNode page = bodyOf(get("/api/v1/coupons/batches", ADMIN), 200);

        assertThat(page.at("/content/0/redeemed").asLong()).isEqualTo(1);
        JsonNode used = listCoupons(batch.at("/id").asLong()).get(0);
        assertThat(used.at("/redeemed").asBoolean()).isTrue();
        assertThat(used.at("/redeemedByName").asText()).isEqualTo("Amina Bensalem");
        assertThat(used.at("/redeemedByUsername").asText()).isEqualTo("amina.bensalem");
    }

    @Test
    void aStudentCannotGenerateOrSeeCoupons() {
        assertThat(post("/api/v1/coupons/batches", """
                {"level":"SEVENTH_BASE","quantity":3}""", STUDENT)).hasStatus(403);
        assertThat(get("/api/v1/coupons/batches", STUDENT)).hasStatus(403);
    }

    @Test
    void theQuantityMustBeReasonable() throws Exception {
        MvcTestResult result = post("/api/v1/coupons/batches", """
                {"level":"SEVENTH_BASE","quantity":0}""", ADMIN);

        assertThat(bodyOf(result, 400).at("/errors/quantity").asText()).isNotBlank();
    }

    @Test
    void anUnknownBatchIsNotFound() {
        assertThat(get("/api/v1/coupons/batches/999999/coupons", ADMIN)).hasStatus(404);
    }

    // ---------- activating a book ----------

    @Test
    void redeemingCreatesAStudentAtTheBooksLevelAndSignsThemIn() throws Exception {
        Coupon coupon = firstCouponOf("SEVENTH_BASE");

        JsonNode body = bodyOf(redeem(coupon.getSerial(), coupon.getCode(), "Amina", "Bensalem"), 201);

        assertThat(body.at("/tokens/accessToken").asText()).isNotBlank();
        assertThat(body.at("/user/role").asText()).isEqualTo("STUDENT");
        assertThat(body.at("/user/level").asText()).isEqualTo("SEVENTH_BASE");
        assertThat(body.at("/user/fullName").asText()).isEqualTo("Amina Bensalem");
        assertThat(body.at("/user/username").asText()).isEqualTo("amina.bensalem");
        assertThat(body.at("/user/email").isNull()).describedAs("no email is asked for").isTrue();
    }

    @Test
    void theStudentCanSignInAgainWithTheirUsername() throws Exception {
        Coupon coupon = firstCouponOf("SEVENTH_BASE");
        bodyOf(redeem(coupon.getSerial(), coupon.getCode(), "Amina", "Bensalem"), 201);

        JsonNode body = bodyOf(post("/api/v1/auth/login", """
                {"email":"Amina.Bensalem","password":"%s"}""".formatted(PASSWORD), null), 200);

        assertThat(body.at("/user/username").asText()).isEqualTo("amina.bensalem");
    }

    @Test
    void theCodeIsAcceptedHoweverTheStudentTypesIt() throws Exception {
        Coupon coupon = firstCouponOf("SEVENTH_BASE");
        String typed = coupon.getCode().substring(0, 4).toLowerCase() + " - " + coupon.getCode().substring(4);

        assertThat(redeem(coupon.getSerial(), typed, "Amina", "Bensalem")).hasStatus(201);
    }

    @Test
    void aCouponWorksOnlyOnce() throws Exception {
        Coupon coupon = firstCouponOf("SEVENTH_BASE");
        bodyOf(redeem(coupon.getSerial(), coupon.getCode(), "Amina", "Bensalem"), 201);

        assertThat(redeem(coupon.getSerial(), coupon.getCode(), "Sami", "Trabelsi")).hasStatus(409);
    }

    @Test
    void aWrongCodeCreatesNothing() {
        Coupon coupon = firstCouponOf("SEVENTH_BASE");
        long before = users.count();

        assertThat(redeem(coupon.getSerial(), "AAAAAAAA", "Amina", "Bensalem")).hasStatus(422);
        assertThat(users.count()).isEqualTo(before);
    }

    @Test
    void theCodeOfAnotherBookDoesNotWork() {
        JsonNode batch;
        try {
            batch = createBatch(ADMIN, "SEVENTH_BASE", 2, null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        var list = coupons.findByBatchIdOrderBySerialAsc(batch.at("/id").asLong());

        assertThat(redeem(list.get(0).getSerial(), list.get(1).getCode(), "Amina", "Bensalem"))
                .describedAs("a shop visitor who saw one unscratched book learns nothing about another")
                .hasStatus(422);
    }

    @Test
    void guessingTheCodeOfOneBookIsThrottled() {
        Coupon coupon = firstCouponOf("SEVENTH_BASE");
        for (int attempt = 0; attempt < 5; attempt++) {
            redeem(coupon.getSerial(), "AAAAAAAA", "Amina", "Bensalem");
        }

        assertThat(redeem(coupon.getSerial(), coupon.getCode(), "Amina", "Bensalem"))
                .describedAs("even the right code waits once the serial is locked").hasStatus(429);
    }

    @Test
    void twoStudentsWithTheSameNameGetDifferentUsernames() throws Exception {
        JsonNode batch = createBatch(ADMIN, "SEVENTH_BASE", 2, null);
        var list = coupons.findByBatchIdOrderBySerialAsc(batch.at("/id").asLong());

        bodyOf(redeem(list.get(0).getSerial(), list.get(0).getCode(), "Amina", "Ben Salem"), 201);
        JsonNode second = bodyOf(redeem(list.get(1).getSerial(), list.get(1).getCode(), "Amina", "Ben Salem"), 201);

        assertThat(second.at("/user/username").asText()).isEqualTo("amina.bensalem2");
    }

    @Test
    void accentsAndArabicScriptStillGiveAUsableUsername() throws Exception {
        JsonNode batch = createBatch(ADMIN, "SEVENTH_BASE", 2, null);
        var list = coupons.findByBatchIdOrderBySerialAsc(batch.at("/id").asLong());

        JsonNode accented = bodyOf(
                redeem(list.get(0).getSerial(), list.get(0).getCode(), "Hélène", "Zaïri"), 201);
        JsonNode arabic = bodyOf(redeem(list.get(1).getSerial(), list.get(1).getCode(), "أمينة", "بن سالم"), 201);

        assertThat(accented.at("/user/username").asText()).isEqualTo("helene.zairi");
        assertThat(arabic.at("/user/username").asText()).isEqualTo("eleve");
        assertThat(arabic.at("/user/fullName").asText()).isEqualTo("أمينة بن سالم");
    }

    @Test
    void missingFieldsAreReportedOneByOne() throws Exception {
        MvcTestResult result = post("/api/v1/coupons/redeem", """
                {"serial":"1-0001","code":"","firstName":"","lastName":"","password":"court"}""", null);

        JsonNode body = bodyOf(result, 400);
        assertThat(body.at("/errors/code").asText()).isNotBlank();
        assertThat(body.at("/errors/firstName").asText()).isNotBlank();
        assertThat(body.at("/errors/lastName").asText()).isNotBlank();
        assertThat(body.at("/errors/password").asText()).isNotBlank();
    }

    // ---------- helpers ----------

    private Coupon firstCouponOf(String level) {
        try {
            JsonNode batch = createBatch(ADMIN, level, 1, null);
            return coupons.findByBatchIdOrderBySerialAsc(batch.at("/id").asLong()).getFirst();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode createBatch(String as, String level, int quantity, String label) throws Exception {
        String labelJson = label == null ? "null" : "\"" + label + "\"";
        return bodyOf(post("/api/v1/coupons/batches", """
                {"level":"%s","quantity":%d,"label":%s}""".formatted(level, quantity, labelJson), as), 201);
    }

    private JsonNode listCoupons(long batchId) throws Exception {
        return bodyOf(get("/api/v1/coupons/batches/" + batchId + "/coupons", ADMIN), 200);
    }

    private MvcTestResult redeem(String serial, String code, String firstName, String lastName) {
        return post("/api/v1/coupons/redeem", """
                {"serial":"%s","code":"%s","firstName":"%s","lastName":"%s","password":"%s"}"""
                .formatted(serial, code, firstName, lastName, PASSWORD), null);
    }

    private String bearer(String email) throws Exception {
        JsonNode body = bodyOf(mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, PASSWORD)).exchange(), 200);
        return "Bearer " + body.at("/tokens/accessToken").asText();
    }

    private MvcTestResult post(String uri, String body, String as) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        if (as != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, bearerUnchecked(as));
        }
        return request.exchange();
    }

    private MvcTestResult get(String uri, String as) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearerUnchecked(as)).exchange();
    }

    private String bearerUnchecked(String email) {
        try {
            return bearer(email);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }
}
