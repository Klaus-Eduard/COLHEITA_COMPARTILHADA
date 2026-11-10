package br.com.colheitacompartilhada.catalogo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.colheitacompartilhada.IntegracaoTest;
import org.junit.jupiter.api.Test;

/** 04_API #8 e #9 — RF03. */
class CatalogoTest extends IntegracaoTest {

    @Test
    void categoriasSaoPublicasEVemOrdenadasPorId() throws Exception {
        mvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(1, 2, 3, 4)))
                .andExpect(jsonPath("$[*].nome").value(contains("Frutas", "Verduras e legumes", "Ovos", "Mel")))
                .andExpect(jsonPath("$[*].origemAnimal").value(contains(false, false, true, true)));
    }

    @Test
    void bairrosSaoPublicosEVemOrdenadosPorNome() throws Exception {
        mvc.perform(get("/bairros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(35)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].nome").value("Água Verde"))
                .andExpect(jsonPath("$[1].nome").value("Badenfurt"))
                .andExpect(jsonPath("$[7].nome").value("Fidélis"))
                .andExpect(jsonPath("$[8].nome").value("Fortaleza"))
                .andExpect(jsonPath("$[9].nome").value("Fortaleza Alta"))
                .andExpect(jsonPath("$[34].nome").value("Vorstadt"));
    }

    @Test
    void bairroExpoeSomenteIdENome() throws Exception {
        mvc.perform(get("/bairros"))
                .andExpect(jsonPath("$[0].municipio").doesNotExist());
    }
}
