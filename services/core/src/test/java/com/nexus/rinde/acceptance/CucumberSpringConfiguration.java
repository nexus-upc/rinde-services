package com.nexus.rinde.acceptance;

import com.nexus.rinde.subscription.platform.infrastructure.PlatformCompanyDirectory;
import com.nexus.rinde.support.AbstractIntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Levanta el contexto de Spring para Cucumber. Extiende la base de integración para compartir la
 * misma configuración (y el mismo contexto en caché) y apuntar a la base rinde_test.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration extends AbstractIntegrationTest {

  /** Firma de prueba del panel del dueño; no es la de ningún entorno. */
  public static final String PLATFORM_SECRET = "acceptance-platform-signing-secret-1234567890";

  /** El directorio de empresas vive en IAM; los pasos de US44 le indican los nombres. */
  @MockitoBean PlatformCompanyDirectory platformCompanyDirectory;

  @DynamicPropertySource
  static void platformProperties(DynamicPropertyRegistry registry) {
    registry.add("rinde.platform.jwt.secret", () -> PLATFORM_SECRET);
  }
}
