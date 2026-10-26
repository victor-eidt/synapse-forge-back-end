package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.User.PreferenciasUsuarioDTO;
import synapseforge.crud.service.PreferenciasUsuarioService;

@ExtendWith(MockitoExtension.class)
class PreferenciasUsuarioControllerTest {

    @Mock
    private PreferenciasUsuarioService service;

    @InjectMocks
    private PreferenciasUsuarioController controller;

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("cliente-1");
        return auth;
    }

    @Test
    void buscarUsaOUsuarioDoToken() {
        PreferenciasUsuarioDTO salvas = new PreferenciasUsuarioDTO("escuro", "pt-BR", null);
        when(service.buscar("cliente-1")).thenReturn(salvas);

        PreferenciasUsuarioDTO result = controller.buscar(auth());

        assertSame(salvas, result);
        verify(service).buscar("cliente-1");
    }

    @Test
    void atualizarUsaOUsuarioDoTokenEDevolveOSalvo() {
        PreferenciasUsuarioDTO dto = new PreferenciasUsuarioDTO();
        dto.setTema("claro");
        PreferenciasUsuarioDTO salvas = new PreferenciasUsuarioDTO("claro", "en-US", null);
        when(service.atualizar("cliente-1", dto)).thenReturn(salvas);

        PreferenciasUsuarioDTO result = controller.atualizar(dto, auth());

        assertEquals("claro", result.getTema());
        assertEquals("en-US", result.getIdioma());
        verify(service).atualizar("cliente-1", dto);
    }
}
