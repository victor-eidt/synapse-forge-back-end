package synapseforge.crud.DTO.Pedido;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusEnvio;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Dados de envio e rastreio do pedido")
public class PedidoEnvioRequestDTO {

    @Schema(description = "ID da forma de envio escolhida (GET /formas-envio); vazio limpa a forma", example = "6704a1c2e4b0f81a2c3d4e21")
    private String formaEnvioId;

    @Schema(description = "Código de rastreio do objeto; ao ser informado, o envio passa a POSTADO", example = "AA123456789BR")
    private String codigoRastreio;

    @Schema(description = "Data de postagem (yyyy-MM-dd); assume hoje quando o código é informado sem data", example = "2026-11-09")
    private LocalDate dataPostagem;

    @Schema(description = "Previsão de entrega (yyyy-MM-dd)", example = "2026-11-16")
    private LocalDate previsaoEntrega;

    @Schema(description = "Situação do envio; opcional, para marcar EM_TRANSITO ou ENTREGUE manualmente", example = "EM_TRANSITO")
    private StatusEnvio statusEnvio;
}
