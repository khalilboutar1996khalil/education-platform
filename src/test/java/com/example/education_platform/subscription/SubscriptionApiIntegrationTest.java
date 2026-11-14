package com.example.education_platform.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.access.entity.AccessRequest;
import com.example.education_platform.access.repository.AccessRequestRepository;
import com.example.education_platform.subscription.entity.SchoolYear;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Yearly plans per level: asking with a plan, the admin accepting, and access ending. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubscriptionApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.tn";
    private static final String STUDENT = "amira@eduflow.tn";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccessRequestRepository requests;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SEVENTH_BASE));
    }

    // ---------- plans ----------

    @Test
    void aPlanTheAdminSetsIsOnThePublicOfferWithThePaymentDetails() throws Exception {
        savePlan("SEVENTH_BASE", "100", true);
        assertThat(put("/api/v1/subscriptions/payment-settings", token(ADMIN), """
                {"d17Number":"22 123 456","rib":"10 006 0351234567890 12","accountHolder":"Khalil"}"""))
                .hasStatus(200);

        JsonNode offer = bodyOf(get("/api/v1/subscriptions/offer", null), 200);

        assertThat(offer.at("/plans").size()).isEqualTo(1);
        assertThat(offer.at("/plans/0/level").asText()).isEqualTo("SEVENTH_BASE");
        assertThat(offer.at("/plans/0/price").decimalValue()).isEqualByComparingTo("100");
        assertThat(offer.at("/plans/0/durationDays").asInt()).describedAs("a year by default").isEqualTo(365);
        assertThat(offer.at("/plans/0/levelName").asText()).startsWith("7");
        assertThat(offer.at("/payment/d17Number").asText()).isEqualTo("22 123 456");
    }

    @Test
    void anInactivePlanIsNotOnSale() throws Exception {
        savePlan("SEVENTH_BASE", "100", false);

        assertThat(bodyOf(get("/api/v1/subscriptions/offer", null), 200).at("/plans").size()).isZero();
        assertThat(bodyOf(get("/api/v1/subscriptions/plans", token(ADMIN)), 200).size())
                .describedAs("the admin still sees it, to switch it back on").isEqualTo(1);
    }

    @Test
    void onlyAnAdminManagesPlans() {
        assertThat(put("/api/v1/subscriptions/plans/SEVENTH_BASE", token(STUDENT), """
                {"price":1,"active":true}""")).hasStatus(403);
        assertThat(put("/api/v1/subscriptions/plans/SEVENTH_BASE", null, """
                {"price":1,"active":true}""")).hasStatus(401);
    }

    @Test
    void aNegativePriceIsRefusedInFrench() {
        assertThat(put("/api/v1/subscriptions/plans/SEVENTH_BASE", token(ADMIN), """
                {"price":-5,"active":true}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.price")
                        .isEqualTo("Le prix ne peut pas être négatif"));
    }

    // ---------- asking with a plan ----------

    @Test
    void aLevelWithAPlanRequiresThePayment() {
        savePlan("SEVENTH_BASE", "100", true);

        assertThat(submit("Yacine Cherif", "yacine@eduflow.tn", "SEVENTH_BASE", null, null))
                .hasStatus(422);
        assertThat(requests.count()).isZero();
    }

    @Test
    void theRequestKeepsThePriceItWasMadeAt() throws Exception {
        savePlan("SEVENTH_BASE", "100", true);
        assertThat(submit("Yacine Cherif", "yacine@eduflow.tn", "SEVENTH_BASE", "D17", " TX-5521 "))
                .hasStatus(202);
        savePlan("SEVENTH_BASE", "120", true);

        AccessRequest request = requests.findAll().getFirst();
        assertThat(request.getPrice()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(request.getPaymentReference()).isEqualTo("TX-5521");

        JsonNode queue = bodyOf(get("/api/v1/access-requests?status=PENDING", token(ADMIN)), 200);
        assertThat(queue.at("/content/0/paymentMethod").asText()).isEqualTo("D17");
        assertThat(queue.at("/content/0/price").decimalValue()).isEqualByComparingTo("100");
        assertThat(queue.at("/content/0/renewal").asBoolean()).isFalse();
    }

    @Test
    void acceptingAPaidRequestGivesAccessForTheDurationOfThePlan() throws Exception {
        savePlan("SEVENTH_BASE", "100", true);
        submit("Yacine Cherif", "yacine@eduflow.tn", "SEVENTH_BASE", "VIREMENT", "VIR-8890");

        JsonNode approved = bodyOf(post("/api/v1/access-requests/" + firstRequestId() + "/approve",
                token(ADMIN), null), 200);

        LocalDate expected = SchoolYear.today().plusDays(365);
        assertThat(approved.at("/request/createdUser/accessUntil").asText()).isEqualTo(expected.toString());
        assertThat(users.findByEmail("yacine@eduflow.tn").orElseThrow().getAccessUntil()).isEqualTo(expected);
    }

    @Test
    void aLevelWithoutAPlanStaysAFreeRequestUntilTheEndOfTheSchoolYear() throws Exception {
        assertThat(submit("Yacine Cherif", "yacine@eduflow.tn", "EIGHTH_BASE", null, null)).hasStatus(202);

        bodyOf(post("/api/v1/access-requests/" + firstRequestId() + "/approve", token(ADMIN), null), 200);

        LocalDate end = users.findByEmail("yacine@eduflow.tn").orElseThrow().getAccessUntil();
        assertThat(end.getMonth()).isEqualTo(Month.JUNE);
        assertThat(end.getDayOfMonth()).isEqualTo(30);
        assertThat(end).isAfterOrEqualTo(SchoolYear.today());
    }

    // ---------- the receipt ----------

    @Test
    void aReceiptCanReplaceTheTypedReferenceAndTheAdminDownloadsIt() throws Exception {
        savePlan("SEVENTH_BASE", "100", true);
        byte[] photo = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);

        assertThat(submitWithReceipt("yacine@eduflow.tn", "D17", null,
                new MockMultipartFile("receipt", "recu-d17.jpg", "image/jpeg", photo))).hasStatus(202);

        JsonNode queue = bodyOf(get("/api/v1/access-requests?status=PENDING", token(ADMIN)), 200);
        assertThat(queue.at("/content/0/receiptName").asText()).isEqualTo("recu-d17.jpg");
        assertThat(queue.at("/content/0/paymentReference").isNull()).isTrue();

        MvcTestResult download = get("/api/v1/access-requests/" + firstRequestId() + "/receipt", token(ADMIN));
        assertThat(download).hasStatus(200);
        assertThat(download.getResponse().getContentAsByteArray()).isEqualTo(photo);
        assertThat(get("/api/v1/access-requests/" + firstRequestId() + "/receipt", token(STUDENT)))
                .describedAs("a receipt is for the admin only").hasStatus(403);
    }

    @Test
    void aReceiptThatIsNotAPhotoOrAPdfIsRefused() {
        savePlan("SEVENTH_BASE", "100", true);

        assertThat(submitWithReceipt("yacine@eduflow.tn", "D17", "TX-1",
                new MockMultipartFile("receipt", "virus.zip", "application/zip", new byte[] {1, 2})))
                .hasStatus(422);
        assertThat(requests.count()).isZero();
    }

    @Test
    void withNeitherReceiptNorReferenceThePaidRequestIsRefused() {
        savePlan("SEVENTH_BASE", "100", true);

        assertThat(submitWithReceipt("yacine@eduflow.tn", "D17", null, null)).hasStatus(422);
    }

    @Test
    void aRequestWithoutAReceiptHasNoneToDownload() {
        savePlan("SEVENTH_BASE", "100", true);
        submit("Yacine Cherif", "yacine@eduflow.tn", "SEVENTH_BASE", "D17", "TX-1");

        assertThat(get("/api/v1/access-requests/" + firstRequestId() + "/receipt", token(ADMIN))).hasStatus(404);
    }

    // ---------- the end of access ----------

    @Test
    void aStudentWhoseAccessEndedCannotSignInAndIsToldWhy() {
        expire(STUDENT);

        assertThat(login(STUDENT, PASSWORD))
                .hasStatus(422)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.detail").asString()
                        .contains("renouvelez votre abonnement"));
    }

    @Test
    void anOpenSessionCannotBeRefreshedOnceAccessEnded() throws Exception {
        String refreshToken = bodyOf(login(STUDENT, PASSWORD), 200).at("/tokens/refreshToken").asText();
        expire(STUDENT);

        assertThat(post("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}""".formatted(refreshToken))).hasStatus(401);
    }

    @Test
    void theLastDayItselfStillCounts() {
        users.findByEmail(STUDENT).orElseThrow().setAccessUntil(SchoolYear.today());
        users.flush();

        assertThat(login(STUDENT, PASSWORD)).hasStatus(200);
    }

    // ---------- renewing ----------

    @Test
    void anExpiredStudentRenewsWithTheSameAddressAndKeepsTheirAccount() throws Exception {
        savePlan("SEVENTH_BASE", "100", true);
        expire(STUDENT);
        long accountId = users.findByEmail(STUDENT).orElseThrow().getId();

        assertThat(submit("Amira Benali", STUDENT, "SEVENTH_BASE", "D17", "TX-1")).hasStatus(202);
        assertThat(requests.findAll().getFirst().isRenewal()).isTrue();

        JsonNode approved = bodyOf(post("/api/v1/access-requests/" + firstRequestId() + "/approve",
                token(ADMIN), null), 200);

        assertThat(approved.at("/request/renewal").asBoolean()).isTrue();
        assertThat(approved.at("/request/createdUser/id").asLong()).isEqualTo(accountId);
        assertThat(approved.at("/temporaryPassword").isNull())
                .describedAs("their password does not change").isTrue();
        assertThat(users.findByEmail(STUDENT).orElseThrow().getAccessUntil())
                .isEqualTo(SchoolYear.today().plusDays(365));
        assertThat(login(STUDENT, PASSWORD)).hasStatus(200);
    }

    @Test
    void renewingEarlyAddsTheYearToWhatIsLeft() throws Exception {
        savePlan("EIGHTH_BASE", "110", true);
        LocalDate end = SchoolYear.today().plusDays(20);
        users.findByEmail(STUDENT).orElseThrow().setAccessUntil(end);
        users.flush();

        submit("Amira Benali", STUDENT, "EIGHTH_BASE", "D17", "TX-2");
        bodyOf(post("/api/v1/access-requests/" + firstRequestId() + "/approve", token(ADMIN), null), 200);

        User renewed = users.findByEmail(STUDENT).orElseThrow();
        assertThat(renewed.getAccessUntil()).isEqualTo(end.plusDays(365));
        assertThat(renewed.getLevel()).describedAs("renewing for 8ème moves her up").isEqualTo(Level.EIGHTH_BASE);
    }

    @Test
    void anAccountForALevelWithoutAPlanStillGetsNoRequest() {
        assertThat(submit("Amira Benali", STUDENT, "EIGHTH_BASE", null, null)).hasStatus(202);

        assertThat(requests.count()).isZero();
    }

    // ---------- by hand ----------

    @Test
    void theAdminCanSetTheLastDayOfAStudentByHand() throws Exception {
        long id = users.findByEmail(STUDENT).orElseThrow().getId();

        JsonNode student = bodyOf(put("/api/v1/subscriptions/students/" + id + "/access-until", token(ADMIN), """
                {"accessUntil":"2028-06-30"}"""), 200);

        assertThat(student.at("/accessUntil").asText()).isEqualTo("2028-06-30");
        assertThat(put("/api/v1/subscriptions/students/" + id + "/access-until", token(STUDENT), """
                {"accessUntil":"2030-06-30"}""")).hasStatus(403);
    }

    @Test
    void studentsCannotSignThemselvesUp() {
        assertThat(post("/api/v1/auth/register", null, """
                {"fullName":"Lina","email":"lina@eduflow.tn","password":"%s","level":"SEVENTH_BASE"}"""
                .formatted(PASSWORD)))
                .describedAs("every student is accepted by the admin").hasStatus(422);
        assertThat(users.findByEmail("lina@eduflow.tn")).isEmpty();
    }

    // ---------- helpers ----------

    private void expire(String email) {
        users.findByEmail(email).orElseThrow().setAccessUntil(SchoolYear.today().minusDays(1));
        users.flush();
    }

    private void savePlan(String level, String price, boolean active) {
        assertThat(put("/api/v1/subscriptions/plans/" + level, token(ADMIN), """
                {"price":%s,"active":%s}""".formatted(price, active))).hasStatus(200);
    }

    private long firstRequestId() {
        return requests.findAll().getFirst().getId();
    }

    private MvcTestResult submit(String fullName, String email, String level, String method, String reference) {
        String payment = method == null ? "" : """
                ,"paymentMethod":"%s","paymentReference":"%s\"""".formatted(method, reference);
        return post("/api/v1/access-requests", null, """
                {"fullName":"%s","email":"%s","level":"%s"%s}""".formatted(fullName, email, level, payment));
    }

    private MvcTestResult submitWithReceipt(String email, String method, String reference, MockMultipartFile receipt) {
        String ref = reference == null ? "" : """
                ,"paymentReference":"%s\"""".formatted(reference);
        String json = """
                {"fullName":"Yacine Cherif","email":"%s","level":"SEVENTH_BASE","paymentMethod":"%s"%s}"""
                .formatted(email, method, ref);
        var request = mvc.post().uri("/api/v1/access-requests").multipart()
                .file(new MockMultipartFile("request", "", "application/json", json.getBytes(StandardCharsets.UTF_8)));
        return (receipt == null ? request : request.file(receipt)).exchange();
    }

    private MvcTestResult login(String email, String password) {
        return post("/api/v1/auth/login", null, """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private String token(String email) {
        try {
            return bodyOf(login(email, PASSWORD), 200).at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not log in as " + email, e);
        }
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult get(String uri, String token) {
        var request = mvc.get().uri(uri);
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }

    private MvcTestResult post(String uri, String token, String body) {
        var request = mvc.post().uri(uri);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        var request = mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }
}
