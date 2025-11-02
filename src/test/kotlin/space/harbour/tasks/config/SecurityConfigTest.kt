package space.harbour.tasks.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest : FunSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userDetailsService: UserDetailsService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    init {

        context("Public Endpoints") {

            test("should allow access to /h2-console without authentication") {
                // H2 console returns 404 in test context, but important thing is
                // it doesn't return 401 (unauthorized) - security permits access
                mockMvc.perform(get("/h2-console"))
                    .andExpect(status().isNotFound)
            }

            test("should allow access to /actuator/health without authentication") {
                mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk)
            }
        }

        context("Protected API Endpoints") {

            test("should return 401 for /api/** endpoints without authentication") {
                mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isUnauthorized)
            }

            test("should allow access to /api/** with correct credentials") {
                mockMvc.perform(
                    get("/api/tasks")
                        .with(httpBasic("admin", "password123"))
                )
                    .andExpect(status().isOk)
            }

            test("should return 401 for /api/** with wrong credentials") {
                mockMvc.perform(
                    get("/api/tasks")
                        .with(httpBasic("admin", "wrongpassword"))
                )
                    .andExpect(status().isUnauthorized)
            }
        }

        context("CSRF Protection") {

            test("should require CSRF token for POST to /api/** endpoints") {
                mockMvc.perform(
                    post("/api/tasks")
                        .with(httpBasic("admin", "password123"))
                        .contentType("application/json")
                        .content("""{"description": "Test"}""")
                )
                    .andExpect(status().isForbidden)
            }

            test("should allow POST to /api/** with CSRF token") {
                mockMvc.perform(
                    post("/api/tasks")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""{"description": "Test"}""")
                )
                    .andExpect(status().isCreated)
            }

            test("should not require CSRF token for /h2-console/**") {
                // H2 console has CSRF disabled, so POST should work without CSRF token
                // Returns 404 in test context (no actual H2 endpoint), but important thing
                // is it doesn't return 403 (CSRF error) - CSRF is correctly disabled
                mockMvc.perform(post("/h2-console/test"))
                    .andExpect(status().isNotFound)
            }
        }

        context("UserDetailsService Bean") {

            test("should return admin user with correct username") {
                val userDetails = userDetailsService.loadUserByUsername("admin")

                userDetails.shouldNotBe(null)
                userDetails.username.shouldBe("admin")
                userDetails.authorities.size.shouldBe(1)
                userDetails.authorities.first().authority.shouldBe("ROLE_USER")
            }

            test("should use BCrypt encoded password") {
                val userDetails = userDetailsService.loadUserByUsername("admin")

                // BCrypt hashes start with $2a$, $2b$, or $2y$
                userDetails.password.shouldStartWith("$2")

                // Verify the password matches "password123"
                passwordEncoder.matches("password123", userDetails.password).shouldBe(true)

                // Verify wrong password doesn't match
                passwordEncoder.matches("wrongpassword", userDetails.password).shouldBe(false)
            }
        }
    }
}
