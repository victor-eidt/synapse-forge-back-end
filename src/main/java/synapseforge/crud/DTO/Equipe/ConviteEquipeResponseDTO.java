package synapseforge.crud.DTO.Equipe;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusConviteEquipe;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConviteEquipeResponseDTO {

    private String id;
    private String equipeId;
    private String equipeNome;
    private String gerenteId;
    private String gerenteNome;
    private String usuarioId;
    private String usuarioNome;
    private StatusConviteEquipe status;
    private LocalDateTime criadoEm;
    private LocalDateTime expiraEm;
}