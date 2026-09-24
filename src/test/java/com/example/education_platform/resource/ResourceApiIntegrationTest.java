package com.example.education_platform.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** The library: who sees what, uploads versus links, and the download counter. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.storage.location=${java.io.tmpdir}/eduflow-resource-test")
class ResourceApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String THIRD_AS = "amira@eduflow.dz";
    private static final String SECOND_AS = "sofiane@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long thirdAsCourseId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", THIRD_AS,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Sofiane Meziane", SECOND_AS,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        thirdAsCourseId = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A")).getId();
        courses.saveAndFlush(new Course("INF101", "Initiation", null, Level.SECOND_AS, "#2563EB"));
    }

    // ---------- scope ----------

    @Test
    void aResourceWithNoScopeReachesEveryone() throws Exception {
        upload(ADMIN, """
                {"title":"Règlement intérieur","type":"PDF"}""", "reglement.pdf");

        assertThat(count(THIRD_AS)).isEqualTo(1);
        assertThat(count(SECOND_AS))
                .describedAs("no course and no level means the whole section").isEqualTo(1);
    }

    @Test
    void aModuleScopedResourceOnlyReachesThatModulesLevel() throws Exception {
        upload(ADMIN, """
                {"title":"Cours d'algo","type":"PDF","courseId":%d}""".formatted(thirdAsCourseId),
                "algo.pdf");

        assertThat(count(THIRD_AS)).isEqualTo(1);
        assertThat(count(SECOND_AS)).isZero();
    }

    @Test
    void aLevelScopedResourceOnlyReachesThatLevel() throws Exception {
        upload(ADMIN, """
                {"title":"Guide 2AS","type":"PDF","level":"SECOND_AS"}""", "guide.pdf");

        assertThat(count(SECOND_AS)).isEqualTo(1);
        assertThat(count(THIRD_AS)).isZero();
    }

    @Test
    void anAdminSeesEverythingWhateverTheScope() throws Exception {
        upload(ADMIN, """
                {"title":"2AS","type":"PDF","level":"SECOND_AS"}""", "a.pdf");
        upload(ADMIN, """
                {"title":"3AS","type":"PDF","level":"THIRD_AS"}""", "b.pdf");

        assertThat(count(ADMIN)).isEqualTo(2);
    }

    @Test
    void readingAResourceOutsideMyScopeIsRefused() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Guide 2AS","type":"PDF","level":"SECOND_AS"}""", "guide.pdf"), 201)
                .at("/id").asLong();

        assertThat(get("/api/v1/resources/" + id, token(THIRD_AS))).hasStatus(403);
        assertThat(get("/api/v1/resources/" + id, token(SECOND_AS))).hasStatusOk();
    }

    // ---------- uploads versus links ----------

    @Test
    void anUploadedResourceCarriesItsFileMetadata() throws Exception {
        JsonNode created = bodyOf(upload(ADMIN, """
                {"title":"Cours d'algo","description":"Chapitre 1","type":"PDF"}""", "algo.pdf"), 201);

        assertThat(created.at("/file/originalFilename").asText()).isEqualTo("algo.pdf");
        assertThat(created.at("/file/contentType").asText()).isEqualTo("application/pdf");
        assertThat(created.at("/externalUrl").isNull()).isTrue();
        assertThat(created.at("/downloadCount").asLong()).isZero();
    }

    @Test
    void aLinkResourceCarriesAnUrlAndNoFile() throws Exception {
        JsonNode created = bodyOf(upload(ADMIN, """
                {"title":"Cours en ligne","type":"LINK","externalUrl":"https://example.dz/algo"}""",
                null), 201);

        assertThat(created.at("/externalUrl").asText()).isEqualTo("https://example.dz/algo");
        assertThat(created.at("/file").isNull()).isTrue();
    }

    @Test
    void aFileBackedTypeWithoutAFileIsRefused() {
        assertThat(upload(ADMIN, """
                {"title":"Sans fichier","type":"PDF"}""", null)).hasStatus(422);
    }

    @Test
    void aLinkWithAFileIsRefused() {
        assertThat(upload(ADMIN, """
                {"title":"Les deux","type":"LINK","externalUrl":"https://example.dz"}""", "trop.pdf"))
                .hasStatus(422);
    }

    @Test
    void aLinkWithoutAnUrlIsRefused() {
        assertThat(upload(ADMIN, """
                {"title":"Sans url","type":"LINK"}""", null)).hasStatus(422);
    }

    @Test
    void anUrlThatIsNotHttpIsRefused() {
        assertThat(upload(ADMIN, """
                {"title":"Douteux","type":"LINK","externalUrl":"javascript:alert(1)"}""", null))
                .describedAs("the platform hands this link to students").hasStatus(422);
    }

    // ---------- downloading ----------

    @Test
    void downloadingServesTheFileAndCountsIt() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Cours d'algo","type":"PDF"}""", "algo.pdf"), 201).at("/id").asLong();

        assertThat(get("/api/v1/resources/" + id + "/download", token(THIRD_AS))).hasStatusOk();
        assertThat(get("/api/v1/resources/" + id + "/download", token(THIRD_AS))).hasStatusOk();

        assertThat(bodyOf(get("/api/v1/resources/" + id, token(ADMIN)), 200)
                .at("/downloadCount").asLong()).isEqualTo(2);
    }

    @Test
    void aLinkHasNothingToDownload() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Cours en ligne","type":"LINK","externalUrl":"https://example.dz"}""", null),
                201).at("/id").asLong();

        assertThat(get("/api/v1/resources/" + id + "/download", token(THIRD_AS))).hasStatus(422);
    }

    @Test
    void downloadingOutsideMyScopeIsRefused() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Guide 2AS","type":"PDF","level":"SECOND_AS"}""", "guide.pdf"), 201)
                .at("/id").asLong();

        assertThat(get("/api/v1/resources/" + id + "/download", token(THIRD_AS))).hasStatus(403);
    }

    // ---------- editing ----------

    @Test
    void anAdminEditsTheDetailsButNotTheType() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Ancien titre","type":"PDF"}""", "algo.pdf"), 201).at("/id").asLong();

        assertThat(bodyOf(put("/api/v1/resources/" + id, token(ADMIN), """
                {"title":"Nouveau titre","type":"PDF","level":"THIRD_AS"}"""), 200)
                .at("/title").asText()).isEqualTo("Nouveau titre");

        assertThat(put("/api/v1/resources/" + id, token(ADMIN), """
                {"title":"Nouveau titre","type":"LINK","externalUrl":"https://example.dz"}"""))
                .describedAs("changing the type would orphan the file").hasStatus(422);
    }

    @Test
    void aStudentCannotAddEditOrDeleteResources() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Cours d'algo","type":"PDF"}""", "algo.pdf"), 201).at("/id").asLong();

        assertThat(upload(THIRD_AS, """
                {"title":"Mon cours","type":"PDF"}""", "mien.pdf")).hasStatus(403);
        assertThat(put("/api/v1/resources/" + id, token(THIRD_AS), """
                {"title":"Piraté","type":"PDF"}""")).hasStatus(403);
        assertThat(delete("/api/v1/resources/" + id, token(THIRD_AS))).hasStatus(403);
    }

    @Test
    void deletingRemovesItFromTheLibrary() throws Exception {
        long id = bodyOf(upload(ADMIN, """
                {"title":"Temporaire","type":"PDF"}""", "temp.pdf"), 201).at("/id").asLong();

        assertThat(delete("/api/v1/resources/" + id, token(ADMIN))).hasStatus(204);
        assertThat(get("/api/v1/resources/" + id, token(ADMIN))).hasStatus(404);
    }

    // ---------- filtering ----------

    @Test
    void filtersNarrowTheLibrary() throws Exception {
        upload(ADMIN, """
                {"title":"Cours d'algorithmique","type":"PDF","courseId":%d}"""
                .formatted(thirdAsCourseId), "algo.pdf");
        upload(ADMIN, """
                {"title":"Vidéo de révision","type":"LINK","externalUrl":"https://example.dz/v"}""",
                null);

        assertThat(bodyOf(get("/api/v1/resources?type=LINK", token(ADMIN)), 200)
                .at("/totalElements").asLong()).isEqualTo(1);
        assertThat(bodyOf(get("/api/v1/resources?courseId=" + thirdAsCourseId, token(ADMIN)), 200)
                .at("/totalElements").asLong()).isEqualTo(1);
        assertThat(bodyOf(get("/api/v1/resources?search=ALGORITHMIQUE", token(ADMIN)), 200)
                .at("/totalElements").asLong())
                .describedAs("search is case-insensitive").isEqualTo(1);
    }

    // ---------- helpers ----------

    private long count(String email) throws Exception {
        return bodyOf(get("/api/v1/resources", token(email)), 200).at("/totalElements").asLong();
    }

    private MvcTestResult upload(String email, String resourceJson, String filename) {
        MockMultipartFile metadata = new MockMultipartFile("resource", "", MediaType.APPLICATION_JSON_VALUE,
                resourceJson.getBytes(StandardCharsets.UTF_8));
        var request = mvc.post().uri("/api/v1/resources").multipart().file(metadata);
        if (filename != null) {
            request = request.file(new MockMultipartFile("file", filename, "application/pdf",
                    "contenu".getBytes(StandardCharsets.UTF_8)));
        }
        return request.header("Authorization", "Bearer " + token(email)).exchange();
    }

    private String token(String email) {
        try {
            MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"%s","password":"%s"}""".formatted(email, PASSWORD))
                    .exchange();
            return bodyOf(result, 200).at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not log in as " + email, e);
        }
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }
}
