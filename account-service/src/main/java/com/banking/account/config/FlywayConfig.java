package com.banking.account.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }

    // EntityManagerFactory'nin flyway bean'inden SONRA yaratılmasını garantiler
    @Bean
    public static BeanFactoryPostProcessor flywayJpaDependencyPostProcessor() {
        return beanFactory -> {
            if (beanFactory instanceof BeanDefinitionRegistry registry) {
                for (String name : beanFactory.getBeanDefinitionNames()) {
                    BeanDefinition def = registry.getBeanDefinition(name);
                    if (name.equals("entityManagerFactory")) {
                        String[] existing = def.getDependsOn();
                        String[] updated = existing == null
                                ? new String[]{"flyway"}
                                : Arrays.copyOf(existing, existing.length + 1);
                        if (existing != null) updated[existing.length] = "flyway";
                        def.setDependsOn(updated);
                    }
                }
            }
        };
    }
}
