package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.bson.types.ObjectId;

import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PedidoService;
import synapseforge.crud.service.PdfService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService service;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private PdfService pdfService;


    // =========================================================
    // MÉTODO AUXILIAR - PEGA A ROLE DO USUÁRIO LOGADO
    // =========================================================

    private Role getRole(Authentication auth) {

        String authority = auth.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        // Remove o "ROLE_" colocado pelo JwtFilter
        String roleName = authority.replace("ROLE_", "");

        return Role.valueOf(roleName);
    }


    // =========================================================
    // CRIAR PEDIDO
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @PostMapping
    public PedidoResponseDTO criar(
            @RequestBody @Valid PedidoRequestDTO dto,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Pedido pedido = service.toEntity(dto, usuarioId);

        Pedido salvo = service.criar(pedido);

        return service.toResponseDTO(salvo);
    }


    // =========================================================
    // CRIAR PEDIDO COM ARQUIVOS
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PedidoResponseDTO criarComArquivos(
            @RequestParam("cliente") String cliente,
            @RequestParam("projeto") String projeto,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam("prazo")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate prazo,
            @RequestParam(value = "objeto3D", required = false)
            MultipartFile objeto3D,
            @RequestParam(value = "imagensReferencia", required = false)
            MultipartFile[] imagensReferencia,
            Authentication auth
    ) throws IOException {

        String usuarioId = (String) auth.getPrincipal();

        PedidoRequestDTO dto = new PedidoRequestDTO();

        dto.setCliente(cliente);
        dto.setProjeto(projeto);
        dto.setDescricao(descricao);
        dto.setPrazo(prazo);

        Pedido pedido = service.toEntity(dto, usuarioId);


        if (objeto3D != null && !objeto3D.isEmpty()) {

            ObjectId fileId = gridFsTemplate.store(
                    objeto3D.getInputStream(),
                    objeto3D.getOriginalFilename(),
                    objeto3D.getContentType()
            );

            pedido.setObjeto3DFileId(fileId.toHexString());
        }


        if (imagensReferencia != null && imagensReferencia.length > 0) {

            List<String> imageIds = new ArrayList<>();

            for (MultipartFile f : imagensReferencia) {

                if (f == null || f.isEmpty()) {
                    continue;
                }

                ObjectId id = gridFsTemplate.store(
                        f.getInputStream(),
                        f.getOriginalFilename(),
                        f.getContentType()
                );

                imageIds.add(id.toHexString());
            }

            pedido.setImagensReferenciaFileIds(imageIds);
        }


        Pedido salvo = service.criar(pedido);

        return service.toResponseDTO(salvo);
    }


    // =========================================================
    // LISTAR PEDIDOS
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @GetMapping
    public List<PedidoResponseDTO> listar(
            @RequestParam(required = false) StatusPedido status,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        List<Pedido> pedidos =
                status != null
                        ? service.listarPorStatus(usuarioId, role, status)
                        : service.listar(usuarioId, role);

        return pedidos
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }


    // =========================================================
    // BUSCAR PEDIDO POR ID
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @GetMapping("/{id}")
    public PedidoResponseDTO buscar(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        Pedido pedido = service.buscarPorId(
                        id,
                        usuarioId,
                        role
                )
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado")
                );

        return service.toResponseDTO(pedido);
    }


    // =========================================================
    // AVANÇAR STATUS
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public PedidoResponseDTO avancarStatus(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        Pedido pedido = service.avancarStatus(
                id,
                usuarioId,
                role
        );

        return service.toResponseDTO(pedido);
    }


    // =========================================================
    // REGREDIR STATUS
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @PatchMapping("/{id}/status/regredir")
    public PedidoResponseDTO regredirStatus(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        Pedido pedido = service.regredirStatus(
                id,
                usuarioId,
                role
        );

        return service.toResponseDTO(pedido);
    }


    // =========================================================
    // ATUALIZAR PEDIDO - JSON
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public PedidoResponseDTO atualizar(
            @PathVariable String id,
            @RequestBody @Valid PedidoRequestDTO dto,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        Pedido pedidoAtualizado =
                service.toEntity(dto, usuarioId);

        Pedido salvo =
                service.atualizar(
                        id,
                        usuarioId,
                        role,
                        pedidoAtualizado
                );

        return service.toResponseDTO(salvo);
    }


    // =========================================================
    // ATUALIZAR PEDIDO - MULTIPART
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public PedidoResponseDTO atualizarComArquivos(
            @PathVariable String id,

            @RequestParam("cliente")
            String cliente,

            @RequestParam("projeto")
            String projeto,

            @RequestParam(value = "descricao", required = false)
            String descricao,

            @RequestParam("prazo")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate prazo,

            @RequestParam(value = "status", required = false)
            StatusPedido status,

            @RequestParam(value = "objeto3D", required = false)
            MultipartFile objeto3D,

            @RequestParam(
                    value = "removerObjeto3D",
                    defaultValue = "false"
            )
            boolean removerObjeto3D,

            @RequestParam(
                    value = "imagensReferencia",
                    required = false
            )
            MultipartFile[] imagensReferencia,

            @RequestParam(
                    value = "imagensRemover",
                    required = false
            )
            List<String> imagensRemover,

            Authentication auth
    ) throws IOException {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);


        PedidoRequestDTO dto = new PedidoRequestDTO();

        dto.setCliente(cliente);
        dto.setProjeto(projeto);
        dto.setDescricao(descricao);
        dto.setPrazo(prazo);
        dto.setStatus(status);


        // =====================================================
        // NOVO ARQUIVO 3D
        // =====================================================

        String novoObjetoId = null;

        if (objeto3D != null && !objeto3D.isEmpty()) {

            novoObjetoId = gridFsTemplate.store(
                    objeto3D.getInputStream(),
                    objeto3D.getOriginalFilename(),
                    objeto3D.getContentType()
            ).toHexString();
        }


        // =====================================================
        // NOVAS IMAGENS
        // =====================================================

        List<String> novasImagensIds = new ArrayList<>();

        if (imagensReferencia != null) {

            for (MultipartFile imagem : imagensReferencia) {

                if (imagem == null || imagem.isEmpty()) {
                    continue;
                }

                novasImagensIds.add(
                        gridFsTemplate.store(
                                imagem.getInputStream(),
                                imagem.getOriginalFilename(),
                                imagem.getContentType()
                        ).toHexString()
                );
            }
        }


        // =====================================================
        // ATUALIZAÇÃO
        // =====================================================

        Pedido salvo = service.atualizarComArquivos(
                id,
                usuarioId,
                role,
                service.toEntity(dto, usuarioId),
                novoObjetoId,
                removerObjeto3D,
                novasImagensIds,
                imagensRemover
        );

        return service.toResponseDTO(salvo);
    }


    // =========================================================
    // DELETAR PEDIDO
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @DeleteMapping("/{id}")
    public void deletar(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        service.deletar(
                id,
                usuarioId,
                role
        );
    }


    // =========================================================
    // BAIXAR OBJETO 3D
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @GetMapping("/{id}/obj3d")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> getObjeto3D(
            @PathVariable String id,
            Authentication auth
    ) throws IOException {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        Pedido pedido = service.buscarPorId(
                        id,
                        usuarioId,
                        role
                )
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado")
                );


        String fileId = pedido.getObjeto3DFileId();

        if (fileId == null) {

            return ResponseEntity
                    .status(404)
                    .build();
        }


        com.mongodb.client.gridfs.model.GridFSFile gridFsFile =
                gridFsTemplate.findOne(
                        new org.springframework.data.mongodb.core.query.Query(
                                new org.springframework.data.mongodb.core.query.Criteria()
                                        .where("_id")
                                        .is(new ObjectId(fileId))
                        )
                );


        if (gridFsFile == null) {

            return ResponseEntity
                    .status(404)
                    .build();
        }


        org.springframework.data.mongodb.gridfs.GridFsResource resource =
                gridFsTemplate.getResource(gridFsFile);


        String contentType = "application/octet-stream";

        if (gridFsFile.getMetadata() != null) {

            if (gridFsFile.getMetadata().getString("contentType") != null) {

                contentType =
                        gridFsFile.getMetadata()
                                .getString("contentType");

            } else if (
                    gridFsFile.getMetadata()
                            .getString("_contentType") != null
            ) {

                contentType =
                        gridFsFile.getMetadata()
                                .getString("_contentType");
            }
        }


        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(contentType)
        );

        headers.setContentDisposition(
                org.springframework.http.ContentDisposition
                        .attachment()
                        .filename(gridFsFile.getFilename())
                        .build()
        );


        org.springframework.core.io.InputStreamResource body =
                new org.springframework.core.io.InputStreamResource(
                        resource.getInputStream()
                );


        return new ResponseEntity<>(
                body,
                headers,
                org.springframework.http.HttpStatus.OK
        );
    }


    // =========================================================
    // GERAR ORDEM DE SERVIÇO
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @GetMapping("/{id}/ordem-servico")
    public ResponseEntity<byte[]> gerarOrdemServico(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        Pedido pedido = service.buscarPorId(
                        id,
                        usuarioId,
                        role
                )
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado")
                );

        byte[] pdf =
                pdfService.gerarOrdemServico(pedido);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ordem-servico.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}