package synapseforge.crud.DTO.Mistura;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Receita de mistura de tintas")
public class MisturaRequestDTO {

    @NotBlank
    @Size(max = 60)
    @Schema(description = "Nome da mistura", example = "Laranja Pôr do Sol")
    private String nome;

    @NotEmpty
    @Valid
    @Schema(description = "Cores e proporções (somando 100%)")
    private List<ItemMisturaRequestDTO> itens;

    @NotNull
    @Positive
    @Schema(description = "Volume total da mistura, em mL", example = "200")
    private Integer volumeMl;
}
