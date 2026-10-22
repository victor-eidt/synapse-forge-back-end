package synapseforge.crud.DTO.Comentario;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comentário registrado em um pedido")
public class ComentarioResponseDTO {

    @Schema(description = "ID do comentário", example = "6704a1c2e4b0f81a2c3d4e0a")
    private String id;

    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;

    @Schema(description = "ID de quem escreveu", example = "6704a1c2e4b0f81a2c3d4e05")
    private String usuarioId;

    @Schema(description = "Nome de quem escreveu", example = "Carlos Mendes")
    private String nomeUsuario;

    @Schema(description = "Texto do comentário", example = "Cliente pediu para escurecer um pouco o tom da base.")
    private String conteudo;

    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;

}