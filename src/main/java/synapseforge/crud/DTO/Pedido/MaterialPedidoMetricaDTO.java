package synapseforge.crud.DTO.Pedido;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// mapeado diretamente do resultado da aggregation, por isso precisa de setters e construtor vazio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Material e quanto ele aparece nos pedidos da equipe")
public class MaterialPedidoMetricaDTO {

    @Schema(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03")
    private String materialId;
    @Schema(description = "Nome do material (null se o material foi excluído)", example = "PLA Branco")
    private String nome;
    @Schema(description = "Quantidade de pedidos que usam o material", example = "12")
    private long quantidadePedidos;
    @Schema(description = "Soma do preço final desses pedidos, em R$", example = "2480.00")
    private BigDecimal valorTotal;
}
