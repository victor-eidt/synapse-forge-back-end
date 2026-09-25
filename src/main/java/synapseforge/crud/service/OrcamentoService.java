package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import com.mongodb.client.gridfs.model.GridFSFile;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Orcamento;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusOrcamento;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.OrcamentoRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// Orçamentos são da equipe (o acesso por perfil GERENTE fica no controller).
@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final OrcamentoRepository repository;
    private final MaterialRepository materialRepository;
    private final PedidoRepository pedidoRepository;
    private final GridFsTemplate gridFsTemplate;
    private final EquipeContexto equipeContexto;

    /**
     * Calcula o orçamento sem persistir (útil para preview no front).
     * O material precisa ser da equipe do usuário.
     */
    public OrcamentoResponseDTO calcular(CalcularOrcamentoRequestDTO dto, String usuarioId) {
        return calcularNaEquipe(dto, equipeContexto.equipeObrigatoria(usuarioId));
    }

    private OrcamentoResponseDTO calcularNaEquipe(CalcularOrcamentoRequestDTO dto, String equipeId) {
        Material material = buscarMaterialAtivo(dto.getMaterialId(), equipeId);
        ResultadoCalculo resultado = aplicarFormula(material, dto);

        return new OrcamentoResponseDTO(
                null,
                dto.getMaterialId(),
                material.getNome(),
                dto.getCliente(),
                dto.getProjeto(),
                dto.getDescricao(),
                dto.getPrazo(),
                StatusOrcamento.PENDENTE,
                null,
                dto.getVolumeCm3(),
                dto.getTempoImpressaoHoras(),
                dto.getTempoMaoDeObraHoras(),
                dto.getCustoMaquinaHora(),
                dto.getCustoMaoDeObraHora(),
                dto.getMargemLucro(),
                resultado.custoMaterial(),
                resultado.custoMaquina(),
                resultado.custoMaoDeObra(),
                resultado.custoTotal(),
                resultado.precoFinal(),
                null,
                null,
                List.of(),
                List.of()
        );
    }

    /**
     * Calcula e persiste o orçamento na equipe do usuário.
     */
    public OrcamentoResponseDTO salvar(CalcularOrcamentoRequestDTO dto, String usuarioId) {
        return salvar(dto, usuarioId, null, List.of());
    }

    public OrcamentoResponseDTO salvar(
            CalcularOrcamentoRequestDTO dto,
            String usuarioId,
            String objeto3DFileId,
            List<String> imagensReferenciaFileIds
    ) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        OrcamentoResponseDTO calculado = calcularNaEquipe(dto, equipeId);

        Orcamento orcamento = new Orcamento();
        orcamento.setEquipeId(equipeId);
        orcamento.setMaterialId(calculado.getMaterialId());
        orcamento.setUsuarioId(usuarioId);
        orcamento.setCliente(calculado.getCliente());
        orcamento.setProjeto(calculado.getProjeto());
        orcamento.setDescricao(calculado.getDescricao());
        orcamento.setPrazo(calculado.getPrazo());
        orcamento.setStatus(StatusOrcamento.PENDENTE);
        orcamento.setVolumeCm3(calculado.getVolumeCm3());
        orcamento.setTempoImpressaoHoras(calculado.getTempoImpressaoHoras());
        orcamento.setTempoMaoDeObraHoras(calculado.getTempoMaoDeObraHoras());
        orcamento.setCustoMaquinaHora(calculado.getCustoMaquinaHora());
        orcamento.setCustoMaoDeObraHora(calculado.getCustoMaoDeObraHora());
        orcamento.setMargemLucro(calculado.getMargemLucro());
        orcamento.setCustoMaterial(calculado.getCustoMaterial());
        orcamento.setCustoMaquina(calculado.getCustoMaquina());
        orcamento.setCustoMaoDeObra(calculado.getCustoMaoDeObra());
        orcamento.setCustoTotal(calculado.getCustoTotal());
        orcamento.setPrecoFinal(calculado.getPrecoFinal());
        orcamento.setCriadoEm(LocalDateTime.now());
        orcamento.setObjeto3DFileId(objeto3DFileId);
        orcamento.setImagensReferenciaFileIds(imagensReferenciaFileIds == null
                ? List.of()
                : List.copyOf(imagensReferenciaFileIds));

        Orcamento salvo = repository.save(orcamento);
        return toResponseDTO(salvo, calculado.getNomeMaterial());
    }

    public List<OrcamentoResponseDTO> listar(String usuarioId) {
        return equipeContexto.equipeDe(usuarioId)
                .map(repository::findByEquipeIdOrderByCriadoEmDesc)
                .orElse(List.of())
                .stream()
                .map(orcamento -> toResponseDTO(orcamento, nomeMaterial(orcamento)))
                .toList();
    }

    public OrcamentoResponseDTO buscarPorId(String id, String usuarioId) {
        // leitura de um registro: sem equipe, simplesmente não existe
        Orcamento orcamento = equipeContexto.equipeDe(usuarioId)
                .flatMap(equipeId -> repository.findByIdAndEquipeId(id, equipeId))
                .orElseThrow(() -> new RuntimeException("Orcamento nao encontrado"));
        return toResponseDTO(orcamento, nomeMaterial(orcamento), true);
    }

    public OrcamentoResponseDTO aprovar(String id, String usuarioId) {
        Orcamento orcamento = buscarDaEquipe(id, usuarioId);
        if (status(orcamento) != StatusOrcamento.PENDENTE) {
            throw new IllegalStateException("Orcamento ja foi decidido");
        }

        // o pedido nasce na mesma equipe do orçamento aprovado
        Pedido pedido = new Pedido();
        pedido.setEquipeId(orcamento.getEquipeId());
        pedido.setUsuarioId(usuarioId);
        pedido.setCliente(orcamento.getCliente());
        pedido.setProjeto(orcamento.getProjeto());
        pedido.setDescricao(orcamento.getDescricao());
        pedido.setPrazo(orcamento.getPrazo());
        pedido.setStatus(StatusPedido.MODELAGEM);
        pedido.setOrcamentoId(orcamento.getId());
        pedido.setMaterialId(orcamento.getMaterialId());
        pedido.setVolumeCm3(orcamento.getVolumeCm3());
        pedido.setTempoImpressaoHoras(orcamento.getTempoImpressaoHoras());
        pedido.setTempoMaoDeObraHoras(orcamento.getTempoMaoDeObraHoras());
        pedido.setCustoMaquinaHora(orcamento.getCustoMaquinaHora());
        pedido.setCustoMaoDeObraHora(orcamento.getCustoMaoDeObraHora());
        pedido.setMargemLucro(orcamento.getMargemLucro());
        pedido.setCustoMaterial(orcamento.getCustoMaterial());
        pedido.setCustoMaquina(orcamento.getCustoMaquina());
        pedido.setCustoMaoDeObra(orcamento.getCustoMaoDeObra());
        pedido.setCustoTotal(orcamento.getCustoTotal());
        pedido.setPrecoFinal(orcamento.getPrecoFinal());
        pedido.setObjeto3DFileId(orcamento.getObjeto3DFileId());
        pedido.setImagensReferenciaFileIds(orcamento.getImagensReferenciaFileIds() == null
                ? List.of()
                : List.copyOf(orcamento.getImagensReferenciaFileIds()));
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setAtualizadoEm(LocalDateTime.now());

        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        orcamento.setStatus(StatusOrcamento.APROVADO);
        orcamento.setPedidoId(pedidoSalvo.getId());
        return toResponseDTO(repository.save(orcamento), nomeMaterial(orcamento));
    }

    public OrcamentoResponseDTO rejeitar(String id, String usuarioId) {
        Orcamento orcamento = buscarDaEquipe(id, usuarioId);
        if (status(orcamento) != StatusOrcamento.PENDENTE) {
            throw new IllegalStateException("Orcamento ja foi decidido");
        }
        orcamento.setStatus(StatusOrcamento.REJEITADO);
        return toResponseDTO(repository.save(orcamento), nomeMaterial(orcamento));
    }

    private ResultadoCalculo aplicarFormula(Material material, CalcularOrcamentoRequestDTO dto) {
        BigDecimal massaGramas = BigDecimal.valueOf(dto.getVolumeCm3() * material.getDensidadeGcm3());
        BigDecimal custoMaterial = material.getPrecoPorGrama().multiply(massaGramas);
        BigDecimal custoMaquina = dto.getCustoMaquinaHora().multiply(BigDecimal.valueOf(dto.getTempoImpressaoHoras()));
        BigDecimal custoMaoDeObra = dto.getCustoMaoDeObraHora().multiply(BigDecimal.valueOf(dto.getTempoMaoDeObraHoras()));
        BigDecimal custoTotal = custoMaterial.add(custoMaquina).add(custoMaoDeObra);

        BigDecimal fator = BigDecimal.ONE.add(dto.getMargemLucro().divide(CEM, 10, RoundingMode.HALF_UP));
        BigDecimal precoFinal = custoTotal.multiply(fator);

        return new ResultadoCalculo(
                escala(custoMaterial),
                escala(custoMaquina),
                escala(custoMaoDeObra),
                escala(custoTotal),
                escala(precoFinal)
        );
    }

    private Material buscarMaterialAtivo(String materialId, String equipeId) {
        Material material = materialRepository.findByIdAndEquipeId(materialId, equipeId)
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));
        if (!Boolean.TRUE.equals(material.getAtivo())) {
            throw new RuntimeException("Material inativo");
        }
        return material;
    }

    private String nomeMaterial(Orcamento orcamento) {
        return materialRepository.findByIdAndEquipeId(orcamento.getMaterialId(), orcamento.getEquipeId())
                .map(Material::getNome)
                .orElse(null);
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private OrcamentoResponseDTO toResponseDTO(Orcamento orcamento, String nomeMaterial) {
        return toResponseDTO(orcamento, nomeMaterial, false);
    }

    /**
     * As imagens de referencia so sao codificadas em base64 na leitura de um orcamento
     * unico: embutir todas elas na listagem carregaria o GridFS inteiro em memoria.
     */
    private OrcamentoResponseDTO toResponseDTO(
            Orcamento orcamento,
            String nomeMaterial,
            boolean incluirImagens
    ) {
        return new OrcamentoResponseDTO(
                orcamento.getId(),
                orcamento.getMaterialId(),
                nomeMaterial,
                orcamento.getCliente(),
                orcamento.getProjeto(),
                orcamento.getDescricao(),
                orcamento.getPrazo(),
                status(orcamento),
                orcamento.getPedidoId(),
                orcamento.getVolumeCm3(),
                orcamento.getTempoImpressaoHoras(),
                orcamento.getTempoMaoDeObraHoras(),
                orcamento.getCustoMaquinaHora(),
                orcamento.getCustoMaoDeObraHora(),
                orcamento.getMargemLucro(),
                orcamento.getCustoMaterial(),
                orcamento.getCustoMaquina(),
                orcamento.getCustoMaoDeObra(),
                orcamento.getCustoTotal(),
                orcamento.getPrecoFinal(),
                orcamento.getCriadoEm(),
                orcamento.getObjeto3DFileId(),
                incluirImagens
                        ? imagensReferenciaBase64(orcamento.getImagensReferenciaFileIds())
                        : List.of(),
                orcamento.getImagensReferenciaFileIds() == null
                        ? List.of()
                        : List.copyOf(orcamento.getImagensReferenciaFileIds())
        );
    }

    private List<String> imagensReferenciaBase64(List<String> fileIds) {
        if (fileIds == null) {
            return List.of();
        }

        List<String> imagens = new ArrayList<>();
        for (String fileId : fileIds) {
            if (!ObjectId.isValid(fileId)) {
                imagens.add(null);
                continue;
            }

            try {
                GridFSFile file = gridFsTemplate.findOne(
                        Query.query(Criteria.where("_id").is(new ObjectId(fileId)))
                );
                if (file == null) {
                    imagens.add(null);
                    continue;
                }

                GridFsResource resource = gridFsTemplate.getResource(file);
                byte[] bytes;
                try (var inputStream = resource.getInputStream()) {
                    bytes = inputStream.readAllBytes();
                }

                imagens.add(
                        "data:" + ArquivoUtils.contentType(file) + ";base64,"
                                + Base64.getEncoder().encodeToString(bytes)
                );
            } catch (IOException e) {
                imagens.add(null);
            }
        }
        return imagens;
    }

    // escrita: sem equipe -> 403 SEM_EQUIPE; orçamento de outra equipe -> não encontrado
    private Orcamento buscarDaEquipe(String id, String usuarioId) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        return repository.findByIdAndEquipeId(id, equipeId)
                .orElseThrow(() -> new RuntimeException("Orcamento nao encontrado"));
    }

    private StatusOrcamento status(Orcamento orcamento) {
        return orcamento.getStatus() == null ? StatusOrcamento.PENDENTE : orcamento.getStatus();
    }

    private record ResultadoCalculo(
            BigDecimal custoMaterial,
            BigDecimal custoMaquina,
            BigDecimal custoMaoDeObra,
            BigDecimal custoTotal,
            BigDecimal precoFinal
    ) {}
}
