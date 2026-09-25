package synapseforge.crud.DTO.Equipe;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipeRequestDTO {

    @NotBlank
    private String nome;
}