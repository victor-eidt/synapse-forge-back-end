package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoResponseDTO;
import synapseforge.crud.service.ConsumoPedidoService;

@RestController
@RequestMapping("/consumos-pedido")
@RequiredArgsConstructor
public class ConsumoPedidoController {

    private final ConsumoPedidoService service;

    @PostMapping
    public ConsumoPedidoResponseDTO salvar(@RequestBody @Valid ConsumoPedidoRequestDTO dto, Authentication auth) {
        return service.salvar(dto);
    }

    @GetMapping("/{pedidoId}")
    public ConsumoPedidoResponseDTO buscarPorPedido(@PathVariable String pedidoId, Authentication auth) {
        return service.buscarPorPedido(pedidoId);
    }
}
