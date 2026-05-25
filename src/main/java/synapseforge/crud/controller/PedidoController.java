package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PedidoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import synapseforge.crud.service.PdfService;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private PedidoService service;

    @PostMapping
    public PedidoResponseDTO criar(@RequestBody @Valid PedidoRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Pedido pedido = service.toEntity(dto, usuarioId);
        Pedido salvo = service.criar(pedido);
        return service.toResponseDTO(salvo);
    }

    @GetMapping
    public List<PedidoResponseDTO> listar(@RequestParam(required = false) StatusPedido status, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        List<Pedido> pedidos = status != null
                ? service.listarPorStatus(usuarioId, status)
                : service.listar(usuarioId);
        return pedidos.stream().map(service::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponseDTO buscar(@PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Pedido pedido = service.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        return service.toResponseDTO(pedido);
    }

    @PatchMapping("/{id}/status")
    public PedidoResponseDTO avancarStatus(@PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Pedido pedido = service.avancarStatus(id, usuarioId);
        return service.toResponseDTO(pedido);
    }

    @PutMapping("/{id}")
    public PedidoResponseDTO atualizar(@PathVariable String id, @RequestBody @Valid PedidoRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Pedido pedidoAtualizado = service.toEntity(dto, usuarioId);
        Pedido salvo = service.atualizar(id, usuarioId, pedidoAtualizado);
        return service.toResponseDTO(salvo);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        service.deletar(id, usuarioId);
    }


    @GetMapping("/{id}/ordem-servico")
    public ResponseEntity<byte[]> gerarOrdemServico(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Pedido pedido = service.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        byte[] pdf = pdfService.gerarOrdemServico(pedido);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ordem-servico.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
