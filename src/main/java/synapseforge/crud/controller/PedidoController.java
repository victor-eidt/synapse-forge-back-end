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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.bson.types.ObjectId;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService service;

    @Autowired
    private GridFsTemplate gridFsTemplate;

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

            // store 3D object in GridFS
        if (objeto3D != null && !objeto3D.isEmpty()) {
            ObjectId fileId = gridFsTemplate.store(objeto3D.getInputStream(), objeto3D.getOriginalFilename(), objeto3D.getContentType());
            pedido.setObjeto3DFileId(fileId.toHexString());
        }

        // store reference images in GridFS
        if (imagensReferencia != null && imagensReferencia.length > 0) {
            List<String> imageIds = new ArrayList<>();
            for (MultipartFile f : imagensReferencia) {
                if (f == null || f.isEmpty()) continue;
                ObjectId id = gridFsTemplate.store(f.getInputStream(), f.getOriginalFilename(), f.getContentType());
                imageIds.add(id.toHexString());
            }
            pedido.setImagensReferenciaFileIds(imageIds);
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

    @PatchMapping("/{id}/status/regredir")
    public PedidoResponseDTO regredirStatus(@PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Pedido pedido = service.regredirStatus(id, usuarioId);
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

    @GetMapping("/{id}/obj3d")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> getObjeto3D(@PathVariable String id, Authentication auth) throws java.io.IOException {
        String usuarioId = (String) auth.getPrincipal();
        synapseforge.crud.infrastructure.entity.Pedido pedido = service.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        String fileId = pedido.getObjeto3DFileId();
        if (fileId == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        com.mongodb.client.gridfs.model.GridFSFile gridFsFile = gridFsTemplate.findOne(new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(new org.bson.types.ObjectId(fileId))));
        if (gridFsFile == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(gridFsFile);

        String contentType = "application/octet-stream";
        if (gridFsFile.getMetadata() != null) {
            if (gridFsFile.getMetadata().getString("contentType") != null) {
                contentType = gridFsFile.getMetadata().getString("contentType");
            } else if (gridFsFile.getMetadata().getString("_contentType") != null) {
                contentType = gridFsFile.getMetadata().getString("_contentType");
            }
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename(gridFsFile.getFilename()).build());

        org.springframework.core.io.InputStreamResource body = new org.springframework.core.io.InputStreamResource(resource.getInputStream());
        return new org.springframework.http.ResponseEntity<>(body, headers, org.springframework.http.HttpStatus.OK);
    }
}
