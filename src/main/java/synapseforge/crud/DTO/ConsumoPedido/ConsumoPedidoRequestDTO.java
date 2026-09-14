package synapseforge.crud.DTO.ConsumoPedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConsumoPedidoRequestDTO {

    @NotBlank
    private String pedidoId;

    @NotEmpty
    @Valid
    private List<ItemConsumoRequestDTO> itens;
}
