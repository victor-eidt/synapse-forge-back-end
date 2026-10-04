package synapseforge.crud.DTO.Notificacao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificacaoResponseDTO {

    private String id;
    private TipoNotificacao tipo;
    private String referenciaId;
    private String titulo;
    private String detalhe;
    private boolean lida;
    private LocalDateTime criadaEm;
}
