package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consumos_pedido")
public class ConsumoPedido {

    @Id
    private String id;

    private String pedidoId;
    private List<ItemConsumo> itens;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
