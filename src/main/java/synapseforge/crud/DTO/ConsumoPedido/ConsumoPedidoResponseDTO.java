package synapseforge.crud.DTO.ConsumoPedido;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Ficha de consumo de insumos de um pedido")
public class ConsumoPedidoResponseDTO {

    @Schema(description = "ID da ficha de consumo", example = "6704a1c2e4b0f81a2c3d4e0f")
    private String id;
    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;
    @Schema(description = "Insumos consumidos por etapa")
    private List<ItemConsumoResponseDTO> itens;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora da última atualização", example = "2026-10-08T09:15:42")
    private LocalDateTime atualizadoEm;
}
