package com.rodiond26.selsup;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UUID;
import org.hibernate.validator.constraints.ru.INN;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private ExecutorService customExecutorService;
    private DocumentApiService documentApiService;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Некорректное значение requestLimit: " + requestLimit);
        }
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.customExecutorService = createCustomExecutorService(timeUnit, requestLimit);
        this.documentApiService = new IntegrationService();
    }

    // example usage
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        crptApi.createDocument(new DocumentDto(), "sign");
    }

    public String createDocument(DocumentDto documentDto, String documentSign) {
        // log...
        CreateDocumentRequestDto requestDto = CreateDocumentRequestDto.builder()
                .document(documentDto)
                .sign(documentSign)
                .build();

        CompletableFuture<String> completableFuture = CompletableFuture
                .supplyAsync(
                        () -> documentApiService.createDocumentV3(requestDto, String.class), customExecutorService)
                .handle((str, throwable) -> {
                    // log...
                    throw new RuntimeException("Что-то не то со строкой: " + str);
                });

        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            // log...
            throw new RuntimeException(e);
        }
    }

    private ExecutorService createCustomExecutorService(TimeUnit timeUnit, int requestLimit) {
        return new ThreadPoolExecutor(
                requestLimit * 1,
                requestLimit * 20,
                10,
                timeUnit,
                new ArrayBlockingQueue<>(requestLimit * 10),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @JsonPropertyOrder({
            "document",
            "sign"
    })
    @Schema(description = "Описание запроса")
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class CreateDocumentRequestDto {
        @JsonProperty("document")
        @Schema(description = "Описание document")
        @NotNull
        private DocumentDto document;
        @JsonProperty("sign")
        @Schema(description = "Описание sign")
        @NotNull
        private String sign;
    }

    @JsonPropertyOrder({
            "description",
            "doc_id",
            "doc_status",
            "doc_type",
            "importRequest",
            "owner_inn",
            "participant_inn",
            "producer_inn",
            "production_date",
            "production_type",
            "products",
            "reg_date",
            "reg_number"
    })
    @Schema(description = "Описание документа")
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class DocumentDto {

        @JsonProperty("description")
        @Schema(description = "Описание description")
        @NotNull
        private DescriptionDto description;

        @JsonProperty("doc_id")
        @Schema(description = "Описание docId")
        @UUID
        private String docId;

        @JsonProperty("doc_status")
        @Schema(description = "Описание docStatus", example = "NEW", implementation = DocumentStatus.class)
        @NotNull
        private DocumentStatus docStatus;

        @JsonProperty("doc_type")
        @Schema(description = "Описание docType", example = "LP_INTRODUCE_GOODS", implementation = DocumentType.class)
        @NotNull
        private DocumentType docType;

        @JsonProperty("importRequest")
        @Schema(description = "Описание importRequest")
        private Boolean importRequest;

        @JsonProperty("owner_inn")
        @Schema(description = "Описание ownerInn")
        @INN
        @NotNull
        private String ownerInn;

        @JsonProperty("participant_inn")
        @Schema(description = "Описание participant_inn")
        @INN
        @NotNull
        private String participantInn;

        @JsonProperty("producer_inn")
        @Schema(description = "Описание producerInn")
        @INN
        @NotNull
        private String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Описание productionDate", example = "2020-01-23")
        private Date productionDate;

        @JsonProperty("production_type")
        @Schema(description = "Описание productionType")
        private String productionType;

        @JsonProperty("products")
        @ArraySchema(schema = @Schema(description = "Описание products", implementation = ProductDto.class))
        private List<ProductDto> products;

        @JsonProperty("reg_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Описание regDate", example = "2020-01-23")
        @NotNull
        private String regDate;

        @JsonProperty("reg_number")
        @Schema(description = "Описание regNumber")
        @NotNull
        private String regNumber;
    }

    @JsonPropertyOrder({
            "participantInn"
    })
    @Schema(description = "Описание описания")
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class DescriptionDto {

        @JsonProperty("participantInn")
        @Schema(description = "Описание participantInn")
        @INN
        @NotNull
        private String participantInn;
    }

    @JsonPropertyOrder({
            "certificate_document",
            "certificate_document_date",
            "certificate_document_number",
            "owner_inn",
            "producer_inn",
            "production_date",
            "tnved_code",
            "uit_code",
            "uitu_code"
    })
    @Schema(description = "Описание продукта")
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class ProductDto {
        @JsonProperty("certificate_document")
        @Schema(description = "Описание certificateDocument")
        private String certificateDocument;

        @JsonProperty("certificate_document_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Описание certificateDocumentDate", example = "2020-01-23")
        private Date certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        @Schema(description = "Описание certificateDocumentNumber")
        private String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        @Schema(description = "Описание ownerInn")
        @INN
        @NotNull
        private String ownerInn;

        @JsonProperty("producer_inn")
        @Schema(description = "Описание producerInn")
        @INN
        @NotNull
        private String producerInn;

        @JsonProperty("production_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Описание productionDate", example = "2020-01-23")
        @NotNull
        private Date productionDate;

        @JsonProperty("tnved_code")
        @Schema(description = "Описание tnved_code")
        @Size(min = 4)
        private String tnvedCode;

        @JsonProperty("uit_code")
        @Schema(description = "Описание uit_code")
        @Digits(integer = 10, fraction = 0)
        private String uitCode;

        @JsonProperty("uitu_code")
        @Schema(description = "Описание uitu_code")
        @Digits(integer = 10, fraction = 0)
        private String uituCode;
    }

    @Schema(enumAsRef = true)
    public enum DocumentType {
        LP_INTRODUCE_GOODS
    }

    @Schema(enumAsRef = true)
    public enum DocumentStatus {
        NEW,
        IN_PROGRESS,
        CLOSED
    }
}

interface DocumentApiService {
    <T, R> T createDocumentV3(R requestBody, Class<T> clazz);
}

class IntegrationService implements DocumentApiService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String host = "https://ismp.crpt.ru";
    private String port = "443";
    private String CREATE_DOCUMENT_PATH_V3 = "/api/v3/lk/documents/create";

    public IntegrationService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <T, R> T createDocumentV3(R requestBody, Class<T> clazz) {
        // log...

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .uri(URI.create(host + ":" + port + CREATE_DOCUMENT_PATH_V3))
                    .setHeader("User-Agent", "CrptApi")
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                // log...
                return objectMapper.readValue(response.body(), clazz);
            } else {
                // log...
                throw new IOException("Что-то не то");
            }
        } catch (IOException | InterruptedException e) {
            // log...
            throw new RuntimeException(e);
        }
    }
}
