package synapseforge.crud.DTO.Mistura;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MisturaResponseDTO {

    private String id;
    private String nome;
    private List<ItemMisturaResponseDTO> itens;
    private Integer volumeMl;
    private String hexResultado;
    private Double custoEstimado;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
