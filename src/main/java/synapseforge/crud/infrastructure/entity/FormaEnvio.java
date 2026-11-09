package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Forma de envio disponível para os pedidos (Correios PAC, SEDEX, transportadora, retirada).
 * Tabela auxiliar: o pedido guarda só o id da forma escolhida ({@code Pedido.formaEnvioId}).
 * <p>
 * equipeId nulo = forma padrão do sistema, visível para todas as oficinas; preenchido =
 * forma cadastrada por uma equipe, visível só para ela.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "formas_envio")
public class FormaEnvio {

    public static final String MARCADOR_CODIGO = "{codigo}";

    @Id
    private String id;

    private String equipeId;

    private String nome;
    private String transportadora;

    // Ex.: https://rastreamento.correios.com.br/app/index.php?objeto={codigo}
    // Nulo ou vazio para formas sem rastreio (retirada no ateliê).
    private String urlRastreioTemplate;

    private Boolean ativo = true;

    /**
     * Link de rastreio para o código informado, substituindo {@code {codigo}} no template.
     * Nulo quando não há código ou a forma não oferece rastreio.
     */
    public String montarLinkRastreio(String codigoRastreio) {
        if (codigoRastreio == null || codigoRastreio.isBlank()
                || urlRastreioTemplate == null || urlRastreioTemplate.isBlank()) {
            return null;
        }
        return urlRastreioTemplate.replace(MARCADOR_CODIGO, codigoRastreio.trim());
    }
}
