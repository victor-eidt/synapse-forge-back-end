package synapseforge.crud.DTO.Orcamento;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import synapseforge.crud.infrastructure.entity.StatusOrcamento;

@Getter
@AllArgsConstructor
public class OrcamentoResponseDTO {

    private String id;
    private String materialId;
    private String nomeMaterial;
    private String cliente;
    private String projeto;
    private String descricao;
    private LocalDate prazo;
    private StatusOrcamento status;
    private String pedidoId;

    // Inputs
    private Double volumeCm3;
    private Double tempoImpressaoHoras;
    private Double tempoMaoDeObraHoras;
    private BigDecimal custoMaquinaHora;
    private BigDecimal custoMaoDeObraHora;
    private BigDecimal margemLucro;

    // Outputs
    private BigDecimal custoMaterial;
    private BigDecimal custoMaquina;
    private BigDecimal custoMaoDeObra;
    private BigDecimal custoTotal;
    private BigDecimal precoFinal;

    private LocalDateTime criadoEm;
}
