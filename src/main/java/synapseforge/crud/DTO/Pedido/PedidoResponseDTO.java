package synapseforge.crud.DTO.Pedido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PedidoResponseDTO {

    private String id;
    private String clienteId;
    private String cliente;
    private String projeto;
    private String descricao;
    private String orcamentoId;
    private String materialId;
    private Double volumeCm3;
    private Double tempoImpressaoHoras;
    private Double tempoMaoDeObraHoras;
    private BigDecimal custoMaquinaHora;
    private BigDecimal custoMaoDeObraHora;
    private BigDecimal margemLucro;
    private BigDecimal custoMaterial;
    private BigDecimal custoMaquina;
    private BigDecimal custoMaoDeObra;
    private BigDecimal custoTotal;
    private BigDecimal precoFinal;
    private StatusPedido status;
    private LocalDate prazo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // GridFS ids for uploaded files
    private String objeto3DFileId;
    private List<String> imagensReferenciaFileIds;
    private List<String> imagensReferenciaIds;
}