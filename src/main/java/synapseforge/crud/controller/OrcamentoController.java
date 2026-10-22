package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import synapseforge.crud.DTO.Comum.PaginaResponseDTO;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.FiltroOrcamentoDTO;
import synapseforge.crud.DTO.Orcamento.SituacaoOrcamento;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.service.OrcamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import synapseforge.crud.service.ArquivoUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Orçamentos", description = "Cálculo e histórico de orçamentos de impressão (papel GERENTE).")
@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;
    private final GridFsTemplate gridFsTemplate;

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Calcular orçamento (sem salvar)", description = "Calcula custos e preço final a partir do volume, tempos e margem. Útil para pré-visualização.")
    @PostMapping("/calcular")
    public OrcamentoResponseDTO calcular(@RequestBody @Valid CalcularOrcamentoRequestDTO dto, Authentication auth) {
        return service.calcular(dto, (String) auth.getPrincipal());
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Salvar orçamento (JSON)", description = "Calcula e salva o orçamento como PENDENTE.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponseDTO salvar(@RequestBody @Valid CalcularOrcamentoRequestDTO dto, Authentication auth) {
        return service.salvar(dto, (String) auth.getPrincipal());
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Salvar orçamento com arquivos", description = "Mesmo cálculo, em multipart, com arquivo 3D e imagens de referência opcionais.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponseDTO salvarComArquivos(
            @Parameter(description = "Nome do cliente", example = "Mariana Costa") @RequestParam("cliente") String cliente,
            @Parameter(description = "Nome do projeto", example = "Miniatura do Dragão Vermelho") @RequestParam("projeto") String projeto,
            @Parameter(description = "Descrição do projeto", example = "Miniatura em escala 1:24 para RPG.") @RequestParam(value = "descricao", required = false) String descricao,
            @Parameter(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20") @RequestParam("prazo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo,
            @Parameter(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03") @RequestParam("materialId") String materialId,
            @Parameter(description = "Volume da peça, em cm³", example = "85.5") @RequestParam("volumeCm3") Double volumeCm3,
            @Parameter(description = "Tempo de impressão, em horas", example = "6.5") @RequestParam("tempoImpressaoHoras") Double tempoImpressaoHoras,
            @Parameter(description = "Tempo de mão de obra, em horas", example = "2.0") @RequestParam("tempoMaoDeObraHoras") Double tempoMaoDeObraHoras,
            @Parameter(description = "Custo da impressora por hora, em R$", example = "4.50") @RequestParam("custoMaquinaHora") BigDecimal custoMaquinaHora,
            @Parameter(description = "Custo da mão de obra por hora, em R$", example = "35.00") @RequestParam("custoMaoDeObraHora") BigDecimal custoMaoDeObraHora,
            @Parameter(description = "Margem de lucro, em %", example = "30") @RequestParam("margemLucro") BigDecimal margemLucro,
            @Parameter(description = "Arquivo 3D da peça (STL ou OBJ)") @RequestParam(value = "objeto3D", required = false) MultipartFile objeto3D,
            @Parameter(description = "Imagens de referência (PNG/JPG)") @RequestParam(value = "imagensReferencia", required = false) MultipartFile[] imagensReferencia,
            Authentication auth
    ) throws IOException {
        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        dto.setCliente(cliente);
        dto.setProjeto(projeto);
        dto.setDescricao(descricao);
        dto.setPrazo(prazo);
        dto.setMaterialId(materialId);
        dto.setVolumeCm3(volumeCm3);
        dto.setTempoImpressaoHoras(tempoImpressaoHoras);
        dto.setTempoMaoDeObraHoras(tempoMaoDeObraHoras);
        dto.setCustoMaquinaHora(custoMaquinaHora);
        dto.setCustoMaoDeObraHora(custoMaoDeObraHora);
        dto.setMargemLucro(margemLucro);

        // Equipe e material validados ANTES de gravar no GridFS (o cálculo recusa
        // sem equipe ou material inexistente/inativo): nada fica órfão.
        service.calcular(dto, (String) auth.getPrincipal());

        String objeto3DFileId = armazenarArquivo(objeto3D);
        List<String> imagensReferenciaFileIds = new ArrayList<>();
        if (imagensReferencia != null) {
            for (MultipartFile imagem : imagensReferencia) {
                String imagemId = armazenarArquivo(imagem);
                if (imagemId != null) imagensReferenciaFileIds.add(imagemId);
            }
        }

        return service.salvar(dto, (String) auth.getPrincipal(), objeto3DFileId, imagensReferenciaFileIds);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Listar orçamentos", description = "Todos os orçamentos da equipe (sem paginação). Prefira GET /orcamentos/busca.")
    @GetMapping
    public List<OrcamentoResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal());
    }

    /**
     * Busca paginada: GET /orcamentos/busca?situacao=PENDENTES&cliente=ana&de=2026-09-01&pagina=0&tamanho=20
     * `de`/`ate` filtram pela data de criação (inclusivas); tamanho máximo 100.
     */
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Buscar orçamentos (paginado)", description = "Pendentes ou histórico, filtrando por cliente, projeto e período de criação.")
    @GetMapping("/busca")
    public PaginaResponseDTO<OrcamentoResponseDTO> buscar(
            @Parameter(description = "PENDENTES (aguardando decisão) ou DECIDIDOS (histórico)", example = "PENDENTES") @RequestParam(defaultValue = "PENDENTES") SituacaoOrcamento situacao,
            @Parameter(description = "Nome do cliente", example = "Mariana Costa") @RequestParam(required = false) String cliente,
            @Parameter(description = "Nome do projeto", example = "Miniatura do Dragão Vermelho") @RequestParam(required = false) String projeto,
            @Parameter(description = "Criados a partir desta data (yyyy-MM-dd)", example = "2026-09-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = "Criados até esta data, inclusive (yyyy-MM-dd)", example = "2026-09-30") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @Parameter(description = "Número da página (começa em 0)", example = "0") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Itens por página (máximo 100)", example = "20") @RequestParam(defaultValue = "20") int tamanho,
            Authentication auth
    ) {
        return service.buscar(
                (String) auth.getPrincipal(),
                new FiltroOrcamentoDTO(situacao, cliente, projeto, de, ate),
                pagina,
                tamanho
        );
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Buscar orçamento", description = "Retorna o orçamento com as imagens de referência em base64.")
    @GetMapping("/{id}")
    public OrcamentoResponseDTO buscarPorId(@Parameter(description = "ID do orçamento", example = "6704a1c2e4b0f81a2c3d4e07") @PathVariable String id, Authentication auth) {
        return service.buscarPorId(id, (String) auth.getPrincipal());
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Baixar arquivo 3D", description = "Download do arquivo 3D do orçamento.")
    @GetMapping("/{id}/obj3d")
    public ResponseEntity<InputStreamResource> baixarObjeto3D(
            @Parameter(description = "ID do orçamento", example = "6704a1c2e4b0f81a2c3d4e07") @PathVariable String id,
            Authentication auth
    ) throws IOException {
        OrcamentoResponseDTO orcamento = buscarPorId(id, auth);
        String fileId = orcamento.getObjeto3DFileId();
        if (fileId == null || !ObjectId.isValid(fileId)) {
            return ResponseEntity.notFound().build();
        }

        return servirArquivo(fileId, true);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Ver imagem de referência", description = "Retorna o binário de uma imagem de referência.")
    @GetMapping("/{id}/imagens/{imagemId}")
    public ResponseEntity<InputStreamResource> visualizarImagem(
            @Parameter(description = "ID do orçamento", example = "6704a1c2e4b0f81a2c3d4e07") @PathVariable String id,
            @Parameter(description = "ID da imagem no GridFS", example = "6704a1c2e4b0f81a2c3d4e12") @PathVariable String imagemId,
            Authentication auth
    ) throws IOException {
        OrcamentoResponseDTO orcamento = buscarPorId(id, auth);
        if (!ObjectId.isValid(imagemId)
                || orcamento.getImagensReferenciaIds().stream()
                        .noneMatch(imagemId::equals)) {
            return ResponseEntity.notFound().build();
        }

        return servirArquivo(imagemId, false);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Aprovar orçamento", description = "Aprova o orçamento e gera o pedido correspondente.")
    @PatchMapping("/{id}/aprovar")
    public OrcamentoResponseDTO aprovar(@Parameter(description = "ID do orçamento", example = "6704a1c2e4b0f81a2c3d4e07") @PathVariable String id, Authentication auth) {
        return service.aprovar(id, (String) auth.getPrincipal());
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Rejeitar orçamento", description = "Marca o orçamento como rejeitado.")
    @PatchMapping("/{id}/rejeitar")
    public OrcamentoResponseDTO rejeitar(@Parameter(description = "ID do orçamento", example = "6704a1c2e4b0f81a2c3d4e07") @PathVariable String id, Authentication auth) {
        return service.rejeitar(id, (String) auth.getPrincipal());
    }

    private String armazenarArquivo(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) return null;
        ObjectId fileId = gridFsTemplate.store(
                arquivo.getInputStream(),
                arquivo.getOriginalFilename(),
                arquivo.getContentType()
        );
        return fileId.toHexString();
    }

    private ResponseEntity<InputStreamResource> servirArquivo(
            String fileId,
            boolean comoAnexo
    ) throws IOException {
        var gridFsFile = gridFsTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(fileId)))
        );
        if (gridFsFile == null) {
            return ResponseEntity.notFound().build();
        }

        GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
        MediaType mediaType = MediaType.parseMediaType(
                ArquivoUtils.contentType(gridFsFile)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        if (comoAnexo) {
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(gridFsFile.getFilename())
                            .build()
            );
        } else {
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.inline()
                            .filename(gridFsFile.getFilename())
                            .build()
            );
        }

        return new ResponseEntity<>(
                new InputStreamResource(resource.getInputStream()),
                headers,
                HttpStatus.OK
        );
    }
}
