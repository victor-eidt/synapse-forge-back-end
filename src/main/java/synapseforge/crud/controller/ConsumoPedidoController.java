package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoResponseDTO;
import synapseforge.crud.service.ConsumoPedidoService;

@Tag(name = "Consumo de insumos", description = "Ficha de consumo do pedido: quais insumos são baixados do estoque em cada etapa.")
@RestController
@RequestMapping("/consumos-pedido")
@RequiredArgsConstructor
public class ConsumoPedidoController {

    private final ConsumoPedidoService service;

    @Operation(summary = "Salvar ficha de consumo", description = "Cria ou substitui a ficha de consumo do pedido. As baixas acontecem automaticamente quando o pedido avança de etapa.")
    @PostMapping
    public ConsumoPedidoResponseDTO salvar(@RequestBody @Valid ConsumoPedidoRequestDTO dto, Authentication auth) {
        return service.salvar(dto, (String) auth.getPrincipal());
    }

    @Operation(summary = "Buscar ficha de consumo", description = "Retorna a ficha de consumo de um pedido.")
    @GetMapping("/{pedidoId}")
    public ConsumoPedidoResponseDTO buscarPorPedido(@Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String pedidoId, Authentication auth) {
        return service.buscarPorPedido(pedidoId, (String) auth.getPrincipal());
    }
}
