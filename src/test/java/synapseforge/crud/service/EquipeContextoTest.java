package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EquipeContextoTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipeRepository equipeRepository;

    @InjectMocks
    private EquipeContexto equipeContexto;

    private User usuario(String id, Role role, String equipeId) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEquipeId(equipeId);
        return user;
    }

    private Equipe equipe(String id, String gerenteId) {
        Equipe equipe = new Equipe();
        equipe.setId(id);
        equipe.setGerenteId(gerenteId);
        return equipe;
    }

    @Test
    void equipeVemDoCadastroDoUsuario() {
        when(userRepository.findById("u-1")).thenReturn(Optional.of(usuario("u-1", Role.TECNICO, "eq-1")));

        assertEquals(Optional.of("eq-1"), equipeContexto.equipeDe("u-1"));
        assertEquals("eq-1", equipeContexto.equipeObrigatoria("u-1"));
        verifyNoInteractions(equipeRepository);
    }

    @Test
    void equipeDoUsuarioTemPrioridadeSobreEquipeQueGerencia() {
        when(userRepository.findById("g-1")).thenReturn(Optional.of(usuario("g-1", Role.GERENTE, "eq-1")));

        assertEquals(Optional.of("eq-1"), equipeContexto.equipeDe("g-1"));
        verifyNoInteractions(equipeRepository);
    }

    @Test
    void gerenteSemEquipeIdUsaAEquipeQueAdministra() {
        // quem cria a equipe pelo app não recebe equipeId; a equipe é achada pelo gerenteId
        when(userRepository.findById("g-1")).thenReturn(Optional.of(usuario("g-1", Role.GERENTE, null)));
        when(equipeRepository.findByGerenteId("g-1")).thenReturn(Optional.of(equipe("eq-7", "g-1")));

        assertEquals(Optional.of("eq-7"), equipeContexto.equipeDe("g-1"));
    }

    @Test
    void adminSemEquipeIdTambemUsaAEquipeQueAdministra() {
        when(userRepository.findById("a-1")).thenReturn(Optional.of(usuario("a-1", Role.ADMIN, " ")));
        when(equipeRepository.findByGerenteId("a-1")).thenReturn(Optional.of(equipe("eq-8", "a-1")));

        assertEquals(Optional.of("eq-8"), equipeContexto.equipeDe("a-1"));
    }

    @Test
    void tecnicoOuClienteSemEquipeIdNaoTemEquipe() {
        when(userRepository.findById("t-1")).thenReturn(Optional.of(usuario("t-1", Role.TECNICO, null)));
        when(userRepository.findById("c-1")).thenReturn(Optional.of(usuario("c-1", Role.CLIENTE, null)));

        assertTrue(equipeContexto.equipeDe("t-1").isEmpty());
        assertTrue(equipeContexto.equipeDe("c-1").isEmpty());
        // só GERENTE/ADMIN podem administrar equipe
        verify(equipeRepository, never()).findByGerenteId(any());
    }

    @Test
    void gerenteSemNenhumaEquipeNaoTemEquipe() {
        when(userRepository.findById("g-1")).thenReturn(Optional.of(usuario("g-1", Role.GERENTE, null)));
        when(equipeRepository.findByGerenteId("g-1")).thenReturn(Optional.empty());

        assertTrue(equipeContexto.equipeDe("g-1").isEmpty());
    }

    @Test
    void usuarioInexistenteOuNuloNaoTemEquipe() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());

        assertTrue(equipeContexto.equipeDe("x").isEmpty());
        assertTrue(equipeContexto.equipeDe((String) null).isEmpty());
    }

    @Test
    void equipeObrigatoriaSemEquipeLancaSemEquipeException() {
        when(userRepository.findById("t-1")).thenReturn(Optional.of(usuario("t-1", Role.TECNICO, null)));

        SemEquipeException ex = assertThrows(SemEquipeException.class,
                () -> equipeContexto.equipeObrigatoria("t-1"));

        assertEquals("SEM_EQUIPE", ex.getCodigo());
        assertEquals("Crie ou entre em uma equipe para acessar estes dados.", ex.getMessage());
    }

    @Test
    void clienteNuncaTemEquipeMesmoComEquipeIdGravado() {
        // cliente se vincula às oficinas só pelos pedidos
        when(userRepository.findById("c-1")).thenReturn(Optional.of(usuario("c-1", Role.CLIENTE, "eq-1")));

        assertTrue(equipeContexto.equipeDe("c-1").isEmpty());
        assertThrows(SemEquipeException.class, () -> equipeContexto.equipeObrigatoria("c-1"));
        verifyNoInteractions(equipeRepository);
    }
}
