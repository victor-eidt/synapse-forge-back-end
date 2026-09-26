package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.Equipe.ConviteEquipeResponseDTO;
import synapseforge.crud.DTO.Equipe.EquipeRequestDTO;
import synapseforge.crud.DTO.Equipe.EquipeResponseDTO;
import synapseforge.crud.DTO.Equipe.MeuConviteResponseDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.security.JwtService;
import synapseforge.crud.service.ConviteEquipeService;
import synapseforge.crud.service.EquipeService;
import synapseforge.crud.service.UserService;

@ExtendWith(MockitoExtension.class)
class EquipeControllerTest {

    @Mock
    private EquipeService service;

    @Mock
    private UserService userService;

    @Mock
    private ConviteEquipeService conviteEquipeService;

    @Mock
    private JwtService jwtService;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private EquipeController controller;

    private Authentication auth(String principal) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        return auth;
    }

    private void injectGridFsTemplate() throws Exception {
        Field field = EquipeController.class.getDeclaredField("gridFsTemplate");
        field.setAccessible(true);
        field.set(controller, gridFsTemplate);
    }

    @Test
    void criar_deveRetornarEquipeCriada() {
        EquipeRequestDTO dto = new EquipeRequestDTO();
        dto.setNome("Equipe Alpha");

        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        equipe.setNome("Equipe Alpha");

        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Equipe Alpha", "ger-1", null, null, null, null);

        when(service.criar("ger-1", "Equipe Alpha", null, null)).thenReturn(equipe);
        when(service.toResponseDTO(equipe)).thenReturn(response);

        ResponseEntity<EquipeResponseDTO> result = controller.criar(dto, auth("ger-1"));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("eq-1", result.getBody().getId());
        verify(service).criar("ger-1", "Equipe Alpha", null, null);
    }

    @Test
    void minhaEquipe_paraGerenteDeveBuscarEquipeAdministrada() {
        User gerente = new User();
        gerente.setId("ger-1");
        gerente.setRole(Role.ADMIN);

        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        equipe.setNome("Equipe Admin");

        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Equipe Admin", "ger-1", null, null, null, null);

        when(userService.buscarPorId("ger-1")).thenReturn(Optional.of(gerente));
        when(service.buscarPorGerenteId("ger-1")).thenReturn(Optional.of(equipe));
        when(service.toResponseDTO(equipe)).thenReturn(response);

        ResponseEntity<EquipeResponseDTO> result = controller.minhaEquipe(auth("ger-1"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("eq-1", result.getBody().getId());
    }

    @Test
    void minhaEquipe_paraTecnicoDeveBuscarEquipeVinculada() {
        User tecnico = new User();
        tecnico.setId("tec-1");
        tecnico.setRole(Role.TECNICO);
        tecnico.setEquipeId("eq-9");

        Equipe equipe = new Equipe();
        equipe.setId("eq-9");
        equipe.setNome("Equipe do Técnico");

        EquipeResponseDTO response = new EquipeResponseDTO("eq-9", "Equipe do Técnico", "ger-9", null, null, null, null);

        when(userService.buscarPorId("tec-1")).thenReturn(Optional.of(tecnico));
        when(service.buscarPorId("eq-9")).thenReturn(Optional.of(equipe));
        when(service.toResponseDTO(equipe)).thenReturn(response);

        ResponseEntity<EquipeResponseDTO> result = controller.minhaEquipe(auth("tec-1"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Equipe do Técnico", result.getBody().getNome());
    }

    @Test
    void listarIntegrantes_paraGerenteDeveUsarEquipeDoGerente() {
        User gerente = new User();
        gerente.setId("ger-1");
        gerente.setRole(Role.ADMIN);
        gerente.setEquipeId(null);

        User integrante = new User();
        integrante.setId("usr-1");
        integrante.setNome("Maria");
        integrante.setEquipeId("eq-1");

        UserResponseDTO dto = new UserResponseDTO("usr-1", "Maria", "maria@email.com", null, null, "TECNICO", "eq-1", null);

        when(userService.buscarPorId("ger-1")).thenReturn(Optional.of(gerente));
        when(service.buscarPorGerenteId("ger-1")).thenReturn(Optional.of(new Equipe("eq-1", "Equipe", "ger-1", null, null, null, null)));
        when(userService.listarPorEquipeId("eq-1")).thenReturn(List.of(integrante));
        when(userService.toResponseDTO(integrante)).thenReturn(dto);

        List<UserResponseDTO> result = controller.listarIntegrantes(auth("ger-1"));

        assertEquals(1, result.size());
        assertEquals("Maria", result.get(0).getNome());
    }

    @Test
    void criarConvite_deveDelegarAoService() {
        ConviteEquipe convite = new ConviteEquipe();
        convite.setId("cv-1");
        convite.setEquipeId("eq-1");
        convite.setGerenteId("ger-1");
        convite.setUsuarioId("usr-1");

        ConviteEquipeResponseDTO dto = new ConviteEquipeResponseDTO("cv-1", "eq-1", "Equipe A", "ger-1", "Gerente", "usr-1", "Usuário", null, null, null);

        when(conviteEquipeService.criarConvite("eq-1", "ger-1", "usr-1")).thenReturn(convite);
        when(conviteEquipeService.toResponseDTO(convite)).thenReturn(dto);

        ConviteEquipeResponseDTO result = controller.criarConvite("eq-1", "usr-1", auth("ger-1"));

        assertEquals("cv-1", result.getId());
        verify(conviteEquipeService).criarConvite("eq-1", "ger-1", "usr-1");
    }

    @Test
    void meuConvite_deveRetornarConvitePendente() {
        ConviteEquipe convite = new ConviteEquipe();
        convite.setId("cv-1");
        convite.setEquipeId("eq-1");

        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        equipe.setNome("Equipe do convite");

        MeuConviteResponseDTO esperado = new MeuConviteResponseDTO(
                new ConviteEquipeResponseDTO("cv-1", "eq-1", "Equipe do convite", "ger-1", "Gerente", "usr-1", "Cliente", null, null, null),
                new EquipeResponseDTO("eq-1", "Equipe do convite", "ger-1", null, null, null, null)
        );

        when(conviteEquipeService.buscarConvitePendenteDoUsuario("usr-1")).thenReturn(Optional.of(convite));
        when(conviteEquipeService.toResponseDTO(convite)).thenReturn(esperado.getConvite());
        when(service.buscarPorId("eq-1")).thenReturn(Optional.of(equipe));
        when(service.toResponseDTO(equipe)).thenReturn(esperado.getEquipe());

        ResponseEntity<MeuConviteResponseDTO> result = controller.meuConvite(auth("usr-1"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("eq-1", result.getBody().getEquipe().getId());
    }

    @Test
    void aceitarMeuConvite_deveGerarTokenAtualizado() {
        User usuario = new User();
        usuario.setId("usr-1");
        usuario.setRole(Role.TECNICO);

        when(conviteEquipeService.aceitarConviteDoUsuario("usr-1")).thenReturn(usuario);
        when(jwtService.generateToken("usr-1", Role.TECNICO)).thenReturn("jwt-token");

        Map<String, String> result = controller.aceitarMeuConvite(auth("usr-1"));

        assertEquals("jwt-token", result.get("access_token"));
        assertEquals("usr-1", result.get("user_id"));
    }

    @Test
    void atualizarFuncaoVisual_deveAceitarJsonEValidarPertencimento() {
        User gerente = new User();
        gerente.setId("ger-1");
        gerente.setRole(Role.ADMIN);
        gerente.setEquipeId(null);

        User integrante = new User();
        integrante.setId("usr-1");
        integrante.setEquipeId("eq-1");

        User atualizado = new User();
        atualizado.setId("usr-1");
        atualizado.setFuncaoVisual("Pintor");

        UserResponseDTO dto = new UserResponseDTO("usr-1", "Maria", "maria@email.com", null, null, "TECNICO", "eq-1", "Pintor");

        when(userService.buscarPorId("ger-1")).thenReturn(Optional.of(gerente));
        when(service.buscarPorGerenteId("ger-1")).thenReturn(Optional.of(new Equipe("eq-1", "Equipe", "ger-1", null, null, null, null)));
        when(userService.buscarPorId("usr-1")).thenReturn(Optional.of(integrante));
        when(userService.atualizarFuncaoVisual("usr-1", "Pintor")).thenReturn(atualizado);
        when(userService.toResponseDTO(atualizado)).thenReturn(dto);

        UserResponseDTO result = controller.atualizarFuncaoVisual("usr-1", "\"Pintor\"", auth("ger-1"));

        assertNotNull(result);
        assertEquals("Pintor", result.getFuncaoVisual());
        verify(userService).atualizarFuncaoVisual("usr-1", "Pintor");
    }

    @Test
    void listarConvites_deveRetornarListaDaEquipe() {
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        equipe.setGerenteId("ger-1");

        ConviteEquipe convite = new ConviteEquipe();
        convite.setId("cv-1");
        convite.setEquipeId("eq-1");

        ConviteEquipeResponseDTO response = new ConviteEquipeResponseDTO(
                "cv-1", "eq-1", "Equipe A", "ger-1", "Gerente", "usr-1", "Maria",
                null, null, null
        );

        when(service.buscarPorId("eq-1")).thenReturn(Optional.of(equipe));
        when(conviteEquipeService.listarConvitesDaEquipe("eq-1")).thenReturn(List.of(convite));
        when(conviteEquipeService.toResponseDTO(convite)).thenReturn(response);

        List<ConviteEquipeResponseDTO> result = controller.listarConvites("eq-1", auth("ger-1"));

        assertEquals(1, result.size());
        assertEquals("cv-1", result.get(0).getId());
    }

    @Test
    void buscarConvite_deveRetornarConvitePublico() {
        ConviteEquipe convite = new ConviteEquipe();
        convite.setId("cv-1");
        ConviteEquipeResponseDTO response = new ConviteEquipeResponseDTO(
                "cv-1", "eq-1", "Equipe A", "ger-1", "Gerente", "usr-1", "Maria",
                null, null, null
        );

        when(conviteEquipeService.buscarPorToken("token-1")).thenReturn(convite);
        when(conviteEquipeService.toResponseDTO(convite)).thenReturn(response);

        assertEquals("cv-1", controller.buscarConvite("token-1").getId());
    }

    @Test
    void aceitarConvitePublico_deveRetornarUsuarioAtualizado() {
        User usuario = new User();
        usuario.setId("usr-1");
        UserResponseDTO response = new UserResponseDTO("usr-1", "Maria", "maria@email.com", null, null, "TECNICO", "eq-1", null);

        when(conviteEquipeService.aceitarConvite("token-1")).thenReturn(usuario);
        when(userService.toResponseDTO(usuario)).thenReturn(response);

        assertEquals("usr-1", controller.aceitarConvite("token-1").getId());
    }

    @Test
    void meuConvite_semConvite_deveRetornarNoContent() {
        when(conviteEquipeService.buscarConvitePendenteDoUsuario("usr-1")).thenReturn(Optional.empty());

        ResponseEntity<MeuConviteResponseDTO> result = controller.meuConvite(auth("usr-1"));

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void recusarMeuConvite_deveRetornarNoContent() {
        ResponseEntity<Void> result = controller.recusarMeuConvite(auth("usr-1"));

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(conviteEquipeService).recusarConviteDoUsuario("usr-1");
    }

    @Test
    void recusarConvitePublico_deveRetornarUsuarioAtualizado() {
        User usuario = new User();
        usuario.setId("usr-2");
        UserResponseDTO response = new UserResponseDTO("usr-2", "Ana", "ana@email.com", null, null, "CLIENTE", null, null);

        when(conviteEquipeService.recusarConvite("token-2")).thenReturn(usuario);
        when(userService.toResponseDTO(usuario)).thenReturn(response);

        assertEquals("usr-2", controller.recusarConvite("token-2").getId());
    }

    @Test
    void removerIntegrante_deveValidarGerenteERemoverMembro() {
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        equipe.setGerenteId("ger-1");

        User usuario = new User();
        usuario.setId("usr-1");

        UserResponseDTO response = new UserResponseDTO("usr-1", "Maria", "maria@email.com", null, null, "CLIENTE", null, null);

        when(service.buscarPorId("eq-1")).thenReturn(Optional.of(equipe));
        when(userService.sairDaEquipe("usr-1", "eq-1")).thenReturn(usuario);
        when(userService.toResponseDTO(usuario)).thenReturn(response);

        assertEquals("usr-1", controller.removerIntegrante("eq-1", "usr-1", auth("ger-1")).getId());
    }

    @Test
    void sairDaEquipe_deveGerarTokenAtualizado() {
        User usuario = new User();
        usuario.setId("usr-1");
        usuario.setEquipeId("eq-1");
        usuario.setRole(Role.TECNICO);

        User atualizado = new User();
        atualizado.setId("usr-1");
        atualizado.setRole(Role.CLIENTE);

        when(userService.buscarPorId("usr-1")).thenReturn(Optional.of(usuario));
        when(userService.sairDaEquipe("usr-1", "eq-1")).thenReturn(atualizado);
        when(jwtService.generateToken("usr-1", Role.CLIENTE)).thenReturn("jwt-saida");

        Map<String, String> result = controller.sairDaEquipe(auth("usr-1"));

        assertEquals("jwt-saida", result.get("access_token"));
        assertEquals("usr-1", result.get("user_id"));
    }

    @Test
    void atualizarEquipe_deveRenomearEquipe() {
        EquipeRequestDTO dto = new EquipeRequestDTO();
        dto.setNome("Novo nome");

        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        equipe.setNome("Novo nome");

        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Novo nome", "ger-1", null, null, null, null);

        when(service.atualizar("eq-1", "ger-1", "Novo nome")).thenReturn(equipe);
        when(service.toResponseDTO(equipe)).thenReturn(response);

        EquipeResponseDTO result = controller.atualizar("eq-1", dto, auth("ger-1"));

        assertEquals("Novo nome", result.getNome());
    }

    @Test
    void uploadFoto_devePersistirArquivo() throws Exception {
        injectGridFsTemplate();

        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Equipe", "ger-1", null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});

        when(gridFsTemplate.store(any(), anyString(), anyString(), any())).thenReturn(new org.bson.types.ObjectId());
        when(service.salvarFoto(eq("eq-1"), eq("ger-1"), anyString())).thenReturn(equipe);
        when(service.toResponseDTO(equipe)).thenReturn(response);

        EquipeResponseDTO result = controller.uploadFoto("eq-1", file, auth("ger-1"));

        assertEquals("eq-1", result.getId());
    }

    @Test
    void removerFoto_deveRemoverArquivo() {
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Equipe", "ger-1", null, null, null, null);

        when(service.removerFoto("eq-1", "ger-1")).thenReturn(equipe);
        when(service.toResponseDTO(equipe)).thenReturn(response);

        assertEquals("eq-1", controller.removerFoto("eq-1", auth("ger-1")).getId());
    }

    @Test
    void uploadBanner_devePersistirBanner() throws Exception {
        injectGridFsTemplate();

        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Equipe", "ger-1", null, null, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", new byte[]{1, 2, 3});

        when(gridFsTemplate.store(any(), anyString(), anyString(), any())).thenReturn(new org.bson.types.ObjectId());
        when(service.salvarBanner(eq("eq-1"), eq("ger-1"), anyString())).thenReturn(equipe);
        when(service.toResponseDTO(equipe)).thenReturn(response);

        EquipeResponseDTO result = controller.uploadBanner("eq-1", file, auth("ger-1"));

        assertEquals("eq-1", result.getId());
    }

    @Test
    void removerBanner_deveRemoverBanner() {
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        EquipeResponseDTO response = new EquipeResponseDTO("eq-1", "Equipe", "ger-1", null, null, null, null);

        when(service.removerBanner("eq-1", "ger-1")).thenReturn(equipe);
        when(service.toResponseDTO(equipe)).thenReturn(response);

        assertEquals("eq-1", controller.removerBanner("eq-1", auth("ger-1")).getId());
    }

    @Test
    void deletarEquipe_deveRemoverEquipe() {
        ResponseEntity<Void> response = controller.deletar("eq-1", auth("ger-1"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deletar("eq-1", "ger-1");
    }
}
