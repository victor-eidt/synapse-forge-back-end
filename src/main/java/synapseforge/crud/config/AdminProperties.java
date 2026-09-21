package synapseforge.crud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AdminProperties {

    private final Set<String> emailsAutorizados;

    public AdminProperties(
            @Value("${app.admin.emails:}") String emails
    ) {
        this.emailsAutorizados = Arrays.stream(
                        emails.split(",")
                )
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public boolean emailAutorizado(String email) {
        if (email == null) {
            return false;
        }

        return emailsAutorizados.contains(
                email.trim().toLowerCase()
        );
    }
}
