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
@Document(collection = "misturas")
public class Mistura {

    @Id
    private String id;

    // Equipe (oficina) dona do registro; sai sempre do usuário logado, nunca do request
    private String equipeId;

    // Quem cadastrou (metadado; o acesso é pela equipe)
    private String usuarioId;

    private String nome;
    private List<ItemMistura> itens;
    private Integer volumeMl;
    private String hexResultado;
    private Double custoEstimado;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
