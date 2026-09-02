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
}
