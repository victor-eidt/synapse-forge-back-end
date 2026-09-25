package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.Estoque.AjusteEstoqueRequestDTO;
import synapseforge.crud.DTO.Estoque.AlertaEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.EntradaEstoqueRequestDTO;
import synapseforge.crud.DTO.Estoque.MovimentoEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.SaldoInsumoResponseDTO;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.service.EstoqueService;

@ExtendWith(MockitoExtension.class)
class EstoqueControllerTest {

    @Mock
    private EstoqueService service;

    @InjectMocks
    private EstoqueController controller;

    private Authentication auth(String principal) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        return auth;
    }

    @Test
    void registrarEntrada_deveDelegarParaService() {
        EntradaEstoqueRequestDTO dto = new EntradaEstoqueRequestDTO();
        dto.setTipoInsumo(TipoInsumo.MATERIAL);
        dto.setInsumoId("mat-1");
        dto.setQuantidade(new BigDecimal("10"));
        dto.setUnidade(UnidadeMedida.G);
        dto.setMotivo("Compra");

        MovimentoEstoqueResponseDTO response = new MovimentoEstoqueResponseDTO(
                "m-1",
                TipoInsumo.MATERIAL,
                "mat-1",
                synapseforge.crud.infrastructure.entity.TipoMovimento.ENTRADA,
                new BigDecimal("10"),
                UnidadeMedida.G,
                new BigDecimal("100"),
                null,
                null,
                null,
                null,
                "Compra",
                "user-1",
                null
        );

        when(service.registrarEntrada(TipoInsumo.MATERIAL, "mat-1", new BigDecimal("10"), UnidadeMedida.G, "Compra", "user-1"))
                .thenReturn(response);

        var result = controller.registrarEntrada(dto, auth("user-1"));

        assertEquals(HttpStatus.CREATED.value(), 201);
        assertEquals(synapseforge.crud.infrastructure.entity.TipoMovimento.ENTRADA, result.getTipo());
        verify(service).registrarEntrada(TipoInsumo.MATERIAL, "mat-1", new BigDecimal("10"), UnidadeMedida.G, "Compra", "user-1");
    }

    @Test
    void ajustar_deveDelegarParaService() {
        AjusteEstoqueRequestDTO dto = new AjusteEstoqueRequestDTO();
        dto.setTipoInsumo(TipoInsumo.COR);
        dto.setInsumoId("cor-1");
        dto.setQuantidade(new BigDecimal("-5"));
        dto.setUnidade(UnidadeMedida.ML);
        dto.setMotivo("Perda");

        MovimentoEstoqueResponseDTO response = new MovimentoEstoqueResponseDTO(
                "m-2",
                TipoInsumo.COR,
                "cor-1",
                synapseforge.crud.infrastructure.entity.TipoMovimento.AJUSTE,
                new BigDecimal("-5"),
                UnidadeMedida.ML,
                new BigDecimal("40"),
                null,
                null,
                null,
                null,
                "Perda",
                "user-1",
                null
        );

        when(service.ajustar(TipoInsumo.COR, "cor-1", new BigDecimal("-5"), UnidadeMedida.ML, "Perda", "user-1"))
                .thenReturn(response);

        var result = controller.ajustar(dto, auth("user-1"));

        assertEquals(synapseforge.crud.infrastructure.entity.TipoMovimento.AJUSTE, result.getTipo());
        verify(service).ajustar(TipoInsumo.COR, "cor-1", new BigDecimal("-5"), UnidadeMedida.ML, "Perda", "user-1");
    }

    @Test
    void listarEmAlerta_deveRetornarAlertas() {
        AlertaEstoqueResponseDTO alerta = new AlertaEstoqueResponseDTO(TipoInsumo.MATERIAL, "mat-1", "Resina", UnidadeMedida.G, new BigDecimal("50"), new BigDecimal("100"));
        when(service.listarEmAlerta("user-1")).thenReturn(List.of(alerta));

        var result = controller.listarEmAlerta(auth("user-1"));

        assertEquals(1, result.size());
        assertEquals("mat-1", result.get(0).getInsumoId());
    }

    @Test
    void consultarSaldo_deveDelegarParaService() {
        SaldoInsumoResponseDTO response = new SaldoInsumoResponseDTO(TipoInsumo.MATERIAL, "mat-1", "Resina", UnidadeMedida.G, new BigDecimal("120"), new BigDecimal("100"), false);
        when(service.consultarSaldo(TipoInsumo.MATERIAL, "mat-1", "user-1")).thenReturn(response);

        var result = controller.consultarSaldo(TipoInsumo.MATERIAL, "mat-1", auth("user-1"));

        assertEquals("Resina", result.getNome());
        assertEquals(new BigDecimal("120"), result.getSaldo());
    }

    @Test
    void listarMovimentos_deveDelegarParaService() {
        MovimentoEstoqueResponseDTO response = new MovimentoEstoqueResponseDTO(
                "m-3",
                TipoInsumo.MATERIAL,
                "mat-1",
                synapseforge.crud.infrastructure.entity.TipoMovimento.ENTRADA,
                new BigDecimal("5"),
                UnidadeMedida.G,
                new BigDecimal("50"),
                null,
                null,
                null,
                null,
                "movimento",
                "user-1",
                null
        );

        when(service.historicoPorInsumo(TipoInsumo.MATERIAL, "mat-1", "user-1")).thenReturn(List.of(response));

        var result = controller.listarMovimentos(TipoInsumo.MATERIAL, "mat-1", auth("user-1"));

        assertEquals(1, result.size());
        assertEquals("mat-1", result.get(0).getInsumoId());
    }
}
