package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PedidoService;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService service;

    @PostMapping
    public PedidoResponseDTO criar(@RequestBody @Valid PedidoRequestDTO dto) {
        Pedido pedido = service.toEntity(dto);
        Pedido salvo = service.criar(pedido);
        return service.toResponseDTO(salvo);
    }

    @GetMapping
    public List<PedidoResponseDTO> listar(@RequestParam(required = false) StatusPedido status) {
        List<Pedido> pedidos = status != null
                ? service.listarPorStatus(status)
                : service.listar();
        return pedidos.stream().map(service::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponseDTO buscar(@PathVariable String id) {
        Pedido pedido = service.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        return service.toResponseDTO(pedido);
    }

    @PatchMapping("/{id}/status")
    public PedidoResponseDTO avancarStatus(@PathVariable String id) {
        Pedido pedido = service.avancarStatus(id);
        return service.toResponseDTO(pedido);
    }

    @PutMapping("/{id}")
    public PedidoResponseDTO atualizar(@PathVariable String id, @RequestBody @Valid PedidoRequestDTO dto) {
        Pedido pedidoAtualizado = service.toEntity(dto);
        Pedido salvo = service.atualizar(id, pedidoAtualizado);
        return service.toResponseDTO(salvo);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }
}
