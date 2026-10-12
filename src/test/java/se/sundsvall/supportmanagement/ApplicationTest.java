package se.sundsvall.supportmanagement;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A test of the whole application on the junit profile, without a web server. The repositories below are wrapped in
 * spies that call the real beans until a test stubs them, and are reset after each test. A test reaches a spy by
 * autowiring its type.
 * <p>
 * Every class carrying it shares one application context. A class that adds an override of its own gets a context of
 * its own instead.
 */
@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@MockitoSpyBean(types = {
	ErrandProcessRepository.class,
	ProcessEventOutboxRepository.class
})
public @interface ApplicationTest {
}
