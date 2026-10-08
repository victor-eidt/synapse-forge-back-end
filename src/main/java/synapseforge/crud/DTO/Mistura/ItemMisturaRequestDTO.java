package synapseforge.crud.DTO.Mistura;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Cor que entra na mistura e sua proporção")
public class ItemMisturaRequestDTO {

    @NotBlank
    @Schema(description = "ID da cor (tinta)", example = "6704a1c2e4b0f81a2c3d4e04")
    private String corId;

    @NotNull
    @Positive
    @Schema(description = "Proporção da cor na mistura, em % (a soma dos itens deve ser 100)", example = "60")
    private Double proporcao;
}
