package synapseforge.crud.DTO.ConsumoPedido;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ConsumoPedidoResponseDTO {

    private String id;
    private String pedidoId;
    private List<ItemConsumoResponseDTO> itens;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
