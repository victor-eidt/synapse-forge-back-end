package synapseforge.crud.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoRequestDTO;
import synapseforge.crud.infrastructure.entity.*;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumoPedidoServiceTest {

    @Mock
    private ConsumoPedidoRepository repository;

    @InjectMocks
    private ConsumoPedidoService service;

    @Test
    void salvarDeveCriarFichaEMapearItens() {
        ConsumoPedidoRequestDTO dto = dto("pedido-1", "mat-1", StatusPedido.IMPRESSAO);
        when(repository.findByPedidoId("pedido-1")).thenReturn(Optional.empty());
        when(repository.save(any(ConsumoPedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.salvar(dto);

        assertEquals("pedido-1", result.getPedidoId());
        assertEquals(1, result.getItens().size());
        assertEquals("mat-1", result.getItens().get(0).getInsumoId());
        verify(repository).save(any(ConsumoPedido.class));
    }

    @Test
    void salvarDeveAtualizarFichaExistente() {
        ConsumoPedido existente = new ConsumoPedido();
        existente.setId("f-1");
        existente.setPedidoId("pedido-1");
        when(repository.findByPedidoId("pedido-1")).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        var result = service.salvar(dto("pedido-1", "cor-1", StatusPedido.PINTURA));

        assertEquals("f-1", result.getId());
        assertEquals("cor-1", result.getItens().get(0).getInsumoId());
    }

    @Test
    void salvarDeveRejeitarItensDuplicados() {
        ConsumoPedidoRequestDTO dto = dto("pedido-1", "mat-1", StatusPedido.IMPRESSAO);
        ItemConsumoRequestDTO duplicado = dto.getItens().get(0);
        dto.setItens(List.of(duplicado, duplicado));

        assertThrows(IllegalArgumentException.class, () -> service.salvar(dto));
        verifyNoInteractions(repository);
    }

    @Test
    void buscarPorPedidoDeveRetornarFichaOuFalhar() {
        ConsumoPedido ficha = new ConsumoPedido();
        ficha.setPedidoId("pedido-1");
        ficha.setItens(List.of(new ItemConsumo(TipoInsumo.MATERIAL, "mat-1",
                BigDecimal.TEN, UnidadeMedida.G, StatusPedido.FINALIZADO)));
        when(repository.findByPedidoId("pedido-1")).thenReturn(Optional.of(ficha));

        assertEquals("pedido-1", service.buscarPorPedido("pedido-1").getPedidoId());

        when(repository.findByPedidoId("ausente")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.buscarPorPedido("ausente"));
    }

    private ConsumoPedidoRequestDTO dto(String pedidoId, String insumoId, StatusPedido etapa) {
        ItemConsumoRequestDTO item = new ItemConsumoRequestDTO();
        item.setTipoInsumo(TipoInsumo.MATERIAL);
        item.setInsumoId(insumoId);
        item.setQuantidade(BigDecimal.TEN);
        item.setUnidade(UnidadeMedida.G);
        item.setEtapaConsumo(etapa);
        ConsumoPedidoRequestDTO dto = new ConsumoPedidoRequestDTO();
        dto.setPedidoId(pedidoId);
        dto.setItens(List.of(item));
        return dto;
    }
}
