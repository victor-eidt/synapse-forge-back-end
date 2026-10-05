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
@Document(collection = "ordens_pintura")
public class OrdemPintura {

    @Id
    private String id;

    // Equipe (oficina) dona do registro; sai sempre do usuário logado, nunca do request
    private String equipeId;

    // Quem cadastrou (metadado; o acesso é pela equipe)
    private String usuarioId;
    private String pedidoId;
    private String corId;
    // Responsável: técnico ativo ou o gerente da mesma equipe (validado no service).
    private String tecnicoId;
    // Nome do técnico no momento da gravação; ordens antigas só têm este campo.
    private String tecnico;
    private PrioridadeOrdemPintura prioridade;
    private LocalDate prazo;
    private EtapaOrdemPintura etapa;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
