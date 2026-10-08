package synapseforge.crud.DTO.Equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Criação ou renomeação de equipe")
public class EquipeRequestDTO {

    @NotBlank
    @Schema(description = "Nome da equipe (oficina)", example = "Oficina Synapse Curitiba")
    private String nome;
}