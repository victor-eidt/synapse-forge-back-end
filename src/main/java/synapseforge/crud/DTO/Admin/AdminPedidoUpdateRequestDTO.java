package synapseforge.crud.DTO.Admin;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Edição parcial de pedido pelo admin: campos omitidos (null) não são alterados")
public class AdminPedidoUpdateRequestDTO {

    @Schema(description = "ID do usuário cliente vinculado ao pedido", example = "6704a1c2e4b0f81a2c3d4e02")
    private String clienteId;

    @Schema(description = "Nome do cliente", example = "Mariana Costa")
    private String cliente;

    @Schema(description = "Nome do projeto", example = "Miniatura do Dragão Vermelho")
    private String projeto;

    @Schema(description = "Descrição do projeto", example = "Miniatura em escala 1:24 para RPG, com base texturizada e pintura em degradê.")
    private String descricao;

    @Schema(description = "ID do material de impressão", example = "6704a1c2e4b0f81a2c3d4e03")
    private String materialId;

    @Schema(description = "Volume da peça, em cm³", example = "85.5")
    private Double volumeCm3;

    @Schema(description = "Tempo estimado de impressão, em horas", example = "6.5")
    private Double tempoImpressaoHoras;

    @Schema(description = "Tempo de mão de obra (pós-processamento e pintura), em horas", example = "2.0")
    private Double tempoMaoDeObraHoras;

    @Schema(description = "Custo da impressora por hora, em R$", example = "4.50")
    private BigDecimal custoMaquinaHora;

    @Schema(description = "Custo da mão de obra por hora, em R$", example = "35.00")
    private BigDecimal custoMaoDeObraHora;

    @Schema(description = "Margem de lucro, em %", example = "30")
    private BigDecimal margemLucro;

    @Schema(description = "Custo do material, em R$ (volume × densidade × preço por grama)", example = "12.72")
    private BigDecimal custoMaterial;

    @Schema(description = "Custo de máquina, em R$", example = "29.25")
    private BigDecimal custoMaquina;

    @Schema(description = "Custo de mão de obra, em R$", example = "70.00")
    private BigDecimal custoMaoDeObra;

    @Schema(description = "Custo total, em R$", example = "111.97")
    private BigDecimal custoTotal;

    @Schema(description = "Preço final com a margem aplicada, em R$", example = "145.56")
    private BigDecimal precoFinal;

    @Schema(description = "Nova etapa do pedido; se mudar, o cliente é notificado", example = "PINTURA")
    private StatusPedido status;

    @Schema(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20")
    private LocalDate prazo;
}
