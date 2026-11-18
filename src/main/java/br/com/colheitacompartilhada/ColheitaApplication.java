package br.com.colheitacompartilhada;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ColheitaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColheitaApplication.class, args);
    }
}