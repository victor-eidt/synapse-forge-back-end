package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import synapseforge.crud.DTO.User.PreferenciasDashboardDTO;
import synapseforge.crud.DTO.User.PreferenciasUsuarioDTO;
import synapseforge.crud.DTO.User.WidgetDashboardDTO;
import synapseforge.crud.infrastructure.entity.PreferenciasDashboard;
import synapseforge.crud.infrastructure.entity.PreferenciasUsuario;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.entity.WidgetDashboard;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Preferências do próprio usuário (RF16): tema, idioma e dashboard personalizado.
 * Ficam embutidas no documento do usuário, campo "preferencias".
 */
@Service
@RequiredArgsConstructor
public class PreferenciasUsuarioService {

    static final List<String> TEMAS = List.of("claro", "escuro");
    static final List<String> IDIOMAS = List.of("pt-BR", "en-US");
    static final List<String> PERIODOS = List.of("30d", "90d", "6m", "12m", "ano", "tudo");
    static final List<String> WIDGETS = List.of(
            "indicadores",
            "pedidosPorMes",
            "pedidosPorEtapa",
            "materiaisMaisUsados",
            "estoqueBaixo"
    );
    static final int MAX_WIDGETS = 10;

    private final UserRepository repository;
    private final MongoTemplate mongoTemplate;


    // =========================================================
    // BUSCAR
    // =========================================================

    public PreferenciasUsuarioDTO buscar(String usuarioId) {

        User user = repository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        return toDTO(user.getPreferencias());
    }


    // =========================================================
    // ATUALIZAR
    // =========================================================
    //
    // Campo ausente ou nulo = manter o valor atual. O dashboard, quando
    // vem, substitui o dashboard inteiro.
    //
    // $set só nos caminhos enviados (preferencias.tema etc.), direto no
    // banco: trocar o tema não regrava o dashboard salvo ao mesmo tempo em
    // outra aba, e nenhum outro campo do usuário é tocado.
    //

    public PreferenciasUsuarioDTO atualizar(
            String usuarioId,
            PreferenciasUsuarioDTO dto
    ) {

        if (dto == null) {
            return buscar(usuarioId);
        }

        validar(dto);

        Update update = new Update();

        if (dto.getTema() != null) {
            update.set("preferencias.tema", dto.getTema());
        }

        if (dto.getIdioma() != null) {
            update.set("preferencias.idioma", dto.getIdioma());
        }

        if (dto.getDashboard() != null) {
            update.set("preferencias.dashboard", toEntity(dto.getDashboard()));
        }

        // Nada enviado: devolve o que já está salvo
        if (update.getUpdateObject().isEmpty()) {
            return buscar(usuarioId);
        }

        update.set("atualizadoEm", LocalDateTime.now());

        User atualizado = mongoTemplate.findAndModify(
                Query.query(Criteria.where("id").is(usuarioId)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                User.class
        );

        if (atualizado == null) {
            throw new RuntimeException("Usuário não encontrado");
        }

        return toDTO(atualizado.getPreferencias());
    }


    // =========================================================
    // VALIDAÇÃO
    // =========================================================

    void validar(PreferenciasUsuarioDTO dto) {

        if (dto.getTema() != null && !TEMAS.contains(dto.getTema())) {
            throw new IllegalArgumentException(
                    "Tema inválido: use claro ou escuro"
            );
        }

        if (dto.getIdioma() != null && !IDIOMAS.contains(dto.getIdioma())) {
            throw new IllegalArgumentException(
                    "Idioma inválido: use pt-BR ou en-US"
            );
        }

        if (dto.getDashboard() != null) {
            validarDashboard(dto.getDashboard());
        }
    }

    private void validarDashboard(PreferenciasDashboardDTO dashboard) {

        if (dashboard.getPeriodo() == null
                || !PERIODOS.contains(dashboard.getPeriodo())) {
            throw new IllegalArgumentException(
                    "Período do dashboard inválido: use "
                            + String.join(", ", PERIODOS)
            );
        }

        List<WidgetDashboardDTO> widgets = dashboard.getWidgets();

        if (widgets == null
                || widgets.isEmpty()
                || widgets.size() > MAX_WIDGETS) {
            throw new IllegalArgumentException(
                    "Informe de 1 a " + MAX_WIDGETS + " widgets do dashboard"
            );
        }

        Set<String> vistos = new HashSet<>();

        for (WidgetDashboardDTO widget : widgets) {

            if (widget == null
                    || widget.getId() == null
                    || !WIDGETS.contains(widget.getId())) {
                throw new IllegalArgumentException(
                        "Widget do dashboard inválido"
                                + (widget != null && widget.getId() != null
                                        ? " (" + widget.getId() + ")"
                                        : "")
                                + ": use "
                                + String.join(", ", WIDGETS)
                );
            }

            if (!vistos.add(widget.getId())) {
                throw new IllegalArgumentException(
                        "Widget do dashboard repetido: " + widget.getId()
                );
            }

            if (widget.getVisivel() == null) {
                throw new IllegalArgumentException(
                        "Informe se o widget " + widget.getId() + " está visível"
                );
            }
        }
    }


    // =========================================================
    // CONVERSÃO
    // =========================================================

    // Nunca salvou = todos os campos nulos (o front usa os padrões dele)
    PreferenciasUsuarioDTO toDTO(PreferenciasUsuario preferencias) {

        if (preferencias == null) {
            return new PreferenciasUsuarioDTO();
        }

        PreferenciasDashboard dashboard = preferencias.getDashboard();

        return new PreferenciasUsuarioDTO(
                preferencias.getTema(),
                preferencias.getIdioma(),
                dashboard == null
                        ? null
                        : new PreferenciasDashboardDTO(
                                dashboard.getPeriodo(),
                                dashboard.getWidgets() == null
                                        ? null
                                        : dashboard.getWidgets()
                                                .stream()
                                                .map(widget -> new WidgetDashboardDTO(
                                                        widget.getId(),
                                                        widget.getVisivel()
                                                ))
                                                .toList()
                        )
        );
    }

    private PreferenciasDashboard toEntity(PreferenciasDashboardDTO dto) {

        return new PreferenciasDashboard(
                dto.getPeriodo(),
                dto.getWidgets()
                        .stream()
                        .map(widget -> new WidgetDashboard(
                                widget.getId(),
                                widget.getVisivel()
                        ))
                        .toList()
        );
    }
}
