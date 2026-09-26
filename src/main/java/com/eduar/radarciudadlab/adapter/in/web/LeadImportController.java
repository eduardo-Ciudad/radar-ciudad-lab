package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.application.ImportLeadsService;
import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;
import com.eduar.radarciudadlab.domain.model.lead.ImportReport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Importação do CSV de leads. POST só ADMIN (regra no SecurityConfig); GET para qualquer autenticado. */
@RestController
@RequestMapping("/api/leads")
public class LeadImportController {

    private final ImportLeadsService importService;

    public LeadImportController(ImportLeadsService importService) {
        this.importService = importService;
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportReport importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.importFile(file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping("/imports")
    public List<ImportBatch> imports(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return importService.recentImports(limit);
    }
}
