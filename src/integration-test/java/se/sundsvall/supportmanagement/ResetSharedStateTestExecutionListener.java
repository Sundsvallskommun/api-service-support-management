package se.sundsvall.supportmanagement;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Resets what the application keeps between test classes before each one: it empties the caches and closes the circuit
 * breakers. Test classes share one application context, so without this a class would see what the classes before it
 * cached, or find a circuit breaker opened by their failure cases, and skip the calls its stubs expect.
 */
public class ResetSharedStateTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public void beforeTestClass(final TestContext testContext) {
		final var applicationContext = testContext.getApplicationContext();
		applicationContext.getBeansOfType(CacheManager.class).values()
			.forEach(cacheManager -> cacheManager.getCacheNames().forEach(name -> Optional.ofNullable(cacheManager.getCache(name)).ifPresent(Cache::clear)));
		applicationContext.getBeanProvider(CircuitBreakerRegistry.class)
			.ifAvailable(registry -> registry.getAllCircuitBreakers().forEach(CircuitBreaker::reset));
	}
}
