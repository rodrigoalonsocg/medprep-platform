package com.medprep.service;

import com.medprep.dto.request.CreateAcademyRequest;
import com.medprep.dto.response.AcademyResponse;
import com.medprep.dto.response.DocumentResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.Academy;
import com.medprep.model.Document;
import com.medprep.repository.AcademyRepository;
import com.medprep.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademyService {

    private final AcademyRepository academyRepository;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;

    @Transactional
    public AcademyResponse create(CreateAcademyRequest req, UUID createdBy) {
        Academy academy = Academy.builder()
                .name(req.getName())
                .description(req.getDescription())
                .createdBy(createdBy)
                .build();
        return toAcademyResponse(academyRepository.save(academy));
    }

    @Transactional(readOnly = true)
    public List<AcademyResponse> listAcademies() {
        return academyRepository.findAllByOrderByNameAsc()
                .stream().map(this::toAcademyResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(UUID academyId) {
        return documentRepository.findByAcademyIdOrderByCreatedAtDesc(academyId)
                .stream().map(this::toDocumentResponse).toList();
    }

    @Transactional
    public DocumentResponse uploadDocument(UUID academyId, MultipartFile file,
                                           boolean isPublic, UUID uploadedBy) {
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> MedPrepException.notFound("Academia", academyId.toString()));

        if (file == null || file.isEmpty()) {
            throw MedPrepException.badRequest("El archivo está vacío");
        }

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento";
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String path = academyId + "/" + UUID.randomUUID() + "_" + safeName;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw MedPrepException.badRequest("No se pudo leer el archivo");
        }

        storageService.upload(path, bytes, file.getContentType());

        Document document = Document.builder()
                .academy(academy)
                .uploadedBy(uploadedBy)
                .fileName(original)
                .storagePath(path)
                .fileSizeBytes(file.getSize())
                .isPublic(isPublic)
                .build();

        return toDocumentResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> MedPrepException.notFound("Documento", documentId.toString()));
        return storageService.createSignedUrl(document.getStoragePath(), 3600);
    }

    private AcademyResponse toAcademyResponse(Academy a) {
        return AcademyResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private DocumentResponse toDocumentResponse(Document d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .academyId(d.getAcademy().getId())
                .fileName(d.getFileName())
                .fileSizeBytes(d.getFileSizeBytes())
                .isPublic(d.isPublic())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
