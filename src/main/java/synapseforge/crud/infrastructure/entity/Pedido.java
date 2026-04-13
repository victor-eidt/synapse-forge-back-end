package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;

    private String usuarioId;

    private String cliente;
    private String projeto;
    private String descricao;

    private StatusPedido status;
    private LocalDate prazo;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
