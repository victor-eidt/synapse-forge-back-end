package synapseforge.crud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita @Async. Usa o executor padrão do Spring Boot (applicationTaskExecutor).
 * Serve para tirar o SMTP do caminho da requisição: um servidor de email lento
 * não pode segurar a resposta de quem avançou a etapa do pedido.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
