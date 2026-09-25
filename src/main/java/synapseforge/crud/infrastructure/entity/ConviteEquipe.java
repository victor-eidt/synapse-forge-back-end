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
@Document(collection = "convites_equipe")
public class ConviteEquipe {

    @Id
    private String id;

    private String equipeId;

    private String gerenteId;

    private String usuarioId;

    private String token;

    private StatusConviteEquipe status;

    private LocalDateTime criadoEm;

    private LocalDateTime expiraEm;

    private LocalDateTime respondidoEm;
}

