package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import synapseforge.crud.DTO.Admin.AdminUserUpdateRequestDTO;
import synapseforge.crud.DTO.User.PerfilUpdateRequestDTO;
import synapseforge.crud.DTO.User.PreferenciasDashboardDTO;
import synapseforge.crud.DTO.User.PreferenciasUsuarioDTO;
import synapseforge.crud.DTO.User.WidgetDashboardDTO;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.List;

/**
 * Preferências num MongoDB de verdade (o embutido): confere o formato gravado em users e que
 * os PUTs parciais e as outras gravações do usuário (perfil, admin) não apagam nada.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.auto-index-creation=true",
    "de.flapdoodle.mongodb.embedded.version=7.0.0",
    "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tMzItYnl0ZXMhISE="
})
class PreferenciasUsuarioMongoTest {

    private static final String EMAIL = "preferencias.syn166@teste.com";

    @Autowired
    private PreferenciasUsuarioService preferenciasService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String usuarioId;

    @BeforeEach
    void criarUsuario() {
        User user = new User();
        user.setNome("Preferências Teste");
        user.setEmail(EMAIL);
        user.setRole(Role.CLIENTE);
        user.setAtivo(true);
        usuarioId = userRepository.save(user).getId();
    }

    @AfterEach
    void limpar() {
        userRepository.deleteById(usuarioId);
    }

    @Test
    void putsParciaisSeSomamEOWidgetFicaGravadoComoId() {
        assertNull(preferenciasService.buscar(usuarioId).getTema());
        assertNull(preferenciasService.buscar(usuarioId).getDashboard());

        preferenciasService.atualizar(usuarioId, new PreferenciasUsuarioDTO("escuro", null, null));
        preferenciasService.atualizar(usuarioId, new PreferenciasUsuarioDTO(null, "en-US", null));
        PreferenciasUsuarioDTO salvas = preferenciasService.atualizar(usuarioId, new PreferenciasUsuarioDTO(null, null, dashboard()));

        assertEquals("escuro", salvas.getTema());
        assertEquals("en-US", salvas.getIdioma());
        assertEquals("6m", salvas.getDashboard().getPeriodo());
        assertEquals(List.of("estoqueBaixo", "indicadores"),
                salvas.getDashboard().getWidgets().stream().map(WidgetDashboardDTO::getId).toList());

        // novo dashboard substitui o anterior inteiro
        salvas = preferenciasService.atualizar(usuarioId, new PreferenciasUsuarioDTO(null, null,
                new PreferenciasDashboardDTO("30d", List.of(new WidgetDashboardDTO("pedidosPorMes", true)))));
        assertEquals("escuro", salvas.getTema());
        assertEquals(1, salvas.getDashboard().getWidgets().size());

        Document documento = mongoTemplate.getCollection("users")
                .find(new Document("_id", new ObjectId(usuarioId)))
                .first();
        Document widget = documento.get("preferencias", Document.class)
                .get("dashboard", Document.class)
                .getList("widgets", Document.class)
                .get(0);
        assertEquals("pedidosPorMes", widget.getString("id"));
        assertFalse(widget.containsKey("_id"));
        assertEquals(true, widget.getBoolean("visivel"));
        // os outros campos do usuário continuam lá
        assertEquals(EMAIL, documento.getString("email"));
    }

    @Test
    void perfilEAdminNaoApagamAsPreferencias() {
        preferenciasService.atualizar(usuarioId, new PreferenciasUsuarioDTO("escuro", "pt-BR", dashboard()));

        PerfilUpdateRequestDTO perfil = new PerfilUpdateRequestDTO();
        perfil.setNome("Preferências Teste Editado");
        userService.atualizarProprioPerfil(usuarioId, perfil);

        AdminUserUpdateRequestDTO admin = new AdminUserUpdateRequestDTO();
        admin.setTelefone("(41) 99876-5432");
        adminService.atualizarUsuario(usuarioId, admin);

        PreferenciasUsuarioDTO depois = preferenciasService.buscar(usuarioId);
        assertEquals("escuro", depois.getTema());
        assertEquals("pt-BR", depois.getIdioma());
        assertEquals("6m", depois.getDashboard().getPeriodo());
        assertEquals(2, depois.getDashboard().getWidgets().size());
        assertEquals("Preferências Teste Editado", userRepository.findById(usuarioId).orElseThrow().getNome());
    }

    private static PreferenciasDashboardDTO dashboard() {
        return new PreferenciasDashboardDTO("6m", List.of(
                new WidgetDashboardDTO("estoqueBaixo", true),
                new WidgetDashboardDTO("indicadores", false)
        ));
    }
}
