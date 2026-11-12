package pe.gob.valdizan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaRepositories(
        basePackages = "pe.gob.valdizan.repository",
        considerNestedRepositories = true
)
public class ValdizanApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        SpringApplication.run(ValdizanApplication.class, args);
    }
}
