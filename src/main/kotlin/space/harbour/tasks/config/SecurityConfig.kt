package space.harbour.tasks.config

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

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    // Allow H2 console access (development only)
                    .requestMatchers("/h2-console/**").permitAll()
                    // Allow actuator health endpoint
                    .requestMatchers("/actuator/health").permitAll()
                    // Require authentication for API endpoints
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .httpBasic { }
            .csrf { csrf ->
                // Disable CSRF for H2 console
                csrf.ignoringRequestMatchers("/h2-console/**")
            }
            .headers { headers ->
                // Allow frames for H2 console
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }

        return http.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        val user = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("password123"))
            .roles("USER")
            .build()

        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
