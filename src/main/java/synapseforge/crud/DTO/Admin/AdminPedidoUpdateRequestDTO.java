package synapseforge.crud.DTO.Admin;

import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AdminPedidoUpdateRequestDTO {

    private String clienteId;

    private String cliente;

    private String projeto;

    private String descricao;

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
}
