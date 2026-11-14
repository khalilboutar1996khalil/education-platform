package com.example.education_platform.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.storage.service.impl.S3StorageService;
import com.example.education_platform.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** The object-store backend, with the S3 client mocked: what is sent, under which key, and what is refused. */
class S3StorageServiceTest {

    private final S3Client s3 = mock(S3Client.class);
    private final StoredFileRepository storedFiles = mock(StoredFileRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private S3StorageService storage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties("unused", DataSize.ofKilobytes(1),
                List.of("application/pdf"), "s3",
                new StorageProperties.S3("https://example.r2.cloudflarestorage.com", "auto", "eduflow",
                        "key", "secret"));
        when(currentUser.find()).thenReturn(Optional.of(new User()));
        when(storedFiles.save(any(StoredFile.class))).thenAnswer(inv -> inv.getArgument(0));
        storage = new S3StorageService(properties, storedFiles, currentUser, s3);
    }

    @Test
    void storingUploadsUnderAGeneratedKeyAndRecordsTheMetadata() {
        StoredFile stored = storage.store(pdf("../../rapport.pdf", "contenu"));

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().bucket()).isEqualTo("eduflow");
        assertThat(put.getValue().key()).isEqualTo(stored.getStorageKey()).endsWith(".pdf").doesNotContain("rapport");
        assertThat(put.getValue().contentLength()).isEqualTo(7L);
        assertThat(stored.getOriginalFilename()).isEqualTo("rapport.pdf");
        assertThat(stored.getSizeBytes()).isEqualTo(7L);
        assertThat(stored.getChecksum()).hasSize(64);
    }

    @Test
    void aTypeOutsideTheAllowListIsRefusedBeforeAnythingIsSent() {
        MockMultipartFile exe = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", new byte[] {1});

        assertThatThrownBy(() -> storage.store(exe)).isInstanceOf(BusinessException.class);
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void aFileOverTheLimitIsRefusedBeforeAnythingIsSent() {
        assertThatThrownBy(() -> storage.store(pdf("gros.pdf", "x".repeat(2048))))
                .isInstanceOf(BusinessException.class);
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void loadingGivesAResourceWithTheRecordedSize() throws Exception {
        StoredFile file = new StoredFile("a.pdf", "application/pdf", 42, "k.pdf", "c", new User());

        Resource resource = storage.loadAsResource(file);

        assertThat(resource.contentLength()).isEqualTo(42L);
        assertThat(resource.getFilename()).isEqualTo("k.pdf");
    }

    @Test
    void deletingRemovesTheObject() {
        storage.delete(new StoredFile("a.pdf", "application/pdf", 1, "k.pdf", "c", new User()));

        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(delete.capture());
        assertThat(delete.getValue().key()).isEqualTo("k.pdf");
    }

    private static MockMultipartFile pdf(String name, String content) {
        return new MockMultipartFile("file", name, "application/pdf", content.getBytes(StandardCharsets.UTF_8));
    }
}
