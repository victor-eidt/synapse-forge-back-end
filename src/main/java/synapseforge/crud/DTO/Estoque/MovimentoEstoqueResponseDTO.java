package synapseforge.crud.DTO.Estoque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.TipoMovimento;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MovimentoEstoqueResponseDTO {

    private String id;
    private TipoInsumo tipoInsumo;
    private String insumoId;
    private TipoMovimento tipo;
    private BigDecimal quantidade;
    private UnidadeMedida unidade;
    private BigDecimal saldoApos;
    private BigDecimal custoUnitario;
    private BigDecimal custoTotal;
    private String pedidoId;
    private StatusPedido etapaOrigem;
    private String motivo;
    private String usuarioId;
    private LocalDateTime criadoEm;
}
