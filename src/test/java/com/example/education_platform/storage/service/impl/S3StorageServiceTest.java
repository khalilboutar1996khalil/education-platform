package com.example.education_platform.storage.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.storage.StorageProperties;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.storage.repository.StoredFileRepository;
import com.example.education_platform.user.entity.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3StorageServiceTest {

    private static final byte[] CONTENT = "%PDF-1.7 cours".getBytes(StandardCharsets.UTF_8);

    private final S3Client s3 = mock(S3Client.class);
    private final StoredFileRepository storedFiles = mock(StoredFileRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private S3StorageService storage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties("unused", DataSize.ofKilobytes(1),
                List.of("application/pdf"),
                new StorageProperties.S3("https://r2.example", "auto", "eduflow", "key", "secret"));
        storage = new S3StorageService(properties, storedFiles, currentUser, s3);
        when(currentUser.get()).thenReturn(mock(User.class));
        when(storedFiles.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void storeUploadsUnderAGeneratedKeyAndRecordsTheChecksum() throws Exception {
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> request = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        // Reading the body is what drives the digest, as the real client would
        ByteArrayOutputStream uploaded = new ByteArrayOutputStream();
        when(s3.putObject(request.capture(), body.capture())).thenAnswer(call -> {
            try (InputStream in = ((RequestBody) call.getArgument(1)).contentStreamProvider().newStream()) {
                in.transferTo(uploaded);
            }
            return null;
        });

        StoredFile stored = storage.store(
                new MockMultipartFile("file", "../../cours.pdf", "application/pdf", CONTENT));

        PutObjectRequest.Builder put = PutObjectRequest.builder();
        request.getValue().accept(put);
        PutObjectRequest sent = put.build();
        assertThat(sent.bucket()).isEqualTo("eduflow");
        assertThat(sent.key()).matches("[0-9a-f-]{36}\\.pdf").isEqualTo(stored.getStorageKey());
        assertThat(uploaded.toByteArray()).isEqualTo(CONTENT);
        assertThat(stored.getOriginalFilename()).isEqualTo("cours.pdf");
        assertThat(stored.getSizeBytes()).isEqualTo(CONTENT.length);
        assertThat(stored.getChecksum()).isEqualTo(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(CONTENT)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void refusedTypesNeverReachTheBucket() {
        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("file", "x.exe", "application/x-msdownload", CONTENT)))
                .isInstanceOf(BusinessException.class);
        verify(s3, never()).putObject(any(Consumer.class), any(RequestBody.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadStreamsTheObjectOnlyWhenRead() throws IOException {
        StoredFile file = new StoredFile("cours.pdf", "application/pdf", CONTENT.length, "abc.pdf", "x", null);
        when(s3.getObject(any(Consumer.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().build(), AbortableInputStream.create(new java.io.ByteArrayInputStream(CONTENT))));

        Resource resource = storage.loadAsResource(file);
        verify(s3, never()).getObject(any(Consumer.class));

        assertThat(resource.contentLength()).isEqualTo(CONTENT.length);
        assertThat(resource.getFilename()).isEqualTo("cours.pdf");
        try (InputStream in = resource.getInputStream()) {
            assertThat(in.readAllBytes()).isEqualTo(CONTENT);
        }
        ArgumentCaptor<Consumer<GetObjectRequest.Builder>> get = ArgumentCaptor.forClass(Consumer.class);
        verify(s3).getObject(get.capture());
        GetObjectRequest.Builder builder = GetObjectRequest.builder();
        get.getValue().accept(builder);
        assertThat(builder.build().key()).isEqualTo("abc.pdf");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteRemovesTheObject() {
        storage.delete(new StoredFile("cours.pdf", "application/pdf", 1, "abc.pdf", "x", null));

        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> del = ArgumentCaptor.forClass(Consumer.class);
        verify(s3).deleteObject(del.capture());
        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        del.getValue().accept(builder);
        assertThat(builder.build().bucket()).isEqualTo("eduflow");
        assertThat(builder.build().key()).isEqualTo("abc.pdf");
    }
}
