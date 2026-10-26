package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Preferências do usuário logado (GET/PUT /users/me/preferencias). O mesmo formato serve de
 * requisição e de resposta. No PUT, campo ausente ou nulo = manter o valor atual; na resposta,
 * campo nunca salvo vem nulo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Preferências do usuário logado. No PUT, campo ausente ou nulo mantém o valor atual.")
public class PreferenciasUsuarioDTO {

    @Schema(description = "Tema da interface", example = "escuro", allowableValues = {"claro", "escuro"}, nullable = true)
    private String tema;

    @Schema(description = "Idioma da interface", example = "pt-BR", allowableValues = {"pt-BR", "en-US"}, nullable = true)
    private String idioma;

    @Schema(description = "Dashboard personalizado. Quando enviado, substitui o dashboard inteiro (periodo e widgets obrigatórios).", nullable = true)
    private PreferenciasDashboardDTO dashboard;
}
