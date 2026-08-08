package dev.learninginfra.configuracao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConfiguracaoDeTempo {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
