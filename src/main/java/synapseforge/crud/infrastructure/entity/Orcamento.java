package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orcamentos")
public class Orcamento {

    @Id
    private String id;

    private String materialId;

    // Dados comerciais compartilhados com o pedido.
    private String usuarioId;
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
    private String objeto3DFileId;

    private List<String> imagensReferenciaFileIds;
}
