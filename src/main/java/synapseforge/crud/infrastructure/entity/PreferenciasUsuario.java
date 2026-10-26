package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Preferências do próprio usuário (RF16), embutidas no documento de users.
 * Campo nunca salvo fica nulo: o front usa o padrão dele (ou o que está no localStorage).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasUsuario {

    private String tema;
    private String idioma;
    private PreferenciasDashboard dashboard;
}
