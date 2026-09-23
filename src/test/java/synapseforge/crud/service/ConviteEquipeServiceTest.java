package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusConviteEquipe;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.ConviteEquipeRepository;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/** SYN-101: o convidado vê e responde o próprio convite dentro do app. */
@ExtendWith(MockitoExtension.class)
class ConviteEquipeServiceTest {

    @Mock
    private ConviteEquipeRepository conviteRepository;

    @Mock
    private EquipeRepository equipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ConviteEquipeService service;

    private ConviteEquipe convitePendente(String usuarioId, LocalDateTime expiraEm) {
        ConviteEquipe convite = new ConviteEquipe();
        convite.setId("cv-1");
        convite.setEquipeId("eq-1");
        convite.setGerenteId("ger-1");
        convite.setUsuarioId(usuarioId);
        convite.setToken("tok-1");
        convite.setStatus(StatusConviteEquipe.PENDENTE);
        convite.setExpiraEm(expiraEm);
        return convite;
    }

    @Test
    void convidadoVeOProprioConvitePendente() {
        ConviteEquipe convite = convitePendente("cli-1", LocalDateTime.now().plusHours(3));
        when(conviteRepository.findByUsuarioIdAndStatus("cli-1", StatusConviteEquipe.PENDENTE))
                .thenReturn(Optional.of(convite));

        assertEquals(Optional.of(convite), service.buscarConvitePendenteDoUsuario("cli-1"));
        verify(conviteRepository, never()).save(any());
    }

    @Test
    void conviteVencidoNaoApareceEFicaExpirado() {
        ConviteEquipe convite = convitePendente("cli-1", LocalDateTime.now().minusMinutes(1));
        when(conviteRepository.findByUsuarioIdAndStatus("cli-1", StatusConviteEquipe.PENDENTE))
                .thenReturn(Optional.of(convite));

        assertTrue(service.buscarConvitePendenteDoUsuario("cli-1").isEmpty());
        verify(conviteRepository).save(argThat(c -> c.getStatus() == StatusConviteEquipe.EXPIRADO));
    }

    @Test
    void semConvitePendenteAceitarERecusarFalham() {
        when(conviteRepository.findByUsuarioIdAndStatus("cli-1", StatusConviteEquipe.PENDENTE))
                .thenReturn(Optional.empty());

        assertEquals("Convite não encontrado",
                assertThrows(RuntimeException.class, () -> service.aceitarConviteDoUsuario("cli-1")).getMessage());
        assertEquals("Convite não encontrado",
                assertThrows(RuntimeException.class, () -> service.recusarConviteDoUsuario("cli-1")).getMessage());
        verifyNoInteractions(userService);
    }

    @Test
    void aceitarNoAppUsaOConviteDoProprioUsuarioEEntraNaEquipe() {
        ConviteEquipe convite = convitePendente("cli-1", LocalDateTime.now().plusHours(3));
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");
        User cliente = new User();
        cliente.setId("cli-1");
        cliente.setRole(Role.CLIENTE);
        User tecnico = new User();
        tecnico.setId("cli-1");
        tecnico.setRole(Role.TECNICO);
        tecnico.setEquipeId("eq-1");

        when(conviteRepository.findByUsuarioIdAndStatus("cli-1", StatusConviteEquipe.PENDENTE))
                .thenReturn(Optional.of(convite));
        when(conviteRepository.findByToken("tok-1")).thenReturn(Optional.of(convite));
        when(equipeRepository.findById("eq-1")).thenReturn(Optional.of(equipe));
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(userService.entrarNaEquipe("cli-1", "eq-1")).thenReturn(tecnico);

        User resultado = service.aceitarConviteDoUsuario("cli-1");

        assertEquals(Role.TECNICO, resultado.getRole());
        assertEquals(StatusConviteEquipe.ACEITO, convite.getStatus());
        // a busca parte sempre do usuário logado: nunca de um id ou token vindo do request
        verify(conviteRepository).findByUsuarioIdAndStatus("cli-1", StatusConviteEquipe.PENDENTE);
    }
}
