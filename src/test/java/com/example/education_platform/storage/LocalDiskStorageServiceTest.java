package com.example.education_platform.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.storage.service.StorageService;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Storage runs against a real directory rather than a mock, because the behaviour worth testing —
 * where bytes land and what a hostile filename does — only exists on a filesystem.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "app.storage.location=${java.io.tmpdir}/eduflow-storage-test",
        "app.storage.max-file-size=1MB"
})
class LocalDiskStorageServiceTest {

    @Autowired
    private StorageService storage;

    @Autowired
    private StoredFileRepository storedFiles;

    @Autowired
    private UserRepository users;

    @Autowired
    private StorageProperties properties;

    private Path root;

    @BeforeEach
    void signIn() {
        User uploader = users.save(new User("Amira Benali", "amira@eduflow.dz",
                "hash", Role.STUDENT, Level.THIRD_AS));
        users.flush();

        // CurrentUser resolves the user from the JWT subject, so a plain test token will not do
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(uploader.getId().toString())
                .claim("role", "STUDENT")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        root = Path.of(properties.location()).toAbsolutePath().normalize();
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storingWritesTheBytesAndRecordsTheMetadata() {
        StoredFile stored = storage.store(pdf("rapport.pdf", "contenu du rapport"));

        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getOriginalFilename()).isEqualTo("rapport.pdf");
        assertThat(stored.getContentType()).isEqualTo("application/pdf");
        assertThat(stored.getSizeBytes()).isEqualTo("contenu du rapport".getBytes(StandardCharsets.UTF_8).length);
        assertThat(stored.getChecksum()).hasSize(64);
        assertThat(stored.getUploadedBy().getEmail()).isEqualTo("amira@eduflow.dz");
    }

    @Test
    void theFileLandsInsideTheRootUnderAGeneratedName() {
        StoredFile stored = storage.store(pdf("rapport.pdf", "abc"));

        Path written = findWritten(stored);
        assertThat(written).exists();
        assertThat(written.normalize().startsWith(root)).isTrue();
        assertThat(written.getFileName().toString())
                .describedAs("the path is built from the generated key, not the client's name")
                .isEqualTo(stored.getStorageKey())
                .doesNotContain("rapport");
    }

    @Test
    void aTraversingFilenameCannotEscapeTheRoot() {
        StoredFile stored = storage.store(pdf("../../../../etc/passwd.pdf", "abc"));

        assertThat(stored.getOriginalFilename())
                .describedAs("only the leaf name survives").isEqualTo("passwd.pdf");
        assertThat(findWritten(stored).normalize().startsWith(root)).isTrue();
    }

    @Test
    void aWindowsStyleTraversingFilenameIsAlsoReducedToItsLeaf() {
        StoredFile stored = storage.store(pdf("..\\..\\windows\\system32\\evil.pdf", "abc"));

        assertThat(stored.getOriginalFilename()).isEqualTo("evil.pdf");
    }

    @Test
    void aFilenameThatIsNothingButDotsFallsBackToAPlaceholder() {
        StoredFile stored = storage.store(pdf("..", "abc"));

        assertThat(stored.getOriginalFilename()).isEqualTo("fichier");
    }

    @Test
    void identicalBytesProduceTheSameChecksumButDifferentKeys() {
        StoredFile first = storage.store(pdf("a.pdf", "même contenu"));
        StoredFile second = storage.store(pdf("b.pdf", "même contenu"));

        assertThat(first.getChecksum()).isEqualTo(second.getChecksum());
        assertThat(first.getStorageKey())
                .describedAs("two uploads never share a path").isNotEqualTo(second.getStorageKey());
    }

    @Test
    void theContentTypeAllowListIsEnforced() {
        MockMultipartFile script = new MockMultipartFile(
                "file", "payload.sh", "application/x-sh", "rm -rf /".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(script))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not accepted");
    }

    @Test
    void aFileWithNoContentTypeIsRefused() {
        MockMultipartFile unknown = new MockMultipartFile(
                "file", "mystery.bin", null, "abc".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(unknown)).isInstanceOf(BusinessException.class);
    }

    @Test
    void anEmptyFileIsRefused() {
        MockMultipartFile empty = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.store(empty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void aFileOverTheLimitIsRefusedAndNothingIsLeftBehind() throws Exception {
        long before = countFilesUnderRoot();
        MockMultipartFile tooBig = new MockMultipartFile(
                "file", "gros.pdf", "application/pdf", new byte[2 * 1024 * 1024]);

        assertThatThrownBy(() -> storage.store(tooBig))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maximum size");
        assertThat(countFilesUnderRoot())
                .describedAs("a rejected upload must not leave bytes on disk").isEqualTo(before);
    }

    @Test
    void aStoredFileCanBeReadBackAndThenDeleted() throws Exception {
        StoredFile stored = storage.store(pdf("rapport.pdf", "contenu"));

        assertThat(storage.loadAsResource(stored).getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("contenu");

        storage.delete(stored);
        assertThat(findWritten(stored)).doesNotExist();
    }

    @Test
    void readingAFileWhoseBytesVanishedFailsLoudly() throws Exception {
        StoredFile stored = storage.store(pdf("rapport.pdf", "contenu"));
        Files.delete(findWritten(stored));

        assertThatThrownBy(() -> storage.loadAsResource(stored))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("missing from disk");
    }

    // ---------- helpers ----------

    private static MockMultipartFile pdf(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/pdf",
                content.getBytes(StandardCharsets.UTF_8));
    }

    /** Mirrors the service's sharding, which is the only thing that knows where a key lives. */
    private Path findWritten(StoredFile stored) {
        String key = stored.getStorageKey();
        return root.resolve(key.substring(0, 2)).resolve(key.substring(2, 4)).resolve(key);
    }

    private long countFilesUnderRoot() throws Exception {
        if (!Files.exists(root)) {
            return 0;
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }
}
