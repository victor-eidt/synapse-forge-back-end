package synapseforge.crud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.auto-index-creation=true",
    "de.flapdoodle.mongodb.embedded.version=7.0.0"
})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
