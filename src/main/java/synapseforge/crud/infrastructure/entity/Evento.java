package synapseforge.crud.infrastructure.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Document(collection = "eventos")
public class Evento {

    @Id
    private String id;
    private String userId;
    private String nome;
    private String data;
    private String descricao;
    private String horarioInicio;
    private String horarioFim;
    private List<String> participantes;

}

