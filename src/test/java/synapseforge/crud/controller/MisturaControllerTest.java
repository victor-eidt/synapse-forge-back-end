package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Mistura.MisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.MisturaResponseDTO;
import synapseforge.crud.service.MisturaService;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class MisturaControllerTest {

    @Mock
    private MisturaService service;

    @InjectMocks
    private MisturaController controller;

    @Test
    void criarDeveRetornarMistura() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        MisturaRequestDTO dto = new MisturaRequestDTO();
        MisturaResponseDTO expected = new MisturaResponseDTO("m-1", "Mistura", List.of(), 100, "#FFFFFF", 1.0, null, null);

        when(service.criar(dto, "user-1")).thenReturn(expected);

        assertEquals("Mistura", controller.criar(dto, auth).getNome());
    }

    @Test
    void listarDeveRetornarLista() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        MisturaResponseDTO expected = new MisturaResponseDTO("m-1", "Mistura", List.of(), 100, "#FFFFFF", 1.0, null, null);
        when(service.listar("user-1")).thenReturn(List.of(expected));

        assertEquals(1, controller.listar(auth).size());
    }

    @Test
    void buscarDeveRetornarMistura() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        MisturaResponseDTO expected = new MisturaResponseDTO("m-1", "Mistura", List.of(), 100, "#FFFFFF", 1.0, null, null);
        when(service.buscarPorId("m-1", "user-1")).thenReturn(expected);

        assertEquals("Mistura", controller.buscar("m-1", auth).getNome());
    }

    @Test
    void atualizarDeveRetornarMisturaAtualizada() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        MisturaRequestDTO dto = new MisturaRequestDTO();
        MisturaResponseDTO expected = new MisturaResponseDTO("m-1", "Mistura Nova", List.of(), 100, "#FFFFFF", 1.0, null, null);
        when(service.atualizar("m-1", "user-1", dto)).thenReturn(expected);

        assertEquals("Mistura Nova", controller.atualizar("m-1", dto, auth).getNome());
    }

    @Test
    void deletarDeveChamarService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        controller.deletar("m-1", auth);

        verify(service).deletar("m-1", "user-1");
    }
}
