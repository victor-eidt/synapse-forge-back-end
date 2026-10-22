package synapseforge.crud.DTO.Orcamento;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Dados para calcular (ou salvar) um orçamento de impressão")
public class CalcularOrcamentoRequestDTO {

    @NotBlank
    @Schema(description = "Nome do cliente", example = "Mariana Costa")
    private String cliente;

    @NotBlank
    @Schema(description = "Nome do projeto", example = "Miniatura do Dragão Vermelho")
    private String projeto;

    @Schema(description = "Descrição do projeto", example = "Miniatura em escala 1:24 para RPG, com base texturizada e pintura em degradê.")
    private String descricao;

    @NotNull
    @Schema(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20")
    private LocalDate prazo;

    @NotBlank
    @Schema(description = "ID do material de impressão", example = "6704a1c2e4b0f81a2c3d4e03")
    private String materialId;

    @NotNull
    @Positive
    @Schema(description = "Volume da peça, em cm³", example = "85.5")
    private Double volumeCm3;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Tempo estimado de impressão, em horas", example = "6.5")
    private Double tempoImpressaoHoras;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Tempo de mão de obra (pós-processamento e pintura), em horas", example = "2.0")
    private Double tempoMaoDeObraHoras;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Custo da impressora por hora, em R$", example = "4.50")
    private BigDecimal custoMaquinaHora;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Custo da mão de obra por hora, em R$", example = "35.00")
    private BigDecimal custoMaoDeObraHora;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Margem de lucro, em %", example = "30")
    private BigDecimal margemLucro;
}
