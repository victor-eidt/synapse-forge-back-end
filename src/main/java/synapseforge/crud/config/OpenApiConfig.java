package synapseforge.crud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Texto da página inicial do Swagger (aceita Markdown)
    private static final String DESCRICAO = """
            API do **Synapse Forge**, sistema de gestão para oficinas de impressão 3D e pintura:
            orçamentos, pedidos e suas etapas de produção, ordens de pintura, paleta de cores,
            estoque de insumos, agenda, equipes e notificações.

            ### Como autenticar
            1. Faça login em `POST /auth/login` com um usuário cadastrado e com o e-mail confirmado.
            2. Copie o `access_token` da resposta.
            3. Clique em **Authorize** e cole o token. As demais rotas passam a enviar o cabeçalho `Authorization: Bearer <token>`.

            ### Papéis
            - **CLIENTE**: vê os próprios pedidos e avisos.
            - **TECNICO**: trabalha nos pedidos e ordens de pintura da equipe.
            - **GERENTE**: tudo do técnico, mais orçamentos e gestão da equipe.
            - **ADMIN**: painel de administração com acesso a todas as equipes.

            Os dados são isolados por equipe: técnico e gerente só enxergam registros da própria oficina.

            ### Códigos de resposta
            - **400**: regra de negócio ou validação (a mensagem explica o motivo). Registro inexistente
              ou de outra equipe também costuma voltar 400.
            - **401**: token ausente, inválido ou expirado.
            - **403**: papel sem permissão, ou usuário sem equipe.
            - **404**: recurso não encontrado (comentários, notificações e arquivos).
            - **422**: estoque insuficiente para avançar a etapa ou ajustar o saldo.
            """;

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Synapse Forge API")
                        .version("1.0.0")
                        .description(DESCRICAO)
                        .contact(new Contact()
                                .name("Equipe Synapse Forge")
                                .url("https://github.com/victor-eidt/synapse-forge-back-end")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Informe o token JWT obtido no login da API.")));
    }
}
