package br.com.colheitacompartilhada.config;

import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http, UsuarioRepository usuarios, ErroJsonWriter erros) throws Exception {
        var csrfHandler = new CsrfTokenRequestAttributeHandler();
        // null = o token é resolvido em toda requisição, então o cookie XSRF-TOKEN está sempre presente para o front.
        csrfHandler.setCsrfRequestAttributeName(null);
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(regras -> regras
                        // "/anuncios/*" não cobre "/anuncios/{id}/contato", que exige login.
                        .requestMatchers(HttpMethod.GET, "/", "/*.html", "/css/**", "/js/**", "/img/**",
                                "/fotos/*", "/csrf", "/categorias", "/bairros",
                                "/anuncios", "/anuncios/*", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios", "/sessao").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint((request, response, ex) -> erros.naoAutenticado(response))
                        .accessDeniedHandler((request, response, ex) -> erros.acessoNegado(response)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // O filtro é criado aqui, sem @Component, para rodar só dentro da cadeia de segurança.
                .addFilterAfter(new AtivoCheckFilter(usuarios, erros), SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }
}
