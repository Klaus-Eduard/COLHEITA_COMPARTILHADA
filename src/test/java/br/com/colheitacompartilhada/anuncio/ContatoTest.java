package br.com.colheitacompartilhada.anuncio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import br.com.colheitacompartilhada.IntegracaoTest;
import br.com.colheitacompartilhada.usuario.ContaDeTeste;
import br.com.colheitacompartilhada.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

/** UC09 Obter contato externo — RF07, RN08 [T07]. O número do anunciante é 5547999998888. */
class ContatoTest extends IntegracaoTest {

    private static final String NUMERO = "5547999998888";

    @Autowired
    CenarioAnuncios cenario;

    @Autowired
    AnuncioRepository anuncios;

    private Usuario dono;
    private Anuncio anuncio;
    private MockHttpSession interessado;

    @BeforeEach
    void preparar() throws Exception {
        cenario.limparAnuncios();
        dono = cenario.anunciante();
        anuncio = cenario.doacaoDeFrutas(dono);
        interessado = ContaDeTeste.entrar(mvc, ContaDeTeste.cadastrar(mvc));
    }

    // Estes testes deixam anúncios vencidos e ainda ATIVOS; sem limpar, o ColheitaApplicationTest (que espera
    // zero anúncios a expirar) falha dependendo da ordem em que as classes rodam.
    @AfterEach
    void limparDepois() {
        cenario.limparAnuncios();
    }

    @Test
    void uc09_1_visitanteRecebe401EONumeroNaoApareceEmNenhumaRespostaPublica() throws Exception {
        mvc.perform(get(contato(anuncio.getId()))).andExpect(status().isUnauthorized());

        mvc.perform(get("/anuncios/" + anuncio.getId())).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(NUMERO))));
        mvc.perform(get("/anuncios")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(NUMERO))));
    }

    @Test
    void uc09_2_usuarioLogadoEmAnuncioVisivelRecebeNumeroFormatadoELinkDoWhatsapp() throws Exception {
        mvc.perform(get(contato(anuncio.getId())).session(interessado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsapp").value(NUMERO))
                .andExpect(jsonPath("$.whatsappFormatado").value("(47) 99999-8888"))
                .andExpect(jsonPath("$.link").value(startsWith("https://wa.me/55")))
                .andExpect(jsonPath("$.link").value(containsString("?text=Ol%C3%A1")))
                .andExpect(jsonPath("$.link").value(containsString("%22Laranjas%20do%20quintal%22")))
                .andExpect(jsonPath("$.link").value(not(containsString("+"))));
    }

    @Test
    void uc09_3_anuncianteQueRevogouOAceiteFazOContatoResponder404() throws Exception {
        cenario.revogarAceite(dono);

        mvc.perform(get(contato(anuncio.getId())).session(interessado)).andExpect(status().isNotFound());
    }

    @Test
    void rn08_contatoDeAnuncioNaoVisivelOuInexistenteResponde404() throws Exception {
        mvc.perform(get(contato(UUID.randomUUID())).session(interessado)).andExpect(status().isNotFound());

        cenario.vencerPrazo(anuncio);
        mvc.perform(get(contato(anuncio.getId())).session(interessado)).andExpect(status().isNotFound());
    }

    @Test
    void rn08_contatoDeAnuncianteBloqueadoResponde404() throws Exception {
        cenario.bloquear(dono);

        mvc.perform(get(contato(anuncio.getId())).session(interessado)).andExpect(status().isNotFound());
    }

    @Test
    void uc08_5_pedirContatoNaoAlteraStatusNemQuantidade() throws Exception {
        mvc.perform(get(contato(anuncio.getId())).session(interessado)).andExpect(status().isOk());

        Anuncio depois = anuncios.findById(anuncio.getId()).orElseThrow();
        assertThat(depois.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(depois.getQuantidade()).isEqualByComparingTo(anuncio.getQuantidade());
    }

    private static String contato(UUID id) {
        return "/anuncios/" + id + "/contato";
    }
}
