package pe.gob.valdizan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba de integración básica: login, protección por JWT/roles y anti-duplicado de citas (H2 en modo MySQL, datos semilla). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthCitaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private JsonNode login(String correo, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"" + correo + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"paciente@valdizan.gob.pe\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    void loginSinCamposDevuelveErroresPorCampo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.identificador").exists());
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/citas/paciente/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void pacienteNoPuedeEntrarAlAreaDeAdministracion() throws Exception {
        String token = login("paciente@valdizan.gob.pe", "Valdizan2026*").get("token").asText();
        mvc.perform(get("/api/admin/indicadores").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void adminConsultaIndicadores() throws Exception {
        String token = login("admin@valdizan.gob.pe", "Admin2026*").get("token").asText();
        mvc.perform(get("/api/admin/indicadores").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCitas").exists());
    }

    @Test
    void reservaYReservaDuplicadaDevuelve409() throws Exception {
        JsonNode sesion = login("paciente@valdizan.gob.pe", "Valdizan2026*");
        String auth = "Bearer " + sesion.get("token").asText();
        long pacienteId = sesion.get("usuario").get("id").asLong();

        long espId = mapper.readTree(mvc.perform(get("/api/especialidades")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();

        String fecha = LocalDate.now().plusDays(2).toString();
        JsonNode libres = mapper.readTree(mvc.perform(get("/api/horarios/disponibles")
                        .param("especialidadId", String.valueOf(espId)).param("fecha", fecha).header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(libres.size() >= 2, "la semilla debe generar horarios disponibles");

        String reserva = "{\"pacienteId\":" + pacienteId + ",\"horarioId\":" + libres.get(0).get("idHorario").asLong() + "}";
        mvc.perform(post("/api/citas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(reserva))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // mismo horario: ya no está disponible -> 409
        mvc.perform(post("/api/citas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(reserva))
                .andExpect(status().isConflict());
    }
}
