package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.FormaEnvio.FormaEnvioRequestDTO;
import synapseforge.crud.DTO.FormaEnvio.FormaEnvioResponseDTO;
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.FormaEnvio;
import synapseforge.crud.infrastructure.repository.FormaEnvioRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class FormaEnvioServiceTest {

    private static final String URL_CORREIOS =
            "https://rastreamento.correios.com.br/app/index.php?objeto={codigo}";

    @Mock
    private FormaEnvioRepository repository;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private FormaEnvioService service;

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

    private void semEquipe(String usuarioId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenThrow(new SemEquipeException());
    }

    private static FormaEnvio forma(String id, String equipeId, String nome, String url) {
        FormaEnvio forma = new FormaEnvio();
        forma.setId(id);
        forma.setEquipeId(equipeId);
        forma.setNome(nome);
        forma.setTransportadora("Correios");
        forma.setUrlRastreioTemplate(url);
        forma.setAtivo(true);
        return forma;
    }

    @Test
    void listarAtivasTrazPadraoDoSistemaEDaEquipe() {
        naEquipe("user-1", "eq-1");
        when(repository.findAtivasDisponiveisPara("eq-1")).thenReturn(List.of(
                forma("f-pac", null, "Correios PAC", URL_CORREIOS),
                forma("f-moto", "eq-1", "Motoboy", null)));

        List<FormaEnvioResponseDTO> formas = service.listarAtivas("user-1");

        assertEquals(2, formas.size());
        assertTrue(formas.get(0).isPadrao());
        assertEquals("Correios PAC", formas.get(0).getNome());
        assertFalse(formas.get(1).isPadrao());
        assertEquals("Motoboy", formas.get(1).getNome());
    }

    @Test
    void usuarioSemEquipeVeSoAsFormasPadrao() {
        semEquipe("cliente-1");
        when(repository.findByAtivoTrueAndEquipeIdIsNull())
                .thenReturn(List.of(forma("f-pac", null, "Correios PAC", URL_CORREIOS)));

        List<FormaEnvioResponseDTO> formas = service.listarAtivas("cliente-1");

        assertEquals(1, formas.size());
        assertTrue(formas.get(0).isPadrao());
        verify(repository, never()).findAtivasDisponiveisPara(any());
    }

    @Test
    void criarGravaNaEquipeDoUsuarioELimpaCamposVazios() {
        naEquipe("user-1", "eq-1");
        when(repository.save(any(FormaEnvio.class))).thenAnswer(inv -> inv.getArgument(0));

        FormaEnvioRequestDTO dto = new FormaEnvioRequestDTO();
        dto.setNome("  Motoboy  ");
        dto.setTransportadora("   ");
        dto.setUrlRastreioTemplate("");

        FormaEnvioResponseDTO criada = service.criar(dto, "user-1");

        ArgumentCaptor<FormaEnvio> captor = ArgumentCaptor.forClass(FormaEnvio.class);
        verify(repository).save(captor.capture());
        assertEquals("eq-1", captor.getValue().getEquipeId());
        assertEquals("Motoboy", captor.getValue().getNome());
        assertNull(captor.getValue().getTransportadora());
        assertNull(captor.getValue().getUrlRastreioTemplate());
        assertTrue(captor.getValue().getAtivo());
        assertFalse(criada.isPadrao());
    }

    @Test
    void criarSemEquipeERecusado() {
        semEquipe("semEquipe");
        FormaEnvioRequestDTO dto = new FormaEnvioRequestDTO();
        dto.setNome("Motoboy");

        assertThrows(SemEquipeException.class, () -> service.criar(dto, "semEquipe"));
        verify(repository, never()).save(any());
    }

    @Test
    void buscarDisponivelDevolveFormaDaEquipeOuPadrao() {
        FormaEnvio pac = forma("f-pac", null, "Correios PAC", URL_CORREIOS);
        when(repository.findAtivaDisponivelPara("f-pac", "eq-1")).thenReturn(Optional.of(pac));

        assertSame(pac, service.buscarDisponivel("f-pac", "eq-1"));
    }

    @Test
    void formaInexistenteOuDeOutraEquipeDa404() {
        when(repository.findAtivaDisponivelPara("f-outra", "eq-1")).thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.buscarDisponivel("f-outra", "eq-1"));
        assertEquals("Forma de envio não encontrada", erro.getMessage());
    }

    @Test
    void buscarPorIdNuloOuVazioNaoConsultaOBanco() {
        assertTrue(service.buscarPorId(null).isEmpty());
        assertTrue(service.buscarPorId("  ").isEmpty());
        verify(repository, never()).findById(any());
    }

    @Test
    void montarLinkRastreioSubstituiOCodigoNoModelo() {
        FormaEnvio pac = forma("f-pac", null, "Correios PAC", URL_CORREIOS);

        assertEquals("https://rastreamento.correios.com.br/app/index.php?objeto=AA123456789BR",
                pac.montarLinkRastreio(" AA123456789BR "));
    }

    @Test
    void formaSemRastreioOuSemCodigoNaoTemLink() {
        FormaEnvio retirada = forma("f-ret", null, "Retirada no ateliê", null);
        FormaEnvio pac = forma("f-pac", null, "Correios PAC", URL_CORREIOS);

        assertNull(retirada.montarLinkRastreio("AA123456789BR"));
        assertNull(pac.montarLinkRastreio(null));
        assertNull(pac.montarLinkRastreio("   "));
    }
}
