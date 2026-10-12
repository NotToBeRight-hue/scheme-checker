package in.project.schemes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude=org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
public class SchemeSelectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemeSelectorApplication.class, args);
    }
}
