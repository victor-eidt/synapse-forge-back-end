package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import synapseforge.crud.service.ArquivoUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Pedidos", description = "Pedidos de impressão e pintura e suas etapas de produção (Modelagem → Impressão → Pintura → Acabamento → Finalizado).")
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

        String roleName =
                authority.replace(
                        "ROLE_",
                        ""
                );

        return Role.valueOf(roleName);
    }


    // =========================================================
    // CRIAR PEDIDO - JSON
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Criar pedido (JSON)", description = "Cria um pedido na etapa MODELAGEM, na equipe do usuário logado.")
    @PostMapping
    public PedidoResponseDTO criar(
            @RequestBody @Valid PedidoRequestDTO dto,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Pedido pedido =
                service.toEntity(
                        dto,
                        usuarioId
                );

        Pedido salvo =
                service.criar(
                        pedido
                );

        return service.toResponseDTO(
                salvo
        );
    }


    // =========================================================
    // CRIAR PEDIDO COM ARQUIVOS
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Criar pedido com arquivos", description = "Mesmo cadastro, em multipart, com arquivo 3D e imagens de referência opcionais.")
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public PedidoResponseDTO criarComArquivos(
            @Parameter(description = "ID do usuário cliente", example = "6704a1c2e4b0f81a2c3d4e02") @RequestParam(value = "clienteId", required = false)
            String clienteId,

            @Parameter(description = "Nome do cliente", example = "Mariana Costa") @RequestParam(value = "cliente", required = false)
            String cliente,

            @Parameter(description = "Nome do projeto", example = "Miniatura do Dragão Vermelho") @RequestParam("projeto")
            String projeto,

            @Parameter(description = "Descrição do projeto", example = "Miniatura em escala 1:24 para RPG.") @RequestParam(value = "descricao", required = false)
            String descricao,

            @Parameter(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20") @RequestParam("prazo")
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate prazo,

            @Parameter(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03") @RequestParam(value = "materialId", required = false) String materialId,
            @Parameter(description = "Volume da peça, em cm³", example = "85.5") @RequestParam(value = "volumeCm3", required = false) Double volumeCm3,
            @Parameter(description = "Tempo de impressão, em horas", example = "6.5") @RequestParam(value = "tempoImpressaoHoras", required = false) Double tempoImpressaoHoras,
            @Parameter(description = "Tempo de mão de obra, em horas", example = "2.0") @RequestParam(value = "tempoMaoDeObraHoras", required = false) Double tempoMaoDeObraHoras,
            @Parameter(description = "Custo da impressora por hora, em R$", example = "4.50") @RequestParam(value = "custoMaquinaHora", required = false) BigDecimal custoMaquinaHora,
            @Parameter(description = "Custo da mão de obra por hora, em R$", example = "35.00") @RequestParam(value = "custoMaoDeObraHora", required = false) BigDecimal custoMaoDeObraHora,
            @Parameter(description = "Margem de lucro, em %", example = "30") @RequestParam(value = "margemLucro", required = false) BigDecimal margemLucro,
            @Parameter(description = "Custo do material, em R$", example = "12.72") @RequestParam(value = "custoMaterial", required = false) BigDecimal custoMaterial,
            @Parameter(description = "Custo de máquina, em R$", example = "29.25") @RequestParam(value = "custoMaquina", required = false) BigDecimal custoMaquina,
            @Parameter(description = "Custo de mão de obra, em R$", example = "70.00") @RequestParam(value = "custoMaoDeObra", required = false) BigDecimal custoMaoDeObra,
            @Parameter(description = "Custo total, em R$", example = "111.97") @RequestParam(value = "custoTotal", required = false) BigDecimal custoTotal,
            @Parameter(description = "Preço final, em R$", example = "145.56") @RequestParam(value = "precoFinal", required = false) BigDecimal precoFinal,

            @Parameter(description = "Arquivo 3D da peça (STL ou OBJ)") @RequestParam(value = "objeto3D", required = false)
            MultipartFile objeto3D,

            @Parameter(description = "Imagens de referência (PNG/JPG)") @RequestParam(
                    value = "imagensReferencia",
                    required = false
            )
            MultipartFile[] imagensReferencia,

            Authentication auth
    ) throws IOException {

        String usuarioId =
                (String) auth.getPrincipal();

        PedidoRequestDTO dto =
                new PedidoRequestDTO();

        dto.setClienteId(
                clienteId
        );

        dto.setCliente(
                cliente == null
                        ? ""
                        : cliente
        );

        dto.setProjeto(
                projeto
        );

        dto.setDescricao(
                descricao
        );

        dto.setPrazo(
                prazo
        );

        preencherDadosOrcamento(dto, materialId, volumeCm3, tempoImpressaoHoras, tempoMaoDeObraHoras, custoMaquinaHora, custoMaoDeObraHora, margemLucro, custoMaterial, custoMaquina, custoMaoDeObra, custoTotal, precoFinal);


        Pedido pedido =
                service.toEntity(
                        dto,
                        usuarioId
                );

        // Equipe e cliente validados ANTES de gravar no GridFS:
        // requisição recusada não deixa arquivo órfão.
        service.prepararParaCriacao(
                pedido
        );


        // =====================================================
        // ARQUIVO 3D
        // =====================================================

        if (
                objeto3D != null
                        && !objeto3D.isEmpty()
        ) {

            ObjectId fileId =
                    gridFsTemplate.store(
                            objeto3D.getInputStream(),
                            objeto3D.getOriginalFilename(),
                            objeto3D.getContentType()
                    );

            pedido.setObjeto3DFileId(
                    fileId.toHexString()
            );
        }


        // =====================================================
        // IMAGENS DE REFERÊNCIA
        // =====================================================

        if (
                imagensReferencia != null
                        && imagensReferencia.length > 0
        ) {

            List<String> imageIds =
                    new ArrayList<>();

            for (
                    MultipartFile f :
                    imagensReferencia
            ) {

                if (
                        f == null
                                || f.isEmpty()
                ) {
                    continue;
                }

                ObjectId id =
                        gridFsTemplate.store(
                                f.getInputStream(),
                                f.getOriginalFilename(),
                                f.getContentType()
                        );

                imageIds.add(
                        id.toHexString()
                );
            }

            pedido.setImagensReferenciaFileIds(
                    imageIds
            );
        }


        Pedido salvo =
                service.criar(
                        pedido
                );

        return service.toResponseDTO(
                salvo
        );
    }


    // =========================================================
    // LISTAR PEDIDOS
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Listar pedidos", description = "Cliente vê os próprios pedidos; equipe vê os pedidos da oficina. Filtro opcional por etapa.")
    @GetMapping
    public List<PedidoResponseDTO> listar(
            @Parameter(description = "Filtra pela etapa de produção", example = "IMPRESSAO") @RequestParam(
                    required = false
            )
            StatusPedido status,

            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        List<Pedido> pedidos =
                status != null
                        ? service.listarPorStatus(
                        usuarioId,
                        role,
                        status
                )
                        : service.listar(
                        usuarioId,
                        role
                );

        return pedidos
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }


    // =========================================================
    // BUSCAR PEDIDO POR ID
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Buscar pedido", description = "Retorna o pedido com as imagens de referência em base64.")
    @GetMapping("/{id}")
    public PedidoResponseDTO buscar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedido =
                service.buscarPorId(
                                id,
                                usuarioId,
                                role
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Pedido não encontrado"
                                        )
                        );

        return service.toResponseDTO(
                pedido
        );
    }


    // =========================================================
    // AVANÇAR STATUS
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Avançar etapa", description = "Passa o pedido para a próxima etapa, dá baixa nos insumos da etapa e avisa o cliente (sino + e-mail).")
    @PatchMapping("/{id}/status")
    public PedidoResponseDTO avancarStatus(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedido =
                service.avancarStatus(
                        id,
                        usuarioId,
                        role
                );

        return service.toResponseDTO(
                pedido
        );
    }


    // =========================================================
    // REGREDIR STATUS
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Voltar etapa", description = "Volta uma etapa, estorna os insumos da etapa abandonada e avisa o cliente.")
    @PatchMapping("/{id}/status/regredir")
    public PedidoResponseDTO regredirStatus(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedido =
                service.regredirStatus(
                        id,
                        usuarioId,
                        role
                );

        return service.toResponseDTO(
                pedido
        );
    }


    // =========================================================
    // CANCELAR PEDIDO
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Cancelar pedido", description = "Cancela o pedido (sem estorno de insumos já consumidos).")
    @PatchMapping("/{id}/cancelar")
    public PedidoResponseDTO cancelar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedido =
                service.cancelar(
                        id,
                        usuarioId,
                        role
                );

        return service.toResponseDTO(
                pedido
        );
    }


    // =========================================================
    // ATUALIZAR PEDIDO - JSON
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Editar pedido (JSON)", description = "Atualiza os dados do pedido. A etapa não muda por aqui.")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public PedidoResponseDTO atualizar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            @RequestBody @Valid PedidoRequestDTO dto,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedidoAtualizado =
                service.toEntity(
                        dto,
                        usuarioId
                );

        Pedido salvo =
                service.atualizar(
                        id,
                        usuarioId,
                        role,
                        pedidoAtualizado
                );

        return service.toResponseDTO(
                salvo
        );
    }


    // =========================================================
    // ATUALIZAR PEDIDO - MULTIPART
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Editar pedido com arquivos", description = "Mesma edição, em multipart, permitindo trocar o arquivo 3D e as imagens.")
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public PedidoResponseDTO atualizarComArquivos(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,

            @Parameter(description = "ID do usuário cliente", example = "6704a1c2e4b0f81a2c3d4e02") @RequestParam(
                    value = "clienteId",
                    required = false
            )
            String clienteId,

            @Parameter(description = "Nome do cliente", example = "Mariana Costa") @RequestParam(
                    value = "cliente",
                    required = false
            )
            String cliente,

            @Parameter(description = "Nome do projeto", example = "Miniatura do Dragão Vermelho") @RequestParam("projeto")
            String projeto,

            @Parameter(description = "Descrição do projeto", example = "Miniatura em escala 1:24 para RPG.") @RequestParam(
                    value = "descricao",
                    required = false
            )
            String descricao,

            @Parameter(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20") @RequestParam("prazo")
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate prazo,

            @Parameter(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03") @RequestParam(value = "materialId", required = false) String materialId,
            @Parameter(description = "Volume da peça, em cm³", example = "85.5") @RequestParam(value = "volumeCm3", required = false) Double volumeCm3,
            @Parameter(description = "Tempo de impressão, em horas", example = "6.5") @RequestParam(value = "tempoImpressaoHoras", required = false) Double tempoImpressaoHoras,
            @Parameter(description = "Tempo de mão de obra, em horas", example = "2.0") @RequestParam(value = "tempoMaoDeObraHoras", required = false) Double tempoMaoDeObraHoras,
            @Parameter(description = "Custo da impressora por hora, em R$", example = "4.50") @RequestParam(value = "custoMaquinaHora", required = false) BigDecimal custoMaquinaHora,
            @Parameter(description = "Custo da mão de obra por hora, em R$", example = "35.00") @RequestParam(value = "custoMaoDeObraHora", required = false) BigDecimal custoMaoDeObraHora,
            @Parameter(description = "Margem de lucro, em %", example = "30") @RequestParam(value = "margemLucro", required = false) BigDecimal margemLucro,
            @Parameter(description = "Custo do material, em R$", example = "12.72") @RequestParam(value = "custoMaterial", required = false) BigDecimal custoMaterial,
            @Parameter(description = "Custo de máquina, em R$", example = "29.25") @RequestParam(value = "custoMaquina", required = false) BigDecimal custoMaquina,
            @Parameter(description = "Custo de mão de obra, em R$", example = "70.00") @RequestParam(value = "custoMaoDeObra", required = false) BigDecimal custoMaoDeObra,
            @Parameter(description = "Custo total, em R$", example = "111.97") @RequestParam(value = "custoTotal", required = false) BigDecimal custoTotal,
            @Parameter(description = "Preço final, em R$", example = "145.56") @RequestParam(value = "precoFinal", required = false) BigDecimal precoFinal,

            @Parameter(description = "Filtra pela etapa de produção", example = "IMPRESSAO") @RequestParam(
                    value = "status",
                    required = false
            )
            StatusPedido status,

            @Parameter(description = "Arquivo 3D da peça (STL ou OBJ)") @RequestParam(
                    value = "objeto3D",
                    required = false
            )
            MultipartFile objeto3D,

            @Parameter(description = "true para remover o arquivo 3D atual", example = "false") @RequestParam(
                    value = "removerObjeto3D",
                    defaultValue = "false"
            )
            boolean removerObjeto3D,

            @Parameter(description = "Imagens de referência (PNG/JPG)") @RequestParam(
                    value = "imagensReferencia",
                    required = false
            )
            MultipartFile[] imagensReferencia,

            @Parameter(description = "IDs das imagens de referência que devem ser removidas", example = "6704a1c2e4b0f81a2c3d4e12") @RequestParam(
                    value = "imagensRemover",
                    required = false
            )
            List<String> imagensRemover,

            Authentication auth
    ) throws IOException {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);


        PedidoRequestDTO dto =
                new PedidoRequestDTO();

        dto.setClienteId(
                clienteId
        );

        dto.setCliente(
                cliente == null
                        ? ""
                        : cliente
        );

        dto.setProjeto(
                projeto
        );

        dto.setDescricao(
                descricao
        );

        dto.setPrazo(
                prazo
        );

        dto.setStatus(
                status
        );

        preencherDadosOrcamento(dto, materialId, volumeCm3, tempoImpressaoHoras, tempoMaoDeObraHoras, custoMaquinaHora, custoMaoDeObraHora, margemLucro, custoMaterial, custoMaquina, custoMaoDeObra, custoTotal, precoFinal);

        Pedido dados =
                service.toEntity(
                        dto,
                        usuarioId
                );

        // Perfil, equipe, pedido e cliente validados ANTES de gravar
        // no GridFS: requisição recusada não deixa arquivo órfão.
        service.validarAtualizacao(
                id,
                usuarioId,
                role,
                dados
        );


        // =====================================================
        // NOVO ARQUIVO 3D
        // =====================================================

        String novoObjetoId = null;

        if (
                objeto3D != null
                        && !objeto3D.isEmpty()
        ) {

            novoObjetoId =
                    gridFsTemplate.store(
                                    objeto3D.getInputStream(),
                                    objeto3D.getOriginalFilename(),
                                    objeto3D.getContentType()
                            )
                            .toHexString();
        }


        // =====================================================
        // NOVAS IMAGENS
        // =====================================================

        List<String> novasImagensIds =
                new ArrayList<>();

        if (
                imagensReferencia != null
        ) {

            for (
                    MultipartFile imagem :
                    imagensReferencia
            ) {

                if (
                        imagem == null
                                || imagem.isEmpty()
                ) {
                    continue;
                }

                novasImagensIds.add(
                        gridFsTemplate.store(
                                        imagem.getInputStream(),
                                        imagem.getOriginalFilename(),
                                        imagem.getContentType()
                                )
                                .toHexString()
                );
            }
        }


        Pedido salvo =
                service.atualizarComArquivos(
                        id,
                        usuarioId,
                        role,
                        dados,
                        novoObjetoId,
                        removerObjeto3D,
                        novasImagensIds,
                        imagensRemover
                );

        return service.toResponseDTO(
                salvo
        );
    }


    // =========================================================
    // DELETAR PEDIDO
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Excluir pedido", description = "Remove o pedido e seus arquivos.")
    @DeleteMapping("/{id}")
    public void deletar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        service.deletar(
                id,
                usuarioId,
                role
        );
    }


    // =========================================================
    // BAIXAR OBJETO 3D
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')"
    )
    @Operation(summary = "Baixar arquivo 3D", description = "Download do arquivo 3D do pedido.")
    @GetMapping("/{id}/obj3d")
    public ResponseEntity<
            org.springframework.core.io.InputStreamResource
            > getObjeto3D(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) throws IOException {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedido =
                service.buscarPorId(
                                id,
                                usuarioId,
                                role
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Pedido não encontrado"
                                        )
                        );


        String fileId =
                pedido.getObjeto3DFileId();

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
                                        .is(
                                                new ObjectId(
                                                        fileId
                                                )
                                        )
                        )
                );


        if (gridFsFile == null) {

            return ResponseEntity
                    .status(404)
                    .build();
        }


        org.springframework.data.mongodb.gridfs.GridFsResource resource =
                gridFsTemplate.getResource(
                        gridFsFile
                );


        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(
                        ArquivoUtils.contentType(gridFsFile)
                )
        );

        headers.setContentDisposition(
                org.springframework.http.ContentDisposition
                        .attachment()
                        .filename(
                                gridFsFile.getFilename()
                        )
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

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Gerar ordem de serviço (PDF)", description = "Gera o PDF da ordem de serviço do pedido.")
    @GetMapping("/{id}/ordem-servico")
    public ResponseEntity<byte[]> gerarOrdemServico(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Role role =
                getRole(auth);

        Pedido pedido =
                service.buscarPorId(
                                id,
                                usuarioId,
                                role
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Pedido não encontrado"
                                        )
                        );

        byte[] pdf =
                pdfService.gerarOrdemServico(
                        pedido
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ordem-servico.pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }

    private void preencherDadosOrcamento(PedidoRequestDTO dto, String materialId, Double volumeCm3,
            Double tempoImpressaoHoras, Double tempoMaoDeObraHoras, BigDecimal custoMaquinaHora,
            BigDecimal custoMaoDeObraHora, BigDecimal margemLucro, BigDecimal custoMaterial,
            BigDecimal custoMaquina, BigDecimal custoMaoDeObra, BigDecimal custoTotal, BigDecimal precoFinal) {
        dto.setMaterialId(materialId);
        dto.setVolumeCm3(volumeCm3);
        dto.setTempoImpressaoHoras(tempoImpressaoHoras);
        dto.setTempoMaoDeObraHoras(tempoMaoDeObraHoras);
        dto.setCustoMaquinaHora(custoMaquinaHora);
        dto.setCustoMaoDeObraHora(custoMaoDeObraHora);
        dto.setMargemLucro(margemLucro);
        dto.setCustoMaterial(custoMaterial);
        dto.setCustoMaquina(custoMaquina);
        dto.setCustoMaoDeObra(custoMaoDeObra);
        dto.setCustoTotal(custoTotal);
        dto.setPrecoFinal(precoFinal);
    }

}
