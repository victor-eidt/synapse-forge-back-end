package synapseforge.crud.DTO.ConsumoPedido;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Ficha de consumo de insumos de um pedido, por etapa")
public class ConsumoPedidoRequestDTO {

    @NotBlank
    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;

    @NotEmpty
    @Valid
    @Schema(description = "Insumos consumidos e em qual etapa a baixa acontece")
    private List<ItemConsumoRequestDTO> itens;
}
