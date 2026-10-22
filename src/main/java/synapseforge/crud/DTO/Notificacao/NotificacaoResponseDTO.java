package synapseforge.crud.DTO.Notificacao;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "Aviso do sino do usuário logado")
public class NotificacaoResponseDTO {

    @Schema(description = "ID da notificação", example = "6704a1c2e4b0f81a2c3d4e09")
    private String id;
    @Schema(description = "Tipo do aviso", example = "PEDIDO_ETAPA_ALTERADA")
    private TipoNotificacao tipo;
    @Schema(description = "ID do registro de origem (pedido ou ordem de pintura)", example = "6704a1c2e4b0f81a2c3d4e01")
    private String referenciaId;
    @Schema(description = "Texto principal (nome do projeto)", example = "Miniatura do Dragão Vermelho")
    private String titulo;
    @Schema(description = "Complemento; no aviso de etapa é a etapa nova", example = "PINTURA")
    private String detalhe;
    @Schema(description = "Indica se o aviso já foi lido", example = "false")
    private boolean lida;
    @Schema(description = "Data e hora do aviso", example = "2026-10-08T09:15:42")
    private LocalDateTime criadaEm;
}
