package com.example.barberrecods.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    private final AppProperties appProperties

    SecurityConfig(AppProperties appProperties) {
        this.appProperties = appProperties
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        new BCryptPasswordEncoder()
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        new InMemoryUserDetailsManager(
                User.withUsername(appProperties.admin.username)
                        .password(passwordEncoder.encode(appProperties.admin.password))
                        .roles('ADMIN')
                        .build()
        )
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests { auth ->
                    auth
                            .requestMatchers('/', '/api/**', '/css/**', '/js/**', '/admin/login').permitAll()
                            .requestMatchers('/admin/**').hasRole('ADMIN')
                            .anyRequest().authenticated()
                }
                .formLogin { form ->
                    form
                            .loginPage('/admin/login')
                            .loginProcessingUrl('/admin/login')
                            .defaultSuccessUrl('/admin', true)
                            .failureUrl('/admin/login?error')
                            .permitAll()
                }
                .logout { logout ->
                    logout
                            .logoutUrl('/admin/logout')
                            .logoutSuccessUrl('/')
                            .permitAll()
                }
                .csrf { csrf ->
                    csrf.ignoringRequestMatchers('/api/**')
                }

        http.build()
    }
}
