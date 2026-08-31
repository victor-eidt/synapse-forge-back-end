package synapseforge.crud.DTO.Orcamento;

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
public class CalcularOrcamentoRequestDTO {

    @NotBlank
    private String cliente;

    @NotBlank
    private String projeto;

    private String descricao;

    @NotNull
    private LocalDate prazo;

    @NotBlank
    private String materialId;

    @NotNull
    @Positive
    private Double volumeCm3;

    @NotNull
    @PositiveOrZero
    private Double tempoImpressaoHoras;

    @NotNull
    @PositiveOrZero
    private Double tempoMaoDeObraHoras;

    @NotNull
    @PositiveOrZero
    private BigDecimal custoMaquinaHora;

    @NotNull
    @PositiveOrZero
    private BigDecimal custoMaoDeObraHora;

    @NotNull
    @PositiveOrZero
    private BigDecimal margemLucro;
}
