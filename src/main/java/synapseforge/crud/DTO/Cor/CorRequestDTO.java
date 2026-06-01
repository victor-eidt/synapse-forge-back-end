package synapseforge.crud.DTO.Cor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.Acabamento;

@Getter
@Setter
public class CorRequestDTO {

    @NotBlank
    private String nome;

    @NotBlank
    private String fornecedor;

    private String codigo;

    @NotBlank
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "O hex deve estar no formato #RRGGBB")
    private String hex;

    @NotNull
    private Acabamento acabamento;

    @NotNull
    @PositiveOrZero
    private Integer estoqueMl;

    @NotNull
    @PositiveOrZero
    private Integer estoqueMinimoMl;

    @NotNull
    @PositiveOrZero
    private Double custoMl;
}
