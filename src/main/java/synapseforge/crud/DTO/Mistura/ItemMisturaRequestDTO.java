package synapseforge.crud.DTO.Mistura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemMisturaRequestDTO {

    @NotBlank
    private String corId;

    @NotNull
    @Positive
    private Double proporcao;
}
