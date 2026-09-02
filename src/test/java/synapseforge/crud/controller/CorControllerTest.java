package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Cor.CorRequestDTO;
import synapseforge.crud.DTO.Cor.CorResponseDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.service.CorService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CorControllerTest {

    @Mock
    private CorService service;

    @InjectMocks
    private CorController controller;

    @Test
    void criarDeveUsarUsuarioDoAuthentication() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        CorRequestDTO dto = new CorRequestDTO();
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setNome("Azul");
        CorResponseDTO response = new CorResponseDTO("c-1", "Azul", "Fornecedor", "ABC", "#0000FF", null, 100, 10, 2.5, LocalDateTime.now(), LocalDateTime.now());

        when(service.toEntity(dto, "user-1")).thenReturn(cor);
        when(service.criar(cor)).thenReturn(cor);
        when(service.toResponseDTO(cor)).thenReturn(response);

        CorResponseDTO result = controller.criar(dto, auth);

        assertEquals("Azul", result.getNome());
        verify(service).criar(cor);
    }

    @Test
    void listarDeveDelegarParaService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setNome("Azul");
        CorResponseDTO response = new CorResponseDTO("c-1", "Azul", "Fornecedor", "ABC", "#0000FF", null, 100, 10, 2.5, LocalDateTime.now(), LocalDateTime.now());

        when(service.listar("user-1")).thenReturn(List.of(cor));
        when(service.toResponseDTO(cor)).thenReturn(response);

        assertEquals(1, controller.listar(auth).size());
    }

    @Test
    void buscarDeveRetornarCor() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setNome("Azul");
        CorResponseDTO response = new CorResponseDTO("c-1", "Azul", "Fornecedor", "ABC", "#0000FF", null, 100, 10, 2.5, LocalDateTime.now(), LocalDateTime.now());

        when(service.buscarPorId("c-1", "user-1")).thenReturn(Optional.of(cor));
        when(service.toResponseDTO(cor)).thenReturn(response);

        assertEquals("Azul", controller.buscar("c-1", auth).getNome());
    }

    @Test
    void atualizarDeveRetornarCorAtualizada() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        CorRequestDTO dto = new CorRequestDTO();
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setNome("Verde");
        CorResponseDTO response = new CorResponseDTO("c-1", "Verde", "Fornecedor", "ABC", "#00FF00", null, 100, 10, 2.5, LocalDateTime.now(), LocalDateTime.now());

        when(service.toEntity(dto, "user-1")).thenReturn(cor);
        when(service.atualizar("c-1", "user-1", cor)).thenReturn(cor);
        when(service.toResponseDTO(cor)).thenReturn(response);

        assertEquals("Verde", controller.atualizar("c-1", dto, auth).getNome());
    }

    @Test
    void deletarDeveChamarService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        controller.deletar("c-1", auth);

        verify(service).deletar("c-1", "user-1");
    }
}
