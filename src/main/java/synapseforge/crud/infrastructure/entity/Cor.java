package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cores")
public class Cor {

    @Id
    private String id;

    // Equipe (oficina) dona do registro; sai sempre do usuário logado, nunca do request
    private String equipeId;

    // Quem cadastrou (metadado; o acesso é pela equipe)
    private String usuarioId;

    private String nome;
    private String fornecedor;
    private String codigo;
    private String hex;

    private Acabamento acabamento;

    private Integer estoqueMl;
    private Integer estoqueMinimoMl;
    private Double custoMl;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
