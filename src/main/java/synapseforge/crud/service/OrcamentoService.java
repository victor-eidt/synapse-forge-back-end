package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final OrcamentoRepository repository;
    private final MaterialRepository materialRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * Calcula o orçamento sem persistir (útil para preview no front).
     */
    public OrcamentoResponseDTO calcular(CalcularOrcamentoRequestDTO dto) {
        Material material = buscarMaterialAtivo(dto.getMaterialId());
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
                null
        );
    }

    /**
     * Calcula e persiste o orçamento.
     */
    public OrcamentoResponseDTO salvar(CalcularOrcamentoRequestDTO dto) {
        return salvar(dto, null);
    }

    public OrcamentoResponseDTO salvar(CalcularOrcamentoRequestDTO dto, String usuarioId) {
        return salvar(dto, usuarioId, null, List.of());
    }

    public OrcamentoResponseDTO salvar(
            CalcularOrcamentoRequestDTO dto,
            String usuarioId,
            String objeto3DFileId,
            List<String> imagensReferenciaFileIds
    ) {
        OrcamentoResponseDTO calculado = calcular(dto);

        Orcamento orcamento = new Orcamento();
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

    public List<OrcamentoResponseDTO> listar() {
        return repository.findAllByOrderByCriadoEmDesc().stream()
                .map(orcamento -> toResponseDTO(orcamento, nomeMaterial(orcamento.getMaterialId())))
                .toList();
    }

    public List<OrcamentoResponseDTO> listar(String usuarioId) {
        return repository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(orcamento -> toResponseDTO(orcamento, nomeMaterial(orcamento.getMaterialId())))
                .toList();
    }

    public OrcamentoResponseDTO buscarPorId(String id) {
        Orcamento orcamento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));
        return toResponseDTO(orcamento, nomeMaterial(orcamento.getMaterialId()));
    }

    public OrcamentoResponseDTO buscarPorId(String id, String usuarioId) {
        Orcamento orcamento = buscarDoUsuario(id, usuarioId);
        return toResponseDTO(orcamento, nomeMaterial(orcamento.getMaterialId()));
    }

    public OrcamentoResponseDTO aprovar(String id, String usuarioId) {
        Orcamento orcamento = buscarDoUsuario(id, usuarioId);
        if (status(orcamento) != StatusOrcamento.PENDENTE) {
            throw new IllegalStateException("Orcamento ja foi decidido");
        }

        Pedido pedido = new Pedido();
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
        return toResponseDTO(repository.save(orcamento), nomeMaterial(orcamento.getMaterialId()));
    }

    public OrcamentoResponseDTO rejeitar(String id, String usuarioId) {
        Orcamento orcamento = buscarDoUsuario(id, usuarioId);
        if (status(orcamento) != StatusOrcamento.PENDENTE) {
            throw new IllegalStateException("Orcamento ja foi decidido");
        }
        orcamento.setStatus(StatusOrcamento.REJEITADO);
        return toResponseDTO(repository.save(orcamento), nomeMaterial(orcamento.getMaterialId()));
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

    private Material buscarMaterialAtivo(String materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material não encontrado"));
        if (!Boolean.TRUE.equals(material.getAtivo())) {
            throw new RuntimeException("Material inativo");
        }
        return material;
    }

    private String nomeMaterial(String materialId) {
        return materialRepository.findById(materialId)
                .map(Material::getNome)
                .orElse(null);
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private OrcamentoResponseDTO toResponseDTO(Orcamento orcamento, String nomeMaterial) {
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
                orcamento.getCriadoEm()
        );
    }

    private Orcamento buscarDoUsuario(String id, String usuarioId) {
        return repository.findById(id)
                .filter(orcamento -> usuarioId.equals(orcamento.getUsuarioId()))
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
