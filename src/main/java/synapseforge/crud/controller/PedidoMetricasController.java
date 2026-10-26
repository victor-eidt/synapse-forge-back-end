package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoMesMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.service.PedidoMetricasService;

import java.time.LocalDate;
import java.util.List;

/**
 * `de`/`ate` são opcionais em todos os endpoints e filtram pela data de criação do pedido
 * (inclusivas); `de` depois de `ate` responde 400.
 */
@Tag(name = "Métricas de pedidos", description = "Indicadores dos pedidos da equipe, calculados com aggregation no MongoDB.")
@RestController
@RequestMapping("/pedidos/metricas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
public class PedidoMetricasController {

    private final PedidoMetricasService service;

    @Operation(summary = "Pedidos por etapa", description = "Quantidade, valor total e atrasados por etapa do pedido, opcionalmente só dos criados no período.")
    @GetMapping("/por-status")
    public List<PedidoStatusMetricaDTO> pedidosPorStatus(
            @Parameter(description = "Criados a partir desta data (yyyy-MM-dd)", example = "2026-08-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Criados até esta data, inclusive (yyyy-MM-dd)", example = "2026-10-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            Authentication auth) {
        return service.pedidosPorStatus(de, ate, (String) auth.getPrincipal());
    }

    @Operation(summary = "Materiais mais usados", description = "Materiais que mais aparecem nos pedidos não cancelados da equipe, opcionalmente só dos criados no período.")
    @GetMapping("/materiais-mais-usados")
    public List<MaterialPedidoMetricaDTO> materiaisMaisUsados(
            @Parameter(description = "Quantos materiais retornar", example = "5") @RequestParam(defaultValue = "5") int limite,
            @Parameter(description = "Criados a partir desta data (yyyy-MM-dd)", example = "2026-08-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Criados até esta data, inclusive (yyyy-MM-dd)", example = "2026-10-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            Authentication auth) {
        return service.materiaisMaisUsados(limite, de, ate, (String) auth.getPrincipal());
    }

    @Operation(summary = "Pedidos por mês", description = "Pedidos criados em cada mês (yyyy-MM, em ordem cronológica): não cancelados com o valor total e cancelados à parte. Mês sem pedido não aparece.")
    @GetMapping("/por-mes")
    public List<PedidoMesMetricaDTO> pedidosPorMes(
            @Parameter(description = "Criados a partir desta data (yyyy-MM-dd)", example = "2026-08-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Criados até esta data, inclusive (yyyy-MM-dd)", example = "2026-10-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            Authentication auth) {
        return service.pedidosPorMes(de, ate, (String) auth.getPrincipal());
    }
}
