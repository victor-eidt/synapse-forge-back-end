package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusConviteEquipe;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.ConviteEquipeRepository;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EquipeServiceTest {

    @Mock
    private EquipeRepository repository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConviteEquipeRepository conviteEquipeRepository;

    @InjectMocks
    private EquipeService service;

    @Test
    void criar_deveSalvarEquipeQuandoGerenteAindaNaoTemEquipe() {
        when(repository.findByGerenteId("ger-1")).thenReturn(Optional.empty());
        when(repository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe equipe = service.criar("ger-1", "Equipe X", "foto-1", "banner-1");

        assertEquals("Equipe X", equipe.getNome());
        assertEquals("ger-1", equipe.getGerenteId());
        assertNotNull(equipe.getCriadoEm());
        assertNotNull(equipe.getAtualizadoEm());
        verify(repository).save(any(Equipe.class));
    }

    @Test
    void criar_deveRejeitarGerenteQueJaPossuiEquipe() {
        when(repository.findByGerenteId("ger-1")).thenReturn(Optional.of(new Equipe()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.criar("ger-1", "Nova", "foto", "banner"));

        assertTrue(ex.getMessage().contains("já possui"));
        verify(repository, never()).save(any());
    }

    @Test
    void atualizar_deveAlterarNome() {
        Equipe equipe = equipe("eq-1", "Antiga", "ger-1");
        when(repository.findById("eq-1")).thenReturn(Optional.of(equipe));
        when(repository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe result = service.atualizar("eq-1", "ger-1", "Nova");

        assertEquals("Nova", result.getNome());
        assertNotNull(result.getAtualizadoEm());
    }

    @Test
    void atualizar_deveFalharQuandoGerenteNaoEhDono() {
        Equipe equipe = equipe("eq-1", "Antiga", "ger-2");
        when(repository.findById("eq-1")).thenReturn(Optional.of(equipe));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.atualizar("eq-1", "ger-1", "Nova"));

        assertTrue(ex.getMessage().contains("permissão"));
    }

    @Test
    void salvarFoto_eSalvarBanner_devePersistirEExcluirArquivoAnterior() {
        Equipe equipe = equipe("eq-1", "Equipe", "ger-1");
        equipe.setFotoFileId("507f1f77bcf86cd799439011");
        equipe.setBannerFileId("507f1f77bcf86cd799439012");
        when(repository.findById("eq-1")).thenReturn(Optional.of(equipe));
        when(repository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe foto = service.salvarFoto("eq-1", "ger-1", "507f1f77bcf86cd799439013");
        Equipe banner = service.salvarBanner("eq-1", "ger-1", "507f1f77bcf86cd799439014");

        assertEquals("507f1f77bcf86cd799439013", foto.getFotoFileId());
        assertEquals("507f1f77bcf86cd799439014", banner.getBannerFileId());
        verify(gridFsTemplate, atLeast(2)).delete(any(Query.class));
    }

    @Test
    void removerFoto_eRemoverBanner_deveLimparArquivos() {
        Equipe equipe = equipe("eq-1", "Equipe", "ger-1");
        equipe.setFotoFileId("507f1f77bcf86cd799439011");
        equipe.setBannerFileId("507f1f77bcf86cd799439012");
        when(repository.findById("eq-1")).thenReturn(Optional.of(equipe));
        when(repository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe foto = service.removerFoto("eq-1", "ger-1");
        Equipe banner = service.removerBanner("eq-1", "ger-1");

        assertNull(foto.getFotoFileId());
        assertNull(banner.getBannerFileId());
        verify(gridFsTemplate, atLeast(2)).delete(any(Query.class));
    }

    @Test
    void deletar_deveZerarMembrosEExcluirConvites() {
        Equipe equipe = equipe("eq-1", "Equipe", "ger-1");
        equipe.setFotoFileId("507f1f77bcf86cd799439011");
        equipe.setBannerFileId("507f1f77bcf86cd799439012");
        User tecnico = user("u-1", "Tec", Role.TECNICO, "eq-1");
        User cliente = user("u-2", "Cli", Role.CLIENTE, "eq-1");
        ConviteEquipe convite = new ConviteEquipe();
        convite.setId("cv-1");
        convite.setStatus(StatusConviteEquipe.PENDENTE);

        when(repository.findById("eq-1")).thenReturn(Optional.of(equipe));
        when(userRepository.findByEquipeId("eq-1")).thenReturn(List.of(tecnico, cliente));
        when(conviteEquipeRepository.findByEquipeIdAndStatus("eq-1", StatusConviteEquipe.PENDENTE))
                .thenReturn(List.of(convite));

        service.deletar("eq-1", "ger-1");

        assertNull(tecnico.getEquipeId());
        assertEquals(Role.CLIENTE, tecnico.getRole());
        assertNull(cliente.getEquipeId());
        verify(userRepository, times(2)).save(any(User.class));
        verify(conviteEquipeRepository).deleteAll(List.of(convite));
        verify(repository).deleteById("eq-1");
        verify(gridFsTemplate, atLeast(2)).delete(any(Query.class));
    }

    private Equipe equipe(String id, String nome, String gerenteId) {
        Equipe equipe = new Equipe();
        equipe.setId(id);
        equipe.setNome(nome);
        equipe.setGerenteId(gerenteId);
        equipe.setCriadoEm(LocalDateTime.now());
        equipe.setAtualizadoEm(LocalDateTime.now());
        return equipe;
    }

    private User user(String id, String nome, Role role, String equipeId) {
        User user = new User();
        user.setId(id);
        user.setNome(nome);
        user.setRole(role);
        user.setEquipeId(equipeId);
        return user;
    }
}
