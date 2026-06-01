package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    // GridFS id for uploaded 3D object
    private String objeto3DFileId;

    // GridFS ids for reference images
    private List<String> imagensReferenciaFileIds;
}
