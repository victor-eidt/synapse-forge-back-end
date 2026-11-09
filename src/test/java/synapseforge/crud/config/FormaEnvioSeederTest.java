package synapseforge.crud.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.infrastructure.entity.FormaEnvio;
import synapseforge.crud.infrastructure.repository.FormaEnvioRepository;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class FormaEnvioSeederTest {

    @Mock
    private FormaEnvioRepository repository;

    @InjectMocks
    private FormaEnvioSeeder seeder;

    @Test
    @SuppressWarnings("unchecked")
    void colecaoVaziaRecebeAsFormasPadraoSemEquipe() {
        when(repository.count()).thenReturn(0L);

        seeder.run();

        ArgumentCaptor<List<FormaEnvio>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<FormaEnvio> formas = captor.getValue();

        assertEquals(List.of("Correios PAC", "Correios SEDEX", "Transportadora", "Retirada no ateliê"),
                formas.stream().map(FormaEnvio::getNome).toList());
        assertTrue(formas.stream().allMatch(f -> f.getEquipeId() == null && Boolean.TRUE.equals(f.getAtivo())));
        // só os Correios têm rastreio pelo modelo de URL
        assertEquals(FormaEnvioSeeder.URL_RASTREIO_CORREIOS, formas.get(0).getUrlRastreioTemplate());
        assertEquals(FormaEnvioSeeder.URL_RASTREIO_CORREIOS, formas.get(1).getUrlRastreioTemplate());
        assertNull(formas.get(2).getUrlRastreioTemplate());
        assertNull(formas.get(3).getUrlRastreioTemplate());
    }

    @Test
    void colecaoComDadosNaoERetocada() {
        when(repository.count()).thenReturn(4L);

        seeder.run();

        verify(repository, never()).saveAll(any());
        verify(repository, never()).save(any());
    }
}
