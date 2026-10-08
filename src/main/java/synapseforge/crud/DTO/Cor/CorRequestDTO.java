package synapseforge.crud.DTO.Cor;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.Acabamento;

@Getter
@Setter
@Schema(description = "Cadastro ou edição de uma cor (tinta) da paleta")
public class CorRequestDTO {

    @NotBlank
    @Schema(description = "Nome da cor", example = "Vermelho Queimado")
    private String nome;

    @NotBlank
    @Schema(description = "Fornecedor da tinta", example = "Coral Tintas")
    private String fornecedor;

    @Schema(description = "Código da tinta no fornecedor", example = "CT-204")
    private String codigo;

    @NotBlank
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "O hex deve estar no formato #RRGGBB")
    @Schema(description = "Cor em hexadecimal (#RRGGBB)", example = "#963A28")
    private String hex;

    @NotNull
    @Schema(description = "Acabamento da tinta", example = "FOSCO")
    private Acabamento acabamento;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Estoque atual da tinta, em mL", example = "450")
    private Integer estoqueMl;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Estoque mínimo da tinta, em mL", example = "200")
    private Integer estoqueMinimoMl;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Custo da tinta por mL, em R$", example = "0.28")
    private Double custoMl;
}
