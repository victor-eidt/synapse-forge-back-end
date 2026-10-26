package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import synapseforge.crud.DTO.User.PreferenciasDashboardDTO;
import synapseforge.crud.DTO.User.PreferenciasUsuarioDTO;
import synapseforge.crud.DTO.User.WidgetDashboardDTO;
import synapseforge.crud.infrastructure.entity.PreferenciasDashboard;
import synapseforge.crud.infrastructure.entity.PreferenciasUsuario;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.entity.WidgetDashboard;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PreferenciasUsuarioServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    private PreferenciasUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new PreferenciasUsuarioService(repository, mongoTemplate);
    }

    // ---------------------------------------------------------
    // GET
    // ---------------------------------------------------------

    @Test
    void semNadaSalvoTodosOsCamposVemNulos() {
        when(repository.findById("u-1")).thenReturn(Optional.of(usuario(null)));

        PreferenciasUsuarioDTO result = service.buscar("u-1");

        assertNull(result.getTema());
        assertNull(result.getIdioma());
        assertNull(result.getDashboard());
    }

    @Test
    void buscarDevolveOQueEstaSalvoNaOrdemDosWidgets() {
        PreferenciasUsuario salvas = new PreferenciasUsuario("escuro", "en-US", new PreferenciasDashboard(
                "6m",
                List.of(new WidgetDashboard("estoqueBaixo", true), new WidgetDashboard("indicadores", false))
        ));
        when(repository.findById("u-1")).thenReturn(Optional.of(usuario(salvas)));

        PreferenciasUsuarioDTO result = service.buscar("u-1");

        assertEquals("escuro", result.getTema());
        assertEquals("en-US", result.getIdioma());
        assertEquals("6m", result.getDashboard().getPeriodo());
        assertEquals("estoqueBaixo", result.getDashboard().getWidgets().get(0).getId());
        assertEquals(true, result.getDashboard().getWidgets().get(0).getVisivel());
        assertEquals("indicadores", result.getDashboard().getWidgets().get(1).getId());
        assertEquals(false, result.getDashboard().getWidgets().get(1).getVisivel());
    }

    @Test
    void buscarUsuarioInexistenteFalha() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.buscar("nope"));
    }

    // ---------------------------------------------------------
    // PUT parcial
    // ---------------------------------------------------------

    @Test
    void putSoComTemaAlteraSoOTema() {
        PreferenciasUsuario depois = new PreferenciasUsuario("claro", "pt-BR", null);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(usuario(depois));

        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setTema("claro");

        PreferenciasUsuarioDTO result = service.atualizar("u-1", dto);

        Document set = setDoUpdate();
        assertEquals("claro", set.get("preferencias.tema"));
        // idioma e dashboard não entram no $set: o que já estava salvo continua
        assertFalse(set.containsKey("preferencias.idioma"));
        assertFalse(set.containsKey("preferencias.dashboard"));
        assertFalse(set.containsKey("preferencias"));
        assertTrue(set.containsKey("atualizadoEm"));

        assertEquals("claro", result.getTema());
        assertEquals("pt-BR", result.getIdioma());
    }

    @Test
    void putAtualizaSoODocumentoDoProprioUsuarioEDevolveOSalvo() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(usuario(new PreferenciasUsuario(null, "en-US", null)));

        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setIdioma("en-US");

        service.atualizar("u-1", dto);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<FindAndModifyOptions> opcoes = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongoTemplate).findAndModify(query.capture(), any(Update.class), opcoes.capture(), eq(User.class));
        assertEquals("u-1", query.getValue().getQueryObject().get("id"));
        assertTrue(opcoes.getValue().isReturnNew());
    }

    @Test
    void putComDashboardSubstituiODashboardInteiro() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenAnswer(i -> usuario(new PreferenciasUsuario("escuro", null,
                        (PreferenciasDashboard) ((Update) i.getArgument(1)).getUpdateObject()
                                .get("$set", Document.class).get("preferencias.dashboard"))));

        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setDashboard(dashboard("12m",
                new WidgetDashboardDTO("pedidosPorEtapa", true),
                new WidgetDashboardDTO("indicadores", false)));

        PreferenciasUsuarioDTO result = service.atualizar("u-1", dto);

        Document set = setDoUpdate();
        assertFalse(set.containsKey("preferencias.tema"));
        PreferenciasDashboard gravado = (PreferenciasDashboard) set.get("preferencias.dashboard");
        assertEquals("12m", gravado.getPeriodo());
        assertEquals(2, gravado.getWidgets().size());
        assertEquals("pedidosPorEtapa", gravado.getWidgets().get(0).getId());
        assertEquals("indicadores", gravado.getWidgets().get(1).getId());
        assertEquals(false, gravado.getWidgets().get(1).getVisivel());

        assertEquals("escuro", result.getTema());
        assertEquals("12m", result.getDashboard().getPeriodo());
        assertEquals("pedidosPorEtapa", result.getDashboard().getWidgets().get(0).getId());
    }

    @Test
    void putVazioNaoGravaNadaEDevolveOAtual() {
        when(repository.findById("u-1")).thenReturn(Optional.of(usuario(new PreferenciasUsuario("escuro", null, null))));

        PreferenciasUsuarioDTO result = service.atualizar("u-1", new PreferenciasUsuarioDTO());

        assertEquals("escuro", result.getTema());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void putDeUsuarioInexistenteFalha() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(null);

        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setTema("claro");

        assertThrows(RuntimeException.class, () -> service.atualizar("nope", dto));
    }

    // ---------------------------------------------------------
    // Validação (400): nada é gravado
    // ---------------------------------------------------------

    @Test
    void temaForaDaListaERecusado() {
        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setTema("azul");

        assertRecusado(dto, "Tema inválido");
    }

    @Test
    void idiomaForaDaListaERecusado() {
        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setIdioma("pt");

        assertRecusado(dto, "Idioma inválido");
    }

    @Test
    void periodoForaDaListaERecusado() {
        assertRecusado(comDashboard(dashboard("7d", new WidgetDashboardDTO("indicadores", true))), "Período do dashboard inválido");
    }

    @Test
    void periodoAusenteERecusado() {
        assertRecusado(comDashboard(dashboard(null, new WidgetDashboardDTO("indicadores", true))), "Período do dashboard inválido");
    }

    @Test
    void widgetsAusentesSaoRecusados() {
        assertRecusado(comDashboard(new PreferenciasDashboardDTO("90d", null)), "Informe de 1 a 10 widgets");
    }

    @Test
    void listaDeWidgetsVaziaERecusada() {
        assertRecusado(comDashboard(new PreferenciasDashboardDTO("90d", List.of())), "Informe de 1 a 10 widgets");
    }

    @Test
    void maisDeDezWidgetsSaoRecusados() {
        List<WidgetDashboardDTO> onze = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            onze.add(new WidgetDashboardDTO("indicadores", true));
        }

        assertRecusado(comDashboard(new PreferenciasDashboardDTO("90d", onze)), "Informe de 1 a 10 widgets");
    }

    @Test
    void widgetDesconhecidoERecusado() {
        assertRecusado(comDashboard(dashboard("90d", new WidgetDashboardDTO("graficoPizza", true))), "Widget do dashboard inválido (graficoPizza)");
    }

    @Test
    void widgetSemIdERecusado() {
        assertRecusado(comDashboard(dashboard("90d", new WidgetDashboardDTO(null, true))), "Widget do dashboard inválido");
    }

    @Test
    void widgetNuloNaListaERecusado() {
        List<WidgetDashboardDTO> comNulo = new ArrayList<>();
        comNulo.add(null);

        assertRecusado(comDashboard(new PreferenciasDashboardDTO("90d", comNulo)), "Widget do dashboard inválido");
    }

    @Test
    void widgetRepetidoERecusado() {
        assertRecusado(comDashboard(dashboard("90d",
                new WidgetDashboardDTO("indicadores", true),
                new WidgetDashboardDTO("indicadores", false))), "Widget do dashboard repetido: indicadores");
    }

    @Test
    void widgetSemVisivelERecusado() {
        assertRecusado(comDashboard(dashboard("90d", new WidgetDashboardDTO("estoqueBaixo", null))), "Informe se o widget estoqueBaixo está visível");
    }

    @Test
    void campoValidoNaoSalvaQuandoOutroEInvalido() {
        // tudo ou nada: tema válido não é gravado se o idioma for recusado
        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setTema("claro");
        dto.setIdioma("es-ES");

        assertRecusado(dto, "Idioma inválido");
    }

    @Test
    void todosOsValoresDoContratoSaoAceitos() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(usuario(null));

        for (String periodo : List.of("30d", "90d", "6m", "12m", "ano", "tudo")) {
            PreferenciasUsuarioDTO dto = comDashboard(dashboard(periodo,
                    new WidgetDashboardDTO("indicadores", true),
                    new WidgetDashboardDTO("pedidosPorMes", true),
                    new WidgetDashboardDTO("pedidosPorEtapa", true),
                    new WidgetDashboardDTO("materiaisMaisUsados", false),
                    new WidgetDashboardDTO("estoqueBaixo", true)));
            dto.setTema(periodo.equals("30d") ? "claro" : "escuro");
            dto.setIdioma(periodo.equals("30d") ? "pt-BR" : "en-US");

            assertDoesNotThrow(() -> service.atualizar("u-1", dto));
        }
    }

    // ---------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------

    private void assertRecusado(PreferenciasUsuarioDTO dto, String trechoDaMensagem) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.atualizar("u-1", dto));
        assertTrue(ex.getMessage().startsWith(trechoDaMensagem), ex.getMessage());
        verifyNoInteractions(mongoTemplate);
    }

    private Document setDoUpdate() {
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(Query.class), update.capture(), any(FindAndModifyOptions.class), eq(User.class));
        return update.getValue().getUpdateObject().get("$set", Document.class);
    }

    private static PreferenciasUsuarioDTO comDashboard(PreferenciasDashboardDTO dashboard) {
        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setDashboard(dashboard);
        return dto;
    }

    private static PreferenciasDashboardDTO dashboard(String periodo, WidgetDashboardDTO... widgets) {
        return new PreferenciasDashboardDTO(periodo, List.of(widgets));
    }

    private static User usuario(PreferenciasUsuario preferencias) {
        User user = new User();
        user.setId("u-1");
        user.setPreferencias(preferencias);
        return user;
    }
}
