package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.service.OrcamentoService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;
    private final GridFsTemplate gridFsTemplate;

    @PostMapping("/calcular")
    public OrcamentoResponseDTO calcular(@RequestBody @Valid CalcularOrcamentoRequestDTO dto) {
        return service.calcular(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponseDTO salvar(@RequestBody @Valid CalcularOrcamentoRequestDTO dto, Authentication auth) {
        return service.salvar(dto, (String) auth.getPrincipal());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponseDTO salvarComArquivos(
            @RequestParam("cliente") String cliente,
            @RequestParam("projeto") String projeto,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam("prazo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo,
            @RequestParam("materialId") String materialId,
            @RequestParam("volumeCm3") Double volumeCm3,
            @RequestParam("tempoImpressaoHoras") Double tempoImpressaoHoras,
            @RequestParam("tempoMaoDeObraHoras") Double tempoMaoDeObraHoras,
            @RequestParam("custoMaquinaHora") BigDecimal custoMaquinaHora,
            @RequestParam("custoMaoDeObraHora") BigDecimal custoMaoDeObraHora,
            @RequestParam("margemLucro") BigDecimal margemLucro,
            @RequestParam(value = "objeto3D", required = false) MultipartFile objeto3D,
            @RequestParam(value = "imagensReferencia", required = false) MultipartFile[] imagensReferencia,
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

    @GetMapping
    public List<OrcamentoResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal());
    }

    @GetMapping("/{id}")
    public OrcamentoResponseDTO buscarPorId(@PathVariable String id, Authentication auth) {
        return service.buscarPorId(id, (String) auth.getPrincipal());
    }

    @PatchMapping("/{id}/aprovar")
    public OrcamentoResponseDTO aprovar(@PathVariable String id, Authentication auth) {
        return service.aprovar(id, (String) auth.getPrincipal());
    }

    @PatchMapping("/{id}/rejeitar")
    public OrcamentoResponseDTO rejeitar(@PathVariable String id, Authentication auth) {
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
}
