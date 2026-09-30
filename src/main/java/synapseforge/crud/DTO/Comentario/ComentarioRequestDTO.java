package synapseforge.crud.DTO.Comentario;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComentarioRequestDTO {

    @NotBlank
    private String conteudo;
}