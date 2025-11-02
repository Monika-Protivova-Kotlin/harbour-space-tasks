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

/**
 * Security Configuration for the application using Spring Security.
 *
 * This class configures:
 * 1. Authentication - who can access the system (username/password)
 * 2. Authorization - what authenticated users can access
 * 3. Password encryption - how passwords are stored securely
 * 4. HTTP security settings - CSRF protection, headers, etc.
 *
 * @Configuration: Tells Spring this is a configuration class that defines beans
 * @EnableWebSecurity: Enables Spring Security for our web application
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    /**
     * Configures the security filter chain - the core of Spring Security.
     *
     * This method defines:
     * - Which URLs require authentication
     * - Which URLs are public (no login needed)
     * - What type of authentication to use (HTTP Basic)
     * - CSRF protection settings
     *
     * @Bean: This method creates a SecurityFilterChain bean that Spring will manage
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Configure URL-based authorization
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints - no authentication required
                    // Allow H2 console access (DEVELOPMENT ONLY - disable in production!)
                    .requestMatchers("/h2-console/**").permitAll()

                    // Allow health check endpoint (useful for monitoring tools)
                    .requestMatchers("/actuator/health").permitAll()

                    // Protected endpoints - authentication required
                    // All /api/** endpoints require login
                    .requestMatchers("/api/**").authenticated()

                    // All other requests also require authentication
                    .anyRequest().authenticated()
            }
            // Use HTTP Basic authentication (sends username/password in headers)
            // For students: This is simple but not production-ready. Use OAuth2/JWT in real apps.
            .httpBasic { }

            // Configure CSRF (Cross-Site Request Forgery) protection
            .csrf { csrf ->
                // Disable CSRF for H2 console (needed for the H2 web interface to work)
                // For students: CSRF protection is important! Only disable for specific cases.
                csrf.ignoringRequestMatchers("/h2-console/**")
            }

            // Configure HTTP headers
            .headers { headers ->
                // Allow frames from same origin (needed for H2 console)
                // For students: Frames can be a security risk. Only allow when necessary.
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }

        return http.build()
    }

    /**
     * Creates a UserDetailsService with in-memory users.
     *
     * ⚠️ IMPORTANT FOR STUDENTS: This is for LEARNING PURPOSES ONLY!
     *
     * NEVER do this in production:
     * ❌ Hardcoded credentials in source code
     * ❌ In-memory user storage (lost on restart)
     * ❌ Single user for entire application
     *
     * In production, you should:
     * ✅ Store users in a database
     * ✅ Load credentials from environment variables or secure vaults
     * ✅ Use OAuth2/OIDC for authentication
     * ✅ Implement proper user registration and management
     *
     * Example for production:
     * ```
     * username = System.getenv("ADMIN_USERNAME")
     * password = System.getenv("ADMIN_PASSWORD")
     * ```
     */
    @Bean
    fun userDetailsService(): UserDetailsService {
        // Create a single in-memory user for testing
        val user = User.builder()
            .username("admin")  // ⚠️ DO NOT hardcode in production!
            .password(passwordEncoder().encode("password123"))  // ⚠️ DO NOT hardcode in production!
            .roles("USER")  // Grant USER role (used for authorization)
            .build()

        // InMemoryUserDetailsManager: stores users in memory (not database)
        // For students: This is OK for learning/testing, but use JdbcUserDetailsManager
        // or custom UserDetailsService with a database in real applications
        return InMemoryUserDetailsManager(user)
    }

    /**
     * Creates a password encoder bean for encrypting passwords.
     *
     * BCryptPasswordEncoder:
     * - Industry-standard password hashing algorithm
     * - One-way encryption (can't decrypt, only verify)
     * - Includes salt to prevent rainbow table attacks
     * - Adaptive: can increase work factor as computers get faster
     *
     * For students: NEVER store passwords in plain text!
     * Always use a proper password encoder like BCrypt, Argon2, or PBKDF2.
     *
     * How it works:
     * - encode("password123") -> "$2a$10$..." (hash stored in database)
     * - matches("password123", "$2a$10$...") -> true (for login verification)
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
