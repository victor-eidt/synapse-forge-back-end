package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.service.PedidoMetricasService;

import java.util.List;

@Tag(name = "Métricas de pedidos", description = "Indicadores dos pedidos da equipe, calculados com aggregation no MongoDB.")
@RestController
@RequestMapping("/pedidos/metricas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
public class PedidoMetricasController {

    private final PedidoMetricasService service;

    @Operation(summary = "Pedidos por etapa", description = "Quantidade, valor total e atrasados por etapa do pedido.")
    @GetMapping("/por-status")
    public List<PedidoStatusMetricaDTO> pedidosPorStatus(Authentication auth) {
        return service.pedidosPorStatus((String) auth.getPrincipal());
    }

    @Operation(summary = "Materiais mais usados", description = "Materiais que mais aparecem nos pedidos não cancelados da equipe.")
    @GetMapping("/materiais-mais-usados")
    public List<MaterialPedidoMetricaDTO> materiaisMaisUsados(
            @Parameter(description = "Quantos materiais retornar", example = "5") @RequestParam(defaultValue = "5") int limite,
            Authentication auth) {
        return service.materiaisMaisUsados(limite, (String) auth.getPrincipal());
    }
}
