package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Orcamento;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.OrcamentoRepository;

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
        OrcamentoResponseDTO calculado = calcular(dto);

        Orcamento orcamento = new Orcamento();
        orcamento.setMaterialId(calculado.getMaterialId());
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

        Orcamento salvo = repository.save(orcamento);
        return toResponseDTO(salvo, calculado.getNomeMaterial());
    }

    public List<OrcamentoResponseDTO> listar() {
        return repository.findAllByOrderByCriadoEmDesc().stream()
                .map(orcamento -> toResponseDTO(orcamento, nomeMaterial(orcamento.getMaterialId())))
                .toList();
    }

    public OrcamentoResponseDTO buscarPorId(String id) {
        Orcamento orcamento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));
        return toResponseDTO(orcamento, nomeMaterial(orcamento.getMaterialId()));
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

    private record ResultadoCalculo(
            BigDecimal custoMaterial,
            BigDecimal custoMaquina,
            BigDecimal custoMaoDeObra,
            BigDecimal custoTotal,
            BigDecimal precoFinal
    ) {}
}
