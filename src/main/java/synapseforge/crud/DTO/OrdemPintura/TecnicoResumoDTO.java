package synapseforge.crud.DTO.OrdemPintura;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Opção do select de técnico na ordem de pintura: só o necessário para exibir e escolher.
@Getter
@AllArgsConstructor
public class TecnicoResumoDTO {
    private String id;
    private String nome;
}
