package com.example.education_platform.blog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.blog.entity.BlogPost;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Drafts, slugs, reading time and the view counter. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long categoryId;

    @BeforeEach
    void seed() throws Exception {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));

        categoryId = bodyOf(post("/api/v1/blog/categories", token(ADMIN), """
                {"name":"Conseils","color":"#16A34A"}"""), 201).at("/id").asLong();
    }

    // ---------- slugs ----------

    @Test
    void theSlugComesFromTheTitleWithAccentsFolded() throws Exception {
        JsonNode post = bodyOf(createPost("Réussir sa première année", "Du contenu."), 201);

        assertThat(post.at("/slug").asText()).isEqualTo("reussir-sa-premiere-annee");
    }

    @Test
    void twoArticlesMaySharaATitleButNotAnUrl() throws Exception {
        String first = bodyOf(createPost("Conseils", "A."), 201).at("/slug").asText();
        String second = bodyOf(createPost("Conseils", "B."), 201).at("/slug").asText();

        assertThat(first).isEqualTo("conseils");
        assertThat(second).isEqualTo("conseils-2");
    }

    @Test
    void editingTheTitleLeavesTheUrlWhereItWas() throws Exception {
        JsonNode created = bodyOf(createPost("Titre initial", "Du contenu."), 201);
        long id = created.at("/id").asLong();

        JsonNode updated = bodyOf(put("/api/v1/blog/posts/" + id, token(ADMIN), """
                {"title":"Titre remanié","content":"Du contenu.","categoryId":%d}"""
                .formatted(categoryId)), 200);

        assertThat(updated.at("/title").asText()).isEqualTo("Titre remanié");
        assertThat(updated.at("/slug").asText())
                .describedAs("a published link must keep working").isEqualTo("titre-initial");
    }

    @Test
    void slugifyFoldsAccentsAndCollapsesPunctuation() {
        assertThat(BlogPost.slugify("Réussir : la 1ère année !")).isEqualTo("reussir-la-1ere-annee");
        assertThat(BlogPost.slugify("???")).describedAs("never an empty url").isEqualTo("article");
    }

    // ---------- drafts ----------

    @Test
    void aDraftIsInvisibleToStudentsUntilPublished() throws Exception {
        JsonNode draft = bodyOf(createPost("Brouillon", "Pas encore prêt."), 201);
        long id = draft.at("/id").asLong();
        String slug = draft.at("/slug").asText();

        assertThat(draft.at("/status").asText()).isEqualTo("DRAFT");
        assertThat(countFor(STUDENT)).isZero();
        assertThat(get("/api/v1/blog/posts/by-slug/" + slug, token(STUDENT))).hasStatus(403);

        post("/api/v1/blog/posts/" + id + "/publish", token(ADMIN));
        assertThat(countFor(STUDENT)).isEqualTo(1);
        assertThat(get("/api/v1/blog/posts/by-slug/" + slug, token(STUDENT))).hasStatusOk();
    }

    @Test
    void unpublishingHidesItAgainButKeepsItsSlugAndViews() throws Exception {
        JsonNode published = bodyOf(publishPost("Article", "Du contenu."), 200);
        long id = published.at("/id").asLong();
        String slug = published.at("/slug").asText();
        get("/api/v1/blog/posts/by-slug/" + slug, token(STUDENT));

        JsonNode back = bodyOf(post("/api/v1/blog/posts/" + id + "/unpublish", token(ADMIN)), 200);
        assertThat(back.at("/status").asText()).isEqualTo("DRAFT");
        assertThat(back.at("/slug").asText()).isEqualTo(slug);
        assertThat(back.at("/viewCount").asLong()).isEqualTo(1);
        assertThat(countFor(STUDENT)).isZero();
    }

    @Test
    void publishingTwiceIsRefused() throws Exception {
        long id = bodyOf(publishPost("Article", "Du contenu."), 200).at("/id").asLong();

        assertThat(post("/api/v1/blog/posts/" + id + "/publish", token(ADMIN))).hasStatus(422);
    }

    // ---------- reading ----------

    @Test
    void theListOmitsArticleBodies() throws Exception {
        publishPost("Article", "Un contenu assez long pour être visible.");

        JsonNode card = bodyOf(get("/api/v1/blog/posts", token(STUDENT)), 200).at("/content/0");
        assertThat(card.at("/title").asText()).isEqualTo("Article");
        assertThat(card.at("/content").isNull())
                .describedAs("a list should not ship every article's body").isTrue();
    }

    @Test
    void readingBySlugCountsAViewAndReadingByIdDoesNot() throws Exception {
        JsonNode published = bodyOf(publishPost("Article", "Du contenu."), 200);
        long id = published.at("/id").asLong();
        String slug = published.at("/slug").asText();

        get("/api/v1/blog/posts/by-slug/" + slug, token(STUDENT));
        get("/api/v1/blog/posts/by-slug/" + slug, token(STUDENT));
        get("/api/v1/blog/posts/" + id, token(ADMIN));

        assertThat(bodyOf(get("/api/v1/blog/posts/" + id, token(ADMIN)), 200).at("/viewCount").asLong())
                .describedAs("the editor's own reads are not readership").isEqualTo(2);
    }

    @Test
    void theReadingTimeComesFromTheContentAndIsNeverZero() throws Exception {
        assertThat(bodyOf(createPost("Court", "Trois mots seulement."), 201)
                .at("/readMinutes").asInt())
                .describedAs("'0 min read' reads like a bug").isEqualTo(1);

        String longContent = "mot ".repeat(600);
        assertThat(bodyOf(createPost("Long", longContent), 201).at("/readMinutes").asInt())
                .describedAs("600 words at 200 wpm").isEqualTo(3);
    }

    // ---------- categories ----------

    @Test
    void aCategoryInUseCannotBeDeleted() throws Exception {
        createPost("Article", "Du contenu.");

        assertThat(delete("/api/v1/blog/categories/" + categoryId, token(ADMIN)))
                .describedAs("articles require a category").hasStatus(422);
    }

    @Test
    void anEmptyCategoryCanBeDeleted() throws Exception {
        long spare = bodyOf(post("/api/v1/blog/categories", token(ADMIN), """
                {"name":"Inutilisée"}"""), 201).at("/id").asLong();

        assertThat(delete("/api/v1/blog/categories/" + spare, token(ADMIN))).hasStatus(204);
    }

    @Test
    void aDuplicateCategoryNameConflicts() {
        assertThat(post("/api/v1/blog/categories", token(ADMIN), """
                {"name":"Conseils"}""")).hasStatus(409);
    }

    // ---------- access ----------

    @Test
    void aStudentCanReadButNotWrite() throws Exception {
        long id = bodyOf(publishPost("Article", "Du contenu."), 200).at("/id").asLong();

        assertThat(get("/api/v1/blog/posts", token(STUDENT))).hasStatusOk();
        assertThat(get("/api/v1/blog/categories", token(STUDENT))).hasStatusOk();
        assertThat(createPostAs(STUDENT, "Le mien", "Non.")).hasStatus(403);
        assertThat(post("/api/v1/blog/posts/" + id + "/unpublish", token(STUDENT))).hasStatus(403);
        assertThat(delete("/api/v1/blog/posts/" + id, token(STUDENT))).hasStatus(403);
    }

    @Test
    void anAdminSeesDraftsInTheListAndAStudentDoesNot() throws Exception {
        createPost("Brouillon", "Pas prêt.");
        publishPost("Publié", "Prêt.");

        assertThat(bodyOf(get("/api/v1/blog/posts", token(ADMIN)), 200).at("/totalElements").asLong())
                .isEqualTo(2);
        assertThat(countFor(STUDENT)).isEqualTo(1);
    }

    // ---------- helpers ----------

    private MvcTestResult createPost(String title, String content) {
        return createPostAs(ADMIN, title, content);
    }

    private MvcTestResult createPostAs(String email, String title, String content) {
        return post("/api/v1/blog/posts", token(email), """
                {"title":"%s","content":"%s","categoryId":%d}"""
                .formatted(title, content, categoryId));
    }

    private MvcTestResult publishPost(String title, String content) throws Exception {
        long id = bodyOf(createPost(title, content), 201).at("/id").asLong();
        return post("/api/v1/blog/posts/" + id + "/publish", token(ADMIN));
    }

    private long countFor(String email) throws Exception {
        return bodyOf(get("/api/v1/blog/posts", token(email)), 200).at("/totalElements").asLong();
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

    private MvcTestResult post(String uri, String token) {
        return mvc.post().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult post(String uri, String token, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }
}
