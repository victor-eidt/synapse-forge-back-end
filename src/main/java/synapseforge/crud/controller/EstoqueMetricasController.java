package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Estoque.ConsumoEtapaMetricaDTO;
import synapseforge.crud.DTO.Estoque.ConsumoInsumoMetricaDTO;
import synapseforge.crud.DTO.Estoque.ConsumoMedioSemanalResponseDTO;
import synapseforge.crud.DTO.Estoque.CustoPedidoResponseDTO;
import synapseforge.crud.DTO.Estoque.InsumoCriticoResponseDTO;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.service.EstoqueMetricasService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "Métricas de estoque", description = "Indicadores de consumo e custo de insumos.")
@RestController
@RequestMapping("/estoque/metricas")
@RequiredArgsConstructor
public class EstoqueMetricasController {

    private final EstoqueMetricasService service;

    @Operation(summary = "Consumo por insumo", description = "Total consumido e custo por insumo no período.")
    @GetMapping("/consumo-por-insumo")
    public List<ConsumoInsumoMetricaDTO> consumoPorInsumo(
            @Parameter(description = "Início do período (yyyy-MM-dd)", example = "2026-09-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fim do período (yyyy-MM-dd)", example = "2026-09-30") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {
        return service.consumoPorInsumo(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX), (String) auth.getPrincipal());
    }

    @Operation(summary = "Custo real do pedido", description = "Custo de insumos baixados para o pedido, total e por etapa.")
    @GetMapping("/custo-pedido/{pedidoId}")
    public CustoPedidoResponseDTO custoPorPedido(@Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String pedidoId, Authentication auth) {
        return service.custoPorPedido(pedidoId, (String) auth.getPrincipal());
    }

    @Operation(summary = "Consumo por etapa", description = "Total consumido e custo por etapa de produção no período.")
    @GetMapping("/consumo-por-etapa")
    public List<ConsumoEtapaMetricaDTO> consumoPorEtapa(
            @Parameter(description = "Início do período (yyyy-MM-dd)", example = "2026-09-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fim do período (yyyy-MM-dd)", example = "2026-09-30") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {
        return service.consumoPorEtapa(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX), (String) auth.getPrincipal());
    }

    @Operation(summary = "Consumo médio semanal", description = "Média de consumo semanal de um insumo nas últimas N semanas.")
    @GetMapping("/consumo-medio-semanal")
    public ConsumoMedioSemanalResponseDTO consumoMedioSemanal(@Parameter(description = "Tipo do insumo", example = "MATERIAL") @RequestParam TipoInsumo tipoInsumo,
                                                              @Parameter(description = "ID do material ou da cor", example = "6704a1c2e4b0f81a2c3d4e03") @RequestParam String insumoId,
                                                              @Parameter(description = "Quantidade de semanas para a média", example = "4") @RequestParam(defaultValue = "4") int semanas,
                                                              Authentication auth) {
        return service.consumoMedioSemanal(tipoInsumo, insumoId, semanas, (String) auth.getPrincipal());
    }

    @Operation(summary = "Insumos críticos", description = "Insumos em risco de acabar, com a cobertura estimada em dias.")
    @GetMapping("/insumos-criticos")
    public List<InsumoCriticoResponseDTO> insumosCriticos(Authentication auth) {
        return service.insumosCriticos((String) auth.getPrincipal());
    }
}
