package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Orcamento;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusOrcamento;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.OrcamentoRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import synapseforge.crud.DTO.Comum.PaginaResponseDTO;
import synapseforge.crud.DTO.Orcamento.FiltroOrcamentoDTO;
import synapseforge.crud.DTO.Orcamento.SituacaoOrcamento;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EquipeContexto equipeContexto;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OrcamentoService service;

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

    private void semEquipe(String usuarioId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenThrow(new SemEquipeException());
    }

    private Material materialPla() {
        Material material = new Material();
        material.setId("m-1");
        material.setEquipeId("eq-1");
        material.setNome("PLA");
        material.setDensidadeGcm3(1.25);
        material.setPrecoPorGrama(new BigDecimal("0.10"));
        material.setAtivo(true);
        return material;
    }

    private CalcularOrcamentoRequestDTO dto() {
        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        dto.setMaterialId("m-1");
        dto.setVolumeCm3(100.0);
        dto.setTempoImpressaoHoras(2.0);
        dto.setTempoMaoDeObraHoras(1.0);
        dto.setCustoMaquinaHora(BigDecimal.valueOf(30));
        dto.setCustoMaoDeObraHora(BigDecimal.valueOf(20));
        dto.setMargemLucro(BigDecimal.valueOf(30));
        return dto;
    }

    private Orcamento orcamento(String equipeId) {
        Orcamento orcamento = new Orcamento();
        orcamento.setId("o-1");
        orcamento.setEquipeId(equipeId);
        orcamento.setUsuarioId("user-2");
        orcamento.setMaterialId("m-1");
        orcamento.setVolumeCm3(100.0);
        orcamento.setTempoImpressaoHoras(2.0);
        orcamento.setTempoMaoDeObraHoras(1.0);
        orcamento.setCustoMaquinaHora(BigDecimal.valueOf(30));
        orcamento.setCustoMaoDeObraHora(BigDecimal.valueOf(20));
        orcamento.setMargemLucro(BigDecimal.valueOf(30));
        orcamento.setCustoMaterial(BigDecimal.valueOf(12.5));
        orcamento.setCustoMaquina(BigDecimal.valueOf(60));
        orcamento.setCustoMaoDeObra(BigDecimal.valueOf(20));
        orcamento.setCustoTotal(BigDecimal.valueOf(92.5));
        orcamento.setPrecoFinal(BigDecimal.valueOf(120.25));
        return orcamento;
    }

    @Test
    void calcularDeveRetornarPreview() {
        naEquipe("user-1", "eq-1");
        when(materialRepository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(materialPla()));

        OrcamentoResponseDTO result = service.calcular(dto(), "user-1");

        assertEquals("PLA", result.getNomeMaterial());
        assertEquals("m-1", result.getMaterialId());
        assertNotNull(result.getCustoTotal());
    }

    @Test
    void calcularComMaterialDeOutraEquipeDeveFalhar() {
        naEquipe("user-9", "eq-2");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.calcular(dto(), "user-9"));

        assertEquals("Material não encontrado", ex.getMessage());
        verify(materialRepository, never()).findById(any());
    }

    @Test
    void salvarDevePersistirOrcamentoNaEquipe() {
        naEquipe("user-1", "eq-1");
        when(materialRepository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(materialPla()));
        when(repository.save(any(Orcamento.class))).thenAnswer(invocation -> {
            Orcamento orcamento = invocation.getArgument(0);
            orcamento.setId("o-1");
            orcamento.setCriadoEm(LocalDateTime.now());
            return orcamento;
        });

        OrcamentoResponseDTO result = service.salvar(dto(), "user-1");

        assertEquals("o-1", result.getId());
        assertEquals("PLA", result.getNomeMaterial());
        verify(repository).save(argThat(o -> "eq-1".equals(o.getEquipeId()) && "user-1".equals(o.getUsuarioId())));
    }

    @Test
    void salvarSemEquipeDeveSerRecusado() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.salvar(dto(), "user-1"));
        verifyNoInteractions(repository, materialRepository);
    }

    @Test
    void listarDeveRetornarOrcamentosDaEquipe() {
        naEquipe("user-1", "eq-1");
        when(repository.findByEquipeIdOrderByCriadoEmDesc("eq-1")).thenReturn(List.of(orcamento("eq-1")));
        when(materialRepository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(materialPla()));

        List<OrcamentoResponseDTO> result = service.listar("user-1");

        assertEquals(1, result.size());
        assertEquals("PLA", result.get(0).getNomeMaterial());
    }

    @Test
    void listarSemEquipeDeveRetornarVazio() {
        semEquipe("user-1");

        assertTrue(service.listar("user-1").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void buscarPorIdDeveRetornarDetalhes() {
        // orçamento criado por outro gerente da mesma equipe
        naEquipe("user-1", "eq-1");
        when(repository.findByIdAndEquipeId("o-1", "eq-1")).thenReturn(Optional.of(orcamento("eq-1")));
        when(materialRepository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(materialPla()));

        OrcamentoResponseDTO result = service.buscarPorId("o-1", "user-1");

        assertEquals("o-1", result.getId());
        assertEquals("PLA", result.getNomeMaterial());
    }

    @Test
    void orcamentoDeOutraEquipeNaoEEncontrado() {
        naEquipe("user-9", "eq-2");

        RuntimeException aoBuscar = assertThrows(RuntimeException.class,
                () -> service.buscarPorId("o-1", "user-9"));
        RuntimeException aoAprovar = assertThrows(RuntimeException.class,
                () -> service.aprovar("o-1", "user-9"));
        RuntimeException aoRejeitar = assertThrows(RuntimeException.class,
                () -> service.rejeitar("o-1", "user-9"));

        assertEquals("Orcamento nao encontrado", aoBuscar.getMessage());
        assertEquals("Orcamento nao encontrado", aoAprovar.getMessage());
        assertEquals("Orcamento nao encontrado", aoRejeitar.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void aprovarDeveCriarPedidoNaMesmaEquipe() {
        naEquipe("user-1", "eq-1");
        when(repository.findByIdAndEquipeId("o-1", "eq-1")).thenReturn(Optional.of(orcamento("eq-1")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId("p-1");
            return pedido;
        });
        when(repository.save(any(Orcamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrcamentoResponseDTO result = service.aprovar("o-1", "user-1");

        assertEquals(StatusOrcamento.APROVADO, result.getStatus());
        assertEquals("p-1", result.getPedidoId());
        verify(pedidoRepository).save(argThat(p -> "eq-1".equals(p.getEquipeId())));
    }

    @Test
    void decisaoSemEquipeDeveSerRecusada() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.aprovar("o-1", "user-1"));
        assertThrows(SemEquipeException.class, () -> service.rejeitar("o-1", "user-1"));
        verifyNoInteractions(repository, pedidoRepository);
    }
    // =========================================================
    // BUSCA PAGINADA
    // =========================================================

    private FiltroOrcamentoDTO filtro(SituacaoOrcamento situacao, String cliente, String projeto, LocalDate de, LocalDate ate) {
        return new FiltroOrcamentoDTO(situacao, cliente, projeto, de, ate);
    }

    @Test
    void buscarSemEquipeDeveRetornarPaginaVaziaSemConsultarOBanco() {
        semEquipe("user-x");

        PaginaResponseDTO<OrcamentoResponseDTO> pagina =
                service.buscar("user-x", filtro(SituacaoOrcamento.PENDENTES, null, null, null, null), 0, 20);

        assertTrue(pagina.getItens().isEmpty());
        assertEquals(0, pagina.getTotal());
        assertFalse(pagina.isTemMais());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void buscarDeveRecusarIntervaloDeDatasInvertido() {
        naEquipe("user-1", "eq-1");

        assertThrows(RuntimeException.class, () -> service.buscar(
                "user-1",
                filtro(SituacaoOrcamento.DECIDIDOS, null, null, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)),
                0, 20));
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void buscarDeveFiltrarPorEquipeTextoLiteralEDataNoBanco() {
        naEquipe("user-1", "eq-1");
        when(mongoTemplate.count(any(Query.class), eq(Orcamento.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Orcamento.class))).thenReturn(List.of());

        service.buscar(
                "user-1",
                filtro(SituacaoOrcamento.DECIDIDOS, " Ana (VIP) ", "vaso", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
                0, 20);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Orcamento.class));
        String consulta = captor.getValue().getQueryObject().toString();

        assertTrue(consulta.contains("equipeId=eq-1"));
        assertTrue(consulta.contains("APROVADO"));
        assertTrue(consulta.contains("REJEITADO"));
        // texto aparado e tratado como literal: o "(" não vira regex
        assertTrue(consulta.contains("\\QAna (VIP)\\E"));
        assertTrue(consulta.contains("\\Qvaso\\E"));
        // "até 30/09" inclui o dia inteiro: limite exclusivo em 01/10
        assertTrue(consulta.contains("2026-09-01T00:00"));
        assertTrue(consulta.contains("2026-10-01T00:00"));
    }

    @Test
    void buscarPendentesDeveIncluirOrcamentosSemStatus() {
        naEquipe("user-1", "eq-1");
        when(mongoTemplate.find(any(Query.class), eq(Orcamento.class))).thenReturn(List.of());

        service.buscar("user-1", filtro(SituacaoOrcamento.PENDENTES, null, null, null, null), 0, 20);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Orcamento.class));
        String consulta = captor.getValue().getQueryObject().toString();

        assertTrue(consulta.contains("PENDENTE"));
        assertTrue(consulta.contains("status=null"));
    }

    @Test
    void buscarDevePaginarNoBancoELimitarOTamanho() {
        naEquipe("user-1", "eq-1");
        when(mongoTemplate.count(any(Query.class), eq(Orcamento.class))).thenReturn(1000L);
        when(mongoTemplate.find(any(Query.class), eq(Orcamento.class))).thenReturn(List.of());

        PaginaResponseDTO<OrcamentoResponseDTO> pagina =
                service.buscar("user-1", filtro(SituacaoOrcamento.DECIDIDOS, null, null, null, null), 2, 500);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Orcamento.class));

        assertEquals(100, captor.getValue().getLimit());
        assertEquals(200, captor.getValue().getSkip());
        assertEquals(100, pagina.getTamanho());
        assertEquals(1000, pagina.getTotal());
        assertTrue(pagina.isTemMais());
    }

    @Test
    void buscarNaUltimaPaginaNaoDeveIndicarMais() {
        naEquipe("user-1", "eq-1");
        when(mongoTemplate.count(any(Query.class), eq(Orcamento.class))).thenReturn(45L);
        when(mongoTemplate.find(any(Query.class), eq(Orcamento.class))).thenReturn(List.of());

        PaginaResponseDTO<OrcamentoResponseDTO> pagina =
                service.buscar("user-1", filtro(SituacaoOrcamento.DECIDIDOS, null, null, null, null), 2, 20);

        assertFalse(pagina.isTemMais());
    }

    @Test
    void buscarDeveResolverNomesDosMateriaisNumaConsultaSo() {
        naEquipe("user-1", "eq-1");
        Orcamento a = new Orcamento();
        a.setId("o-1");
        a.setEquipeId("eq-1");
        a.setMaterialId("m-1");
        Orcamento b = new Orcamento();
        b.setId("o-2");
        b.setEquipeId("eq-1");
        b.setMaterialId("m-1");
        when(mongoTemplate.count(any(Query.class), eq(Orcamento.class))).thenReturn(2L);
        when(mongoTemplate.find(any(Query.class), eq(Orcamento.class))).thenReturn(List.of(a, b));
        when(materialRepository.findByEquipeIdAndIdIn(eq("eq-1"), any())).thenReturn(List.of(materialPla()));

        PaginaResponseDTO<OrcamentoResponseDTO> pagina =
                service.buscar("user-1", filtro(SituacaoOrcamento.PENDENTES, null, null, null, null), 0, 20);

        assertEquals(2, pagina.getItens().size());
        verify(materialRepository, times(1)).findByEquipeIdAndIdIn(eq("eq-1"), any());
        verify(materialRepository, never()).findByIdAndEquipeId(any(), any());
    }
}
