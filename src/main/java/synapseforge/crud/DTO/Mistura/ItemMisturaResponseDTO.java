package synapseforge.crud.DTO.Mistura;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemMisturaResponseDTO {

    private String corId;
    private String nome;
    private String fornecedor;
    private String hex;
    private Double proporcao;
    private Integer volumeMl;
    private Double custo;
}
