package synapseforge.crud.DTO.FormaEnvio;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Forma de envio disponível para os pedidos")
public class FormaEnvioResponseDTO {

    @Schema(description = "ID da forma de envio", example = "6704a1c2e4b0f81a2c3d4e21")
    private String id;
    @Schema(description = "Nome da forma de envio", example = "Correios PAC")
    private String nome;
    @Schema(description = "Transportadora responsável", example = "Correios")
    private String transportadora;
    @Schema(description = "Modelo da URL de rastreio; {codigo} é trocado pelo código do objeto",
            example = "https://rastreamento.correios.com.br/app/index.php?objeto={codigo}")
    private String urlRastreioTemplate;
    @Schema(description = "true para forma padrão do sistema; false para forma cadastrada pela equipe", example = "true")
    private boolean padrao;
    @Schema(description = "Forma disponível para novos envios", example = "true")
    private Boolean ativo;
}
