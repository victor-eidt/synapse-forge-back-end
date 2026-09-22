package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "movimentos_estoque")
public class MovimentoEstoque {

    @Id
    private String id;

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private TipoMovimento tipo;

    // quantidade sempre registrada na unidade base do insumo (G, ML ou UN)
    private BigDecimal quantidade;
    private UnidadeMedida unidade;
    private BigDecimal saldoApos;

    // snapshot do custo no momento do movimento
    private BigDecimal custoUnitario;
    private BigDecimal custoTotal;

    private String pedidoId;
    private StatusPedido etapaOrigem;
    private String motivo;
    private String usuarioId;
    private LocalDateTime criadoEm;

    private String chaveIdempotencia;
}
