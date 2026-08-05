package com.medprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.medprep.exception.MedPrepException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Cliente para Supabase Storage (REST). Usa la service key (solo backend).
 * El bucket debe existir previamente en Supabase Storage.
 */
@Slf4j
@Service
public class StorageService {

    private final WebClient webClient;
    private final String bucket;
    private final String storageBaseUrl;

    public StorageService(@Value("${supabase.url}") String supabaseUrl,
                          @Value("${supabase.service-key}") String serviceKey,
                          @Value("${supabase.storage.bucket-documents}") String bucket) {
        this.bucket = bucket;
        this.storageBaseUrl = supabaseUrl + "/storage/v1";
        this.webClient = WebClient.builder()
                .baseUrl(this.storageBaseUrl)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .defaultHeader("apikey", serviceKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(60 * 1024 * 1024))
                .build();
    }

    /** Sube los bytes a {bucket}/{path}. Devuelve el path almacenado. */
    public String upload(String path, byte[] content, String contentType) {
        try {
            webClient.post()
                    .uri("/object/{bucket}/{path}", bucket, path)
                    .header("x-upsert", "true")
                    .contentType(contentType != null
                            ? MediaType.parseMediaType(contentType)
                            : MediaType.APPLICATION_OCTET_STREAM)
                    .bodyValue(content)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return path;
        } catch (Exception e) {
            log.error("Error subiendo archivo a Supabase Storage: {}", e.getMessage());
            throw new MedPrepException("No se pudo subir el archivo: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "STORAGE_UPLOAD_FAILED");
        }
    }

    /** Genera una URL firmada temporal para descargar un objeto privado. */
    public String createSignedUrl(String path, int expiresInSeconds) {
        try {
            JsonNode node = webClient.post()
                    .uri("/object/sign/{bucket}/{path}", bucket, path)
                    .bodyValue(Map.of("expiresIn", expiresInSeconds))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            String signed = node != null ? node.path("signedURL").asText(null) : null;
            if (signed == null) {
                throw new IllegalStateException("Respuesta sin signedURL");
            }
            return storageBaseUrl + signed;
        } catch (Exception e) {
            log.error("Error generando URL firmada: {}", e.getMessage());
            throw new MedPrepException("No se pudo generar el enlace de descarga: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "STORAGE_SIGN_FAILED");
        }
    }

    public void delete(String path) {
        try {
            webClient.delete()
                    .uri("/object/{bucket}/{path}", bucket, path)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("No se pudo eliminar el archivo {} de Storage: {}", path, e.getMessage());
        }
    }
}
