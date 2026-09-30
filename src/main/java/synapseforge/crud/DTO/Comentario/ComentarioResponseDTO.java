package synapseforge.crud.DTO.Comentario;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComentarioResponseDTO {

    private String id;

    private String pedidoId;

    private String usuarioId;

    private String nomeUsuario;

    private String conteudo;

    private LocalDateTime criadoEm;

}