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
@Document(collection = "equipes")
public class Equipe {

    @Id
    private String id;

    // Nome da equipe
    private String nome;

    // Gerente responsável pela equipe
    private String gerenteId;

    // Arquivos armazenados no MongoDB GridFS
    private String fotoFileId;
    private String bannerFileId;

    // Datas
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}