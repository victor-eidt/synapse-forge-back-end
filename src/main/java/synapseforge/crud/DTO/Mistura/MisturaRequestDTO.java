package synapseforge.crud.DTO.Mistura;

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
public class MisturaRequestDTO {

    @NotBlank
    @Size(max = 60)
    private String nome;

    @NotEmpty
    @Valid
    private List<ItemMisturaRequestDTO> itens;

    @NotNull
    @Positive
    private Integer volumeMl;
}
