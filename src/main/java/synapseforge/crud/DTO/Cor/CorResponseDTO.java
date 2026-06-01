package synapseforge.crud.DTO.Cor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.Acabamento;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CorResponseDTO {

    private String id;
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
