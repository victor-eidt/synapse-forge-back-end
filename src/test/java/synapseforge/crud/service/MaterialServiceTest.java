package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository repository;

    @Mock
    private EquipeContexto equipeContexto;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private MaterialService service;

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

    private void semEquipe(String usuarioId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenThrow(new SemEquipeException());
    }

    private MaterialRequestDTO dtoPla() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PLA");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.24);
        dto.setPrecoPorGrama(new BigDecimal("0.05"));
        dto.setAtivo(true);
        return dto;
    }

    private Material materialPla() {
        Material material = new Material();
        material.setId("m-1");
        material.setEquipeId("eq-1");
        material.setNome("PLA");
        material.setTipo("Filamento");
        material.setDensidadeGcm3(1.24);
        material.setPrecoPorGrama(new BigDecimal("0.05"));
        material.setAtivo(true);
        return material;
    }

    @Test
    void criarDevePersistirMaterialNaEquipe() {
        naEquipe("user-1", "eq-1");
        when(repository.save(any(Material.class))).thenReturn(materialPla());

        MaterialResponseDTO result = service.criar(dtoPla(), "user-1");

        assertEquals("PLA", result.getNome());
        assertTrue(result.getAtivo());
        verify(repository).save(argThat(m -> "eq-1".equals(m.getEquipeId())));
    }

    @Test
    void criarSemEquipeDeveSerRecusado() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.criar(dtoPla(), "user-1"));
        verify(repository, never()).save(any());
    }

    @Test
    void criarComEstoqueMinimoInformadoPersisteOValorESaldoNasceZerado() {
        naEquipe("user-1", "eq-1");
        MaterialRequestDTO dto = dtoPla();
        dto.setAtivo(null);
        dto.setEstoqueMinimo(new BigDecimal("200"));

        when(repository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialResponseDTO result = service.criar(dto, "user-1");

        assertEquals(new BigDecimal("200"), result.getEstoqueMinimo());
        assertEquals(BigDecimal.ZERO, result.getSaldo());
        assertEquals(UnidadeMedida.G, result.getUnidade());
    }

    @Test
    void atualizarComEstoqueMinimoNuloPreservaOValorENuncaAlteraOSaldo() {
        naEquipe("user-1", "eq-1");
        Material material = materialPla();
        material.setUnidade(UnidadeMedida.G);
        material.setSaldo(new BigDecimal("350"));
        material.setEstoqueMinimo(new BigDecimal("200"));

        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PLA Premium");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.24);
        dto.setPrecoPorGrama(new BigDecimal("0.07"));

        when(repository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(material));
        when(repository.save(material)).thenReturn(material);

        MaterialResponseDTO result = service.atualizar("m-1", dto, "user-1");

        assertEquals("PLA Premium", result.getNome());
        assertEquals(new BigDecimal("200"), result.getEstoqueMinimo());
        assertEquals(new BigDecimal("350"), result.getSaldo());
    }

    @Test
    void listarAtivosDeveRetornarSoOsDaEquipe() {
        naEquipe("user-1", "eq-1");
        when(repository.findByEquipeIdAndAtivoTrue("eq-1")).thenReturn(List.of(materialPla()));

        List<MaterialResponseDTO> result = service.listarAtivos("user-1");

        assertEquals(1, result.size());
        assertEquals("PLA", result.get(0).getNome());
    }

    @Test
    void listarAtivosSemEquipeDeveRetornarVazio() {
        semEquipe("user-1");

        assertTrue(service.listarAtivos("user-1").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void buscarPorIdDeveRetornarMaterialDaEquipe() {
        naEquipe("user-1", "eq-1");
        when(repository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(materialPla()));

        MaterialResponseDTO result = service.buscarPorId("m-1", "user-1");

        assertEquals("PLA", result.getNome());
    }

    @Test
    void materialDeOutraEquipeNaoEEncontrado() {
        naEquipe("user-9", "eq-2");

        RuntimeException aoBuscar = assertThrows(RuntimeException.class,
                () -> service.buscarPorId("m-1", "user-9"));
        RuntimeException aoAtualizar = assertThrows(RuntimeException.class,
                () -> service.atualizar("m-1", dtoPla(), "user-9"));
        RuntimeException aoInativar = assertThrows(RuntimeException.class,
                () -> service.inativar("m-1", "user-9"));

        assertEquals("Material não encontrado", aoBuscar.getMessage());
        assertEquals("Material não encontrado", aoAtualizar.getMessage());
        assertEquals("Material não encontrado", aoInativar.getMessage());
        verify(repository, times(3)).findByIdAndEquipeId("m-1", "eq-2");
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarDeveAplicarDados() {
        naEquipe("user-1", "eq-1");
        Material material = materialPla();

        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PETG");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.27);
        dto.setPrecoPorGrama(new BigDecimal("0.08"));
        dto.setAtivo(true);

        when(repository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(material));
        when(repository.save(material)).thenReturn(material);

        MaterialResponseDTO result = service.atualizar("m-1", dto, "user-1");

        assertEquals("PETG", result.getNome());
        assertEquals(new BigDecimal("0.08"), result.getPrecoPorGrama());
    }

    @Test
    void inativarDeveMarcarComoInativo() {
        naEquipe("user-1", "eq-1");
        Material material = materialPla();

        when(repository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(material));

        service.inativar("m-1", "user-1");

        assertFalse(material.getAtivo());
        verify(repository).save(material);
    }

    @Test
    void escritaSemEquipeDeveSerRecusada() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.atualizar("m-1", dtoPla(), "user-1"));
        assertThrows(SemEquipeException.class, () -> service.inativar("m-1", "user-1"));
        verifyNoInteractions(repository);
    }

    @Test
    void clienteLeMaterialDoProprioPedido() {
        // cliente não tem equipe: só lê o material de um pedido em que ele é o cliente
        semEquipe("cliente-1");
        when(repository.findById("m-1")).thenReturn(Optional.of(materialPla()));
        when(pedidoRepository.existsByEquipeIdAndClienteIdAndMaterialId("eq-1", "cliente-1", "m-1"))
                .thenReturn(true);

        assertEquals("PLA", service.buscarPorId("m-1", "cliente-1").getNome());
    }

    @Test
    void clienteNaoLeMaterialSemPedidoDele() {
        semEquipe("cliente-1");
        when(repository.findById("m-1")).thenReturn(Optional.of(materialPla()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.buscarPorId("m-1", "cliente-1"));

        assertEquals("Material não encontrado", ex.getMessage());
    }
}
