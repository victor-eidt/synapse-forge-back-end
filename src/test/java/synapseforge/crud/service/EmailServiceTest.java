package synapseforge.crud.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @InjectMocks
    EmailService service;

    @Test
    void enviarConfirmacaoCadastro_should_call_mailSender() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(msg);

        // set properties used by EmailService
        service = new EmailService(mailSender);
        // using reflection to set private fields appUrl and mailFrom
        java.lang.reflect.Field f1 = EmailService.class.getDeclaredField("appUrl");
        f1.setAccessible(true); f1.set(service, "http://app.local");
        java.lang.reflect.Field f2 = EmailService.class.getDeclaredField("mailFrom");
        f2.setAccessible(true); f2.set(service, "no-reply@sf.com");

        service.enviarConfirmacaoCadastro("dest@d.com", "Nome", "token-123");

        verify(mailSender).send(any(MimeMessage.class));
    }
    @Test
    void enviarPedidoFinalizado_deveEscaparProjetoEApontarParaOPedido() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(msg);

        service = new EmailService(mailSender);
        java.lang.reflect.Field f1 = EmailService.class.getDeclaredField("appUrl");
        f1.setAccessible(true); f1.set(service, "http://app.local");
        java.lang.reflect.Field f2 = EmailService.class.getDeclaredField("mailFrom");
        f2.setAccessible(true); f2.set(service, "no-reply@sf.com");

        service.enviarPedidoFinalizado("cli@d.com", "Ana", "<b>Dragão</b>", "abc123def45");

        verify(mailSender).send(msg);
        assertEquals("Seu pedido foi finalizado – SynapseForge", msg.getSubject());

        String corpo = textoHtml(msg.getContent());
        assertTrue(corpo.contains("http://app.local/dashboard?pedido=abc123def45"));
        assertTrue(corpo.contains("#DEF45"));
        assertTrue(corpo.contains("&lt;b&gt;Drag"));
        assertFalse(corpo.contains("<b>Drag"));
    }

    @Test
    void enviarOrdemPinturaAtribuida_deveTrazerProjetoCorPrazoELinkDoQuadro() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(msg);

        service = new EmailService(mailSender);
        java.lang.reflect.Field f1 = EmailService.class.getDeclaredField("appUrl");
        f1.setAccessible(true); f1.set(service, "http://app.local");
        java.lang.reflect.Field f2 = EmailService.class.getDeclaredField("mailFrom");
        f2.setAccessible(true); f2.set(service, "no-reply@sf.com");

        service.enviarOrdemPinturaAtribuida("tec@d.com", "José", "<i>Vaso</i>", "Azul Royal",
                java.time.LocalDate.of(2026, 10, 15));

        verify(mailSender).send(msg);
        assertEquals("Nova ordem de pintura – SynapseForge", msg.getSubject());

        String corpo = textoHtml(msg.getContent());
        assertTrue(corpo.contains("http://app.local/ordens-pintura"));
        assertTrue(corpo.contains("Azul Royal"));
        assertTrue(corpo.contains("15/10/2026"));
        assertTrue(corpo.contains("&lt;i&gt;Vaso"));
        assertFalse(corpo.contains("<i>Vaso"));
    }

    // O helper monta multipart (mixed > related > html): desce até achar o texto
    private static String textoHtml(Object conteudo) throws Exception {
        if (conteudo instanceof String texto) {
            return texto;
        }
        jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) conteudo;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            sb.append(textoHtml(multipart.getBodyPart(i).getContent()));
        }
        return sb.toString();
    }
}
