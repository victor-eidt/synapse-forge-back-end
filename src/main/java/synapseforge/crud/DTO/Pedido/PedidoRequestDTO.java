package synapseforge.crud.DTO.Pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@Setter
public class PedidoRequestDTO {

    @NotBlank
    private String cliente;

    @NotBlank
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

    @NotNull
    private LocalDate prazo;

    private StatusPedido status;
}
