package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PedidoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService service;

    @PostMapping
    public PedidoResponseDTO criar(@RequestBody @Valid PedidoRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Pedido pedido = service.toEntity(dto, usuarioId);
        Pedido salvo = service.criar(pedido);
        return service.toResponseDTO(salvo);
    }

    // Multipart endpoint to accept one 3D file and multiple reference images
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PedidoResponseDTO criarComArquivos(
            @RequestParam("cliente") String cliente,
            @RequestParam("projeto") String projeto,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam("prazo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo,
            @RequestParam(value = "objeto3D", required = false) MultipartFile objeto3D,
            @RequestParam(value = "imagensReferencia", required = false) MultipartFile[] imagensReferencia,
            Authentication auth
    ) throws IOException {
        String usuarioId = (String) auth.getPrincipal();

        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setCliente(cliente);
        dto.setProjeto(projeto);
        dto.setDescricao(descricao);
        dto.setPrazo(prazo);

        Pedido pedido = service.toEntity(dto, usuarioId);

        // save 3D object
        if (objeto3D != null && !objeto3D.isEmpty()) {
            String filename = UUID.randomUUID().toString() + "_" + objeto3D.getOriginalFilename();
            Path dir = Paths.get("uploads", "obj3d");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            objeto3D.transferTo(target.toFile());
            pedido.setObjeto3DPath(target.toString());
        }

        // save reference images
        if (imagensReferencia != null && imagensReferencia.length > 0) {
            List<String> imagePaths = new ArrayList<>();
            Path dir = Paths.get("uploads", "images");
            Files.createDirectories(dir);
            for (MultipartFile f : imagensReferencia) {
                if (f == null || f.isEmpty()) continue;
                String filename = UUID.randomUUID().toString() + "_" + f.getOriginalFilename();
                Path target = dir.resolve(filename);
                f.transferTo(target.toFile());
                imagePaths.add(target.toString());
            }
            pedido.setImagensReferenciaPaths(imagePaths);
        }

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
}
