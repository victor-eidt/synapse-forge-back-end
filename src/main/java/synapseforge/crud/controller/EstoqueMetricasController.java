package synapseforge.crud.controller;

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

@RestController
@RequestMapping("/estoque/metricas")
@RequiredArgsConstructor
public class EstoqueMetricasController {

    private final EstoqueMetricasService service;

    @GetMapping("/consumo-por-insumo")
    public List<ConsumoInsumoMetricaDTO> consumoPorInsumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {
        return service.consumoPorInsumo(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX));
    }

    @GetMapping("/custo-pedido/{pedidoId}")
    public CustoPedidoResponseDTO custoPorPedido(@PathVariable String pedidoId, Authentication auth) {
        return service.custoPorPedido(pedidoId);
    }

    @GetMapping("/consumo-por-etapa")
    public List<ConsumoEtapaMetricaDTO> consumoPorEtapa(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication auth) {
        return service.consumoPorEtapa(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX));
    }

    @GetMapping("/consumo-medio-semanal")
    public ConsumoMedioSemanalResponseDTO consumoMedioSemanal(@RequestParam TipoInsumo tipoInsumo,
                                                              @RequestParam String insumoId,
                                                              @RequestParam(defaultValue = "4") int semanas,
                                                              Authentication auth) {
        return service.consumoMedioSemanal(tipoInsumo, insumoId, semanas);
    }

    @GetMapping("/insumos-criticos")
    public List<InsumoCriticoResponseDTO> insumosCriticos(Authentication auth) {
        return service.insumosCriticos();
    }
}
