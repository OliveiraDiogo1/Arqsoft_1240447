package pt.psoft.g1.psoftg1.bibliographicmanagement.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BibliographicProperties.class)
public class BibliographicConfiguration {
}
