package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Movimentação de estoque (entrada, baixa, estorno ou ajuste)")
public class MovimentoEstoqueResponseDTO {

    @Schema(description = "ID da movimentação", example = "6704a1c2e4b0f81a2c3d4e0e")
    private String id;
    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;
    @Schema(description = "Tipo da movimentação", example = "BAIXA")
    private TipoMovimento tipo;
    @Schema(description = "Quantidade movimentada", example = "106.020")
    private BigDecimal quantidade;
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;
    @Schema(description = "Saldo do insumo depois da movimentação", example = "1143.980")
    private BigDecimal saldoApos;
    @Schema(description = "Custo por unidade, em R$", example = "0.12")
    private BigDecimal custoUnitario;
    @Schema(description = "Custo da movimentação, em R$", example = "12.72")
    private BigDecimal custoTotal;
    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;
    @Schema(description = "Etapa do pedido que gerou a baixa (quando houver)", example = "IMPRESSAO")
    private StatusPedido etapaOrigem;
    @Schema(description = "Motivo informado (entradas e ajustes)", example = "Baixa automática ao avançar para Impressão")
    private String motivo;
    @Schema(description = "ID de quem fez a movimentação", example = "6704a1c2e4b0f81a2c3d4e05")
    private String usuarioId;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
}
