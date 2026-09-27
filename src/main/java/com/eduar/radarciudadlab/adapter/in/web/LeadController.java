package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.application.LeadCommandService;
import com.eduar.radarciudadlab.application.LeadQueryService;
import com.eduar.radarciudadlab.domain.model.lead.LeadBoard;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilter;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilterOptions;
import com.eduar.radarciudadlab.domain.model.lead.LeadPage;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Funil de leads. Todo /api/leads/** é só ADMIN (regra no SecurityConfig): prospecção é uso interno,
 * e o VIEWER existe para clientes, que não podem ver a lista de prospects.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadQueryService queries;
    private final LeadCommandService commands;

    public LeadController(LeadQueryService queries, LeadCommandService commands) {
        this.queries = queries;
        this.commands = commands;
    }

    /** Kanban: colunas NOVO..FECHADO com contagem e os primeiros de cada uma por score. */
    @GetMapping("/board")
    public LeadBoard board(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "neighborhood", required = false) String neighborhood,
            @RequestParam(name = "withoutWebsite", required = false) Boolean withoutWebsite,
            @RequestParam(name = "mobile", required = false) Boolean mobile,
            @RequestParam(name = "minScore", required = false) Integer minScore,
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "perColumn", defaultValue = "" + LeadQueryService.DEFAULT_PER_COLUMN) int perColumn) {

        LeadFilter filter = new LeadFilter(null, category, neighborhood, withoutWebsite, mobile, minScore, search);
        return queries.board(filter, perColumn);
    }

    /** Lista paginada (ver mais de uma coluna, perdidos/descartados, tabela). */
    @GetMapping
    public LeadPage list(
            @RequestParam(name = "status", required = false) LeadStatus status,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "neighborhood", required = false) String neighborhood,
            @RequestParam(name = "withoutWebsite", required = false) Boolean withoutWebsite,
            @RequestParam(name = "mobile", required = false) Boolean mobile,
            @RequestParam(name = "minScore", required = false) Integer minScore,
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "sort", defaultValue = "score") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        LeadFilter filter = new LeadFilter(status, category, neighborhood, withoutWebsite, mobile, minScore, search);
        return queries.page(filter, sort, page, size);
    }

    /** Opções dos selects de Categoria e Bairro. */
    @GetMapping("/filters")
    public LeadFilterOptions filters() {
        return queries.filterOptions();
    }

    @GetMapping("/{id}")
    public LeadDetailResponse detail(@PathVariable("id") Long id) {
        return LeadDetailResponse.from(queries.detail(id));
    }

    @PatchMapping("/{id}")
    public LeadDetailResponse update(@PathVariable("id") Long id, @Valid @RequestBody LeadUpdateRequest request) {
        return LeadDetailResponse.from(commands.update(id, request.toDomain()));
    }

    /** Recalcula o score de todos (depois de mudar o nicho-alvo ou para leads antigos sem score). */
    @PostMapping("/rescore")
    public Map<String, Integer> rescore() {
        return Map.of("rescored", commands.rescoreAll());
    }
}
