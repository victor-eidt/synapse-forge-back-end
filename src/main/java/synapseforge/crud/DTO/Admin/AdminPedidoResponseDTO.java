package synapseforge.crud.DTO.Admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminPedidoResponseDTO {

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

    private String status;

    private LocalDate prazo;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private String objeto3DFileId;
    private int quantidadeImagensReferencia;
}
