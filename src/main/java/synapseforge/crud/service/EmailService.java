package synapseforge.crud.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.mail.from}")
    private String mailFrom;

    public void enviarConfirmacaoCadastro(String destinatario, String nome, String token) {
        String link = appUrl + "/confirmar-email?token=" + token;
        String html = buildHtml(
            "Confirme seu email",
            "Olá, " + nome + "!",
            "Sua conta na SynapseForge foi criada com sucesso. Clique no botão abaixo para confirmar seu email e acessar sua conta.",
            link,
            "Confirmar email",
            "Este link expira em 24 horas."
        );
        enviar(destinatario, "Confirme seu email – SynapseForge", html);
    }

    public void enviarConfirmacaoMudancaEmail(String destinatario, String nome, String token) {
        String link = appUrl + "/confirmar-mudanca-email?token=" + token;
        String html = buildHtml(
            "Confirme seu novo email",
            "Olá, " + nome + "!",
            "Recebemos uma solicitação para alterar o email da sua conta SynapseForge para <strong>" + destinatario + "</strong>. Clique abaixo para confirmar.",
            link,
            "Confirmar novo email",
            "Se você não solicitou isso, ignore este email. O link expira em 1 hora."
        );
        enviar(destinatario, "Confirme seu novo email – SynapseForge", html);
    }

    private void enviar(String destinatario, String assunto, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar email: " + e.getMessage());
        }
    }

    private String buildHtml(String titulo, String saudacao, String corpo, String link, String botaoTexto, String rodape) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f2f2f6;font-family:'Inter',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 16px;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(31,26,30,.08);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#FB4A14,#e04310);padding:32px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;">SynapseForge</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px;">
                        <h2 style="margin:0 0 8px;color:#1F1A1E;font-size:20px;">%s</h2>
                        <p style="margin:0 0 16px;color:#434656;font-size:15px;">%s</p>
                        <p style="margin:0 0 28px;color:#434656;font-size:15px;line-height:1.6;">%s</p>
                        <div style="text-align:center;margin-bottom:28px;">
                          <a href="%s" style="display:inline-block;padding:14px 32px;background:linear-gradient(135deg,#FB4A14,#e04310);color:#ffffff;text-decoration:none;border-radius:9999px;font-weight:600;font-size:15px;">%s</a>
                        </div>
                        <p style="margin:0;color:#888;font-size:13px;text-align:center;">%s</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(saudacao, titulo, corpo, link, botaoTexto, rodape);
    }
}
