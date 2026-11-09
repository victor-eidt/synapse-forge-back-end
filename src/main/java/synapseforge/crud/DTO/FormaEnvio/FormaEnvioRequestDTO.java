package synapseforge.crud.DTO.FormaEnvio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Cadastro de forma de envio da equipe")
public class FormaEnvioRequestDTO {

    @NotBlank
    @Schema(description = "Nome da forma de envio", example = "Correios PAC")
    private String nome;

    @Schema(description = "Transportadora responsável", example = "Correios")
    private String transportadora;

    @Schema(description = "Modelo da URL de rastreio; {codigo} é trocado pelo código do objeto",
            example = "https://rastreamento.correios.com.br/app/index.php?objeto={codigo}")
    private String urlRastreioTemplate;
}
