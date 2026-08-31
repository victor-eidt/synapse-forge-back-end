package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.OrdemPintura.AtualizarEtapaOrdemPinturaDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaRequestDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaResponseDTO;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;
import synapseforge.crud.service.OrdemPinturaService;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrdemPinturaControllerTest {

    @Mock
    private OrdemPinturaService service;

    @InjectMocks
    private OrdemPinturaController controller;

    @Test
    void listarDeveRetornarOrdens() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        OrdemPinturaResponseDTO response = new OrdemPinturaResponseDTO("ord-1", "p-1", "Projeto", "Cliente", "c-1", "Azul", "#0000FF", null, "José", PrioridadeOrdemPintura.ALTA, LocalDate.now(), EtapaOrdemPintura.AGUARDANDO, List.of(), null, null);
        when(service.listar("user-1")).thenReturn(List.of(response));

        assertEquals(1, controller.listar(auth).size());
    }

    @Test
    void criarDeveRetornarOrdem() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        OrdemPinturaRequestDTO dto = new OrdemPinturaRequestDTO();
        dto.setPedidoId("p-1");
        dto.setCorId("c-1");
        dto.setTecnico("José");
        dto.setPrioridade(PrioridadeOrdemPintura.ALTA);
        dto.setPrazo(LocalDate.now());

        OrdemPinturaResponseDTO response = new OrdemPinturaResponseDTO("ord-1", "p-1", "Projeto", "Cliente", "c-1", "Azul", "#0000FF", null, "José", PrioridadeOrdemPintura.ALTA, LocalDate.now(), EtapaOrdemPintura.AGUARDANDO, List.of(), null, null);
        when(service.criar(dto, "user-1")).thenReturn(response);

        assertEquals("José", controller.criar(dto, auth).getTecnicoNome());
    }

    @Test
    void atualizarEtapaDeveRetornarOrdemAtualizada() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        AtualizarEtapaOrdemPinturaDTO dto = new AtualizarEtapaOrdemPinturaDTO();
        dto.setEtapa(EtapaOrdemPintura.EM_PINTURA);

        OrdemPinturaResponseDTO response = new OrdemPinturaResponseDTO("ord-1", "p-1", "Projeto", "Cliente", "c-1", "Azul", "#0000FF", null, "José", PrioridadeOrdemPintura.ALTA, LocalDate.now(), EtapaOrdemPintura.EM_PINTURA, List.of(), null, null);
        when(service.atualizarEtapa("ord-1", EtapaOrdemPintura.EM_PINTURA, "user-1")).thenReturn(response);

        assertEquals(EtapaOrdemPintura.EM_PINTURA, controller.atualizarEtapa("ord-1", dto, auth).getEtapa());
    }

    @Test
    void atualizarDeveRetornarOrdemAtualizada() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        OrdemPinturaRequestDTO dto = new OrdemPinturaRequestDTO();
        dto.setPedidoId("p-1");
        dto.setCorId("c-1");
        dto.setTecnico("José");
        dto.setPrioridade(PrioridadeOrdemPintura.MEDIA);
        dto.setPrazo(LocalDate.now());

        OrdemPinturaResponseDTO response = new OrdemPinturaResponseDTO("ord-1", "p-1", "Projeto", "Cliente", "c-1", "Azul", "#0000FF", null, "José", PrioridadeOrdemPintura.MEDIA, LocalDate.now(), EtapaOrdemPintura.AGUARDANDO, List.of(), null, null);
        when(service.atualizar("ord-1", dto, "user-1")).thenReturn(response);

        assertEquals(PrioridadeOrdemPintura.MEDIA, controller.atualizar("ord-1", dto, auth).getPrioridade());
    }

    @Test
    void deletarDeveChamarService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        controller.deletar("ord-1", auth);

        verify(service).deletar("ord-1", "user-1");
    }
}
