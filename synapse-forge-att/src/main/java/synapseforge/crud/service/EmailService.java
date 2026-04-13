package synapseforge.crud.service;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Value("${mailtrap.api.token}")
    private String apiToken;

    @Value("${mailtrap.from.email}")
    private String fromEmail;

    @Value("${mailtrap.from.name}")
    private String fromName;

    public void enviarResetSenha(String destinatario, String token) {
        MailtrapConfig config = new MailtrapConfig.Builder()
                .token(apiToken)
                .build();

        MailtrapClient client = MailtrapClientFactory.createMailtrapClient(config);

        String corpo = "Você solicitou a redefinição de senha.\n\n"
                + "Use o token abaixo para redefinir sua senha (válido por 1 hora):\n\n"
                + token + "\n\n"
                + "Se não foi você, ignore este e-mail.";

        MailtrapMail mail = MailtrapMail.builder()
                .from(new Address(fromEmail, fromName))
                .to(List.of(new Address(destinatario)))
                .subject("Redefinição de senha - Synapse Forge")
                .text(corpo)
                .build();

        try {
            client.send(mail);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}
