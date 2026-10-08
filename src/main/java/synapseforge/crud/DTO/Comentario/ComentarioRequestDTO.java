package synapseforge.crud.DTO.Comentario;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Texto de um comentário no pedido")
public class ComentarioRequestDTO {

    @NotBlank
    @Schema(description = "Texto do comentário", example = "Cliente pediu para escurecer um pouco o tom da base.")
    private String conteudo;
}