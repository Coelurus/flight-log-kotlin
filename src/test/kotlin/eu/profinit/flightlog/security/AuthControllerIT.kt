package eu.profinit.flightlog.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT @Autowired constructor(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) {

    @Test
    fun `login with bad password returns 401 with generic message`() {
        val body = mapper.writeValueAsString(mapOf("email" to "writer@flightlog.local", "password" to "wrongpass"))
        mockMvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login succeeds for default writer account`() {
        val body = mapper.writeValueAsString(mapOf("email" to "writer@flightlog.local", "password" to "writer1234"))
        val result = mockMvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
        ).andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `protected admin endpoint without login returns 401`() {
        mockMvc.perform(post("/api/admin/flights").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
    }
}
