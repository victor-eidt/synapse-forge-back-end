package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.FormaEnvio;
import synapseforge.crud.infrastructure.repository.FormaEnvioRepository;

import java.util.List;

/**
 * Formas de envio padrão do sistema, criadas só quando a coleção formas_envio está vazia.
 * Sem equipe (equipeId nulo): valem para todas as oficinas. Roda em todo ambiente, pois
 * são dados de referência do produto, não dados de teste (por isso não depende do
 * DataInitializer nem de app.seed.enabled).
 */
@Component
@RequiredArgsConstructor
public class FormaEnvioSeeder implements CommandLineRunner {

    static final String URL_RASTREIO_CORREIOS =
            "https://rastreamento.correios.com.br/app/index.php?objeto=" + FormaEnvio.MARCADOR_CODIGO;

    private final FormaEnvioRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                padrao("Correios PAC", "Correios", URL_RASTREIO_CORREIOS),
                padrao("Correios SEDEX", "Correios", URL_RASTREIO_CORREIOS),
                padrao("Transportadora", null, null),
                padrao("Retirada no ateliê", null, null)
        ));
    }

    private static FormaEnvio padrao(String nome, String transportadora, String urlRastreioTemplate) {
        FormaEnvio forma = new FormaEnvio();
        forma.setNome(nome);
        forma.setTransportadora(transportadora);
        forma.setUrlRastreioTemplate(urlRastreioTemplate);
        forma.setAtivo(true);
        return forma;
    }
}
