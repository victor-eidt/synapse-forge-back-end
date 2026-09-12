package synapseforge.crud.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.mail.from}")
    private String mailFrom;


    // =========================================================
    // CONFIRMAÇÃO DE CADASTRO
    // =========================================================

    public void enviarConfirmacaoCadastro(
            String destinatario,
            String nome,
            String token
    ) {

        String link =
                appUrl + "/confirmar-email?token=" +
                        URLEncoder.encode(
                                token,
                                StandardCharsets.UTF_8
                        );

        String html = buildHtml(
                "Confirme seu email",
                "Olá, " + nome + "!",
                "Sua conta na SynapseForge foi criada com sucesso. "
                        + "Clique no botão abaixo para confirmar seu email "
                        + "e acessar sua conta.",
                link,
                "Confirmar email",
                "Este link expira em 24 horas."
        );

        enviar(
                destinatario,
                "Confirme seu email – SynapseForge",
                html
        );
    }


    // =========================================================
    // MUDANÇA DE EMAIL
    // =========================================================

    public void enviarConfirmacaoMudancaEmail(
            String destinatario,
            String nome,
            String token
    ) {

        String link =
                appUrl + "/confirmar-mudanca-email?token=" +
                        URLEncoder.encode(
                                token,
                                StandardCharsets.UTF_8
                        );

        String html = buildHtml(
                "Confirme seu novo email",
                "Olá, " + nome + "!",
                "Recebemos uma solicitação para alterar o email "
                        + "da sua conta SynapseForge para <strong>"
                        + destinatario
                        + "</strong>. Clique abaixo para confirmar.",
                link,
                "Confirmar novo email",
                "Se você não solicitou isso, ignore este email. "
                        + "O link expira em 1 hora."
        );

        enviar(
                destinatario,
                "Confirme seu novo email – SynapseForge",
                html
        );
    }


    // =========================================================
    // RECUPERAÇÃO DE SENHA
    // =========================================================

    public void enviarRecuperacaoSenha(
            String destinatario,
            String nome,
            String token
    ) {

        String link =
                appUrl + "/redefinir-senha?token="
                        + URLEncoder.encode(
                        token,
                        StandardCharsets.UTF_8
                )
                        + "&email="
                        + URLEncoder.encode(
                        destinatario,
                        StandardCharsets.UTF_8
                );

        String html = buildHtml(
                "Redefinição de senha",
                "Olá, " + nome + "!",
                "Recebemos uma solicitação para redefinir a senha "
                        + "da sua conta SynapseForge. Clique no botão "
                        + "abaixo para escolher uma nova senha.",
                link,
                "Redefinir senha",
                "Se você não solicitou isso, ignore este email. "
                        + "O link expira em 1 hora."
        );

        enviar(
                destinatario,
                "Redefinição de senha – SynapseForge",
                html
        );
    }


    // =========================================================
    // CONVITE PARA EQUIPE
    // =========================================================

    public void enviarConviteEquipe(
            String destinatario,
            String nomeUsuario,
            String nomeGerente,
            String nomeEquipe,
            String token
    ) {

        String baseLink =
                appUrl
                        + "/convite-equipe?token="
                        + URLEncoder.encode(
                        token,
                        StandardCharsets.UTF_8
                );

        String linkAceitar =
                baseLink + "&acao=aceitar";

        String linkRecusar =
                baseLink + "&acao=recusar";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Convite para equipe</title>
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f2f2f6;
                    font-family:Arial,sans-serif;
                ">

                    <table
                        width="100%%"
                        cellpadding="0"
                        cellspacing="0"
                    >
                        <tr>
                            <td
                                align="center"
                                style="padding:40px 16px;"
                            >

                                <table
                                    width="100%%"
                                    cellpadding="0"
                                    cellspacing="0"
                                    style="
                                        max-width:520px;
                                        background:#ffffff;
                                        border-radius:16px;
                                        overflow:hidden;
                                        box-shadow:
                                            0 4px 24px
                                            rgba(31,26,30,.08);
                                    "
                                >

                                    <!-- HEADER -->

                                    <tr>
                                        <td style="
                                            background:
                                                linear-gradient(
                                                    135deg,
                                                    #FB4A14,
                                                    #e04310
                                                );
                                            padding:32px;
                                            text-align:center;
                                        ">

                                            <h1 style="
                                                margin:0;
                                                color:#ffffff;
                                                font-size:22px;
                                            ">
                                                SynapseForge
                                            </h1>

                                        </td>
                                    </tr>


                                    <!-- CONTENT -->

                                    <tr>
                                        <td style="
                                            padding:36px 32px;
                                        ">

                                            <h2 style="
                                                margin:0 0 12px;
                                                color:#1F1A1E;
                                                font-size:22px;
                                            ">
                                                Convite para equipe
                                            </h2>

                                            <p style="
                                                margin:0 0 16px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.6;
                                            ">
                                                Olá, %s!
                                            </p>

                                            <p style="
                                                margin:0 0 16px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.6;
                                            ">
                                                Você recebeu um convite para
                                                fazer parte da equipe
                                                <strong>%s</strong>.
                                            </p>

                                            <p style="
                                                margin:0 0 28px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.6;
                                            ">
                                                Convite enviado por:
                                                <strong>%s</strong>.
                                            </p>


                                            <!-- ACEITAR -->

                                            <div style="
                                                text-align:center;
                                                margin-bottom:14px;
                                            ">

                                                <a
                                                    href="%s"
                                                    style="
                                                        display:inline-block;
                                                        padding:14px 32px;
                                                        background:
                                                            linear-gradient(
                                                                135deg,
                                                                #FB4A14,
                                                                #e04310
                                                            );
                                                        color:#ffffff;
                                                        text-decoration:none;
                                                        border-radius:9999px;
                                                        font-weight:600;
                                                        font-size:15px;
                                                    "
                                                >
                                                    Aceitar convite
                                                </a>

                                            </div>


                                            <!-- RECUSAR -->

                                            <div style="
                                                text-align:center;
                                                margin-bottom:28px;
                                            ">

                                                <a
                                                    href="%s"
                                                    style="
                                                        display:inline-block;
                                                        padding:12px 28px;
                                                        background:#f2f2f6;
                                                        color:#434656;
                                                        text-decoration:none;
                                                        border-radius:9999px;
                                                        font-weight:600;
                                                        font-size:14px;
                                                    "
                                                >
                                                    Recusar convite
                                                </a>

                                            </div>


                                            <p style="
                                                margin:0;
                                                color:#888;
                                                font-size:13px;
                                                text-align:center;
                                                line-height:1.5;
                                            ">
                                                Este convite expira em 24 horas.
                                            </p>

                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                </body>
                </html>
                """.formatted(
                nomeUsuario,
                nomeEquipe,
                nomeGerente,
                linkAceitar,
                linkRecusar
        );

        enviar(
                destinatario,
                "Convite para equipe – SynapseForge",
                html
        );
    }


    // =========================================================
    // NOTIFICAÇÃO AO GERENTE — CONVITE ACEITO
    // =========================================================

    public void enviarConviteAceito(
            String destinatario,
            String nomeGerente,
            String nomeUsuario,
            String nomeEquipe
    ) {

        String html = buildNotificacaoHtml(
                "Convite aceito",
                "Olá, " + nomeGerente + "!",
                "O usuário <strong>"
                        + nomeUsuario
                        + "</strong> aceitou o convite "
                        + "para fazer parte da equipe "
                        + "<strong>"
                        + nomeEquipe
                        + "</strong>.",
                "O usuário agora faz parte da equipe."
        );

        enviar(
                destinatario,
                "Convite aceito – SynapseForge",
                html
        );
    }


    // =========================================================
    // NOTIFICAÇÃO AO GERENTE — CONVITE RECUSADO
    // =========================================================

    public void enviarConviteRecusado(
            String destinatario,
            String nomeGerente,
            String nomeUsuario,
            String nomeEquipe
    ) {

        String html = buildNotificacaoHtml(
                "Convite recusado",
                "Olá, " + nomeGerente + "!",
                "O usuário <strong>"
                        + nomeUsuario
                        + "</strong> recusou o convite "
                        + "para fazer parte da equipe "
                        + "<strong>"
                        + nomeEquipe
                        + "</strong>.",
                "Nenhuma alteração foi feita na equipe."
        );

        enviar(
                destinatario,
                "Convite recusado – SynapseForge",
                html
        );
    }


    // =========================================================
    // HTML GENÉRICO
    // =========================================================

    private String buildHtml(
            String titulo,
            String saudacao,
            String corpo,
            String link,
            String botaoTexto,
            String rodape
    ) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f2f2f6;
                    font-family:'Inter',Arial,sans-serif;
                ">

                    <table
                        width="100%%"
                        cellpadding="0"
                        cellspacing="0"
                    >

                        <tr>

                            <td
                                align="center"
                                style="padding:40px 16px;"
                            >

                                <table
                                    width="100%%"
                                    cellpadding="0"
                                    cellspacing="0"
                                    style="
                                        max-width:520px;
                                        background:#ffffff;
                                        border-radius:16px;
                                        overflow:hidden;
                                        box-shadow:
                                            0 4px 24px
                                            rgba(31,26,30,.08);
                                    "
                                >

                                    <tr>

                                        <td style="
                                            background:
                                                linear-gradient(
                                                    135deg,
                                                    #FB4A14,
                                                    #e04310
                                                );
                                            padding:32px;
                                            text-align:center;
                                        ">

                                            <h1 style="
                                                margin:0;
                                                color:#ffffff;
                                                font-size:22px;
                                                font-weight:700;
                                            ">
                                                SynapseForge
                                            </h1>

                                        </td>

                                    </tr>


                                    <tr>

                                        <td style="
                                            padding:36px 32px;
                                        ">

                                            <h2 style="
                                                margin:0 0 8px;
                                                color:#1F1A1E;
                                                font-size:20px;
                                            ">
                                                %s
                                            </h2>

                                            <p style="
                                                margin:0 0 16px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.5;
                                            ">
                                                %s
                                            </p>

                                            <p style="
                                                margin:0 0 28px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.6;
                                            ">
                                                %s
                                            </p>

                                            <div style="
                                                text-align:center;
                                                margin-bottom:28px;
                                            ">

                                                <a
                                                    href="%s"
                                                    style="
                                                        display:inline-block;
                                                        padding:14px 32px;
                                                        background:
                                                            linear-gradient(
                                                                135deg,
                                                                #FB4A14,
                                                                #e04310
                                                            );
                                                        color:#ffffff;
                                                        text-decoration:none;
                                                        border-radius:9999px;
                                                        font-weight:600;
                                                        font-size:15px;
                                                    "
                                                >
                                                    %s
                                                </a>

                                            </div>

                                            <p style="
                                                margin:0;
                                                color:#888;
                                                font-size:13px;
                                                text-align:center;
                                            ">
                                                %s
                                            </p>

                                        </td>

                                    </tr>

                                </table>

                            </td>

                        </tr>

                    </table>

                </body>
                </html>
                """
                .formatted(
                        titulo,
                        saudacao,
                        corpo,
                        link,
                        botaoTexto,
                        rodape
                );
    }


    // =========================================================
    // HTML DE NOTIFICAÇÃO
    // =========================================================

    private String buildNotificacaoHtml(
            String titulo,
            String saudacao,
            String corpo,
            String rodape
    ) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f2f2f6;
                    font-family:Arial,sans-serif;
                ">

                    <table
                        width="100%%"
                        cellpadding="0"
                        cellspacing="0"
                    >

                        <tr>

                            <td
                                align="center"
                                style="padding:40px 16px;"
                            >

                                <table
                                    width="100%%"
                                    cellpadding="0"
                                    cellspacing="0"
                                    style="
                                        max-width:520px;
                                        background:#ffffff;
                                        border-radius:16px;
                                        overflow:hidden;
                                        box-shadow:
                                            0 4px 24px
                                            rgba(31,26,30,.08);
                                    "
                                >

                                    <tr>

                                        <td style="
                                            background:
                                                linear-gradient(
                                                    135deg,
                                                    #FB4A14,
                                                    #e04310
                                                );
                                            padding:32px;
                                            text-align:center;
                                        ">

                                            <h1 style="
                                                margin:0;
                                                color:#ffffff;
                                                font-size:22px;
                                            ">
                                                SynapseForge
                                            </h1>

                                        </td>

                                    </tr>


                                    <tr>

                                        <td style="
                                            padding:36px 32px;
                                        ">

                                            <h2 style="
                                                margin:0 0 12px;
                                                color:#1F1A1E;
                                                font-size:20px;
                                            ">
                                                %s
                                            </h2>

                                            <p style="
                                                margin:0 0 16px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.6;
                                            ">
                                                %s
                                            </p>

                                            <p style="
                                                margin:0 0 28px;
                                                color:#434656;
                                                font-size:15px;
                                                line-height:1.6;
                                            ">
                                                %s
                                            </p>

                                            <p style="
                                                margin:0;
                                                color:#888;
                                                font-size:13px;
                                                text-align:center;
                                            ">
                                                %s
                                            </p>

                                        </td>

                                    </tr>

                                </table>

                            </td>

                        </tr>

                    </table>

                </body>
                </html>
                """
                .formatted(
                        titulo,
                        saudacao,
                        corpo,
                        rodape
                );
    }


    // =========================================================
    // ENVIO DO EMAIL
    // =========================================================

    private void enviar(
            String destinatario,
            String assunto,
            String html
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(mailFrom);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Falha ao enviar email: "
                            + e.getMessage()
            );
        }
    }
}
