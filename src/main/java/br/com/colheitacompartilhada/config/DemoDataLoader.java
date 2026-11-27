package br.com.colheitacompartilhada.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.colheitacompartilhada.anuncio.AnuncioRepository;
import br.com.colheitacompartilhada.anuncio.AnuncioService;
import br.com.colheitacompartilhada.anuncio.Modalidade;
import br.com.colheitacompartilhada.anuncio.StatusAnuncio;
import br.com.colheitacompartilhada.anuncio.Unidade;
import br.com.colheitacompartilhada.anuncio.dto.AnuncioForm;
import br.com.colheitacompartilhada.catalogo.Bairro;
import br.com.colheitacompartilhada.catalogo.CatalogoService;
import br.com.colheitacompartilhada.catalogo.Categoria;
import br.com.colheitacompartilhada.usuario.TipoParticipante;
import br.com.colheitacompartilhada.usuario.Usuario;
import br.com.colheitacompartilhada.usuario.UsuarioRepository;
import br.com.colheitacompartilhada.usuario.UsuarioService;
import br.com.colheitacompartilhada.usuario.dto.CadastroRequest;
import br.com.colheitacompartilhada.usuario.dto.PerfilRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dados fictícios para a apresentação (issue #19). Só roda com o perfil "demo" (SPRING_PROFILES_ACTIVE=demo).
 * Usa os services de verdade, então tudo passa pelas mesmas regras (RN02 a RN08) da aplicação.
 * Não faz nada se a conta de demonstração (Maria) já tiver anúncio ativo e dentro do prazo, por isso é seguro a cada reinício.
 * Nunca impede o app de subir: qualquer falha vira um aviso no log.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    static final String EMAIL_MARIA = "maria.demo@exemplo.com";
    static final String EMAIL_JOAO = "joao.demo@exemplo.com";

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final String DADO_FICTICIO = "SIM 0000 (dado fictício)";

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarios;
    private final AnuncioService anuncioService;
    private final AnuncioRepository anuncios;
    private final CatalogoService catalogo;
    private final Clock clock;
    private final String senha;

    public DemoDataLoader(UsuarioService usuarioService, UsuarioRepository usuarios, AnuncioService anuncioService,
            AnuncioRepository anuncios, CatalogoService catalogo, Clock clock,
            @Value("${app.demo.senha}") String senha) {
        this.usuarioService = usuarioService;
        this.usuarios = usuarios;
        this.anuncioService = anuncioService;
        this.anuncios = anuncios;
        this.catalogo = catalogo;
        this.clock = clock;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (senha.length() < 8 || senha.length() > 72) {
            log.warn("Perfil demo ativo, mas DEMO_PASSWORD precisa ter de 8 a 72 caracteres. Nada foi carregado.");
            return;
        }
        // Só os anúncios da conta de demonstração contam: anúncios de teste de outras pessoas não impedem o carregamento.
        if (anuncios.existsByUsuarioEmailAndStatusAndExpiraEmAfter(EMAIL_MARIA, StatusAnuncio.ATIVO, clock.instant())) {
            log.info("Dados de demonstração já presentes. Nada a fazer.");
            return;
        }
        try {
            log.info("Dados de demonstração carregados. Anúncios criados: {}", carregar());
        } catch (RuntimeException e) {
            log.error("Não foi possível carregar os dados de demonstração. O app continua normalmente.", e);
        }
    }

    int carregar() {
        Usuario maria = garantirUsuario("Maria (demonstração)", EMAIL_MARIA, TipoParticipante.MORADOR, "(47) 99999-0001");
        Usuario joao = garantirUsuario("João (demonstração)", EMAIL_JOAO, TipoParticipante.PEQUENO_PRODUTOR,
                "(47) 99999-0002");
        Map<String, Long> categorias = catalogo.listarCategorias().stream()
                .collect(Collectors.toMap(Categoria::getNome, Categoria::getId));
        Map<String, Long> bairros = catalogo.listarBairros().stream()
                .collect(Collectors.toMap(Bairro::getNome, Bairro::getId));
        LocalDate hoje = ClockConfig.hoje(clock);

        List<Modelo> modelos = List.of(
                new Modelo(maria, "Laranjas do quintal", "Laranjas colhidas hoje, doces e sem agrotóxico. Retirada combinada pelo WhatsApp.",
                        "Frutas", "Garcia", "5", Unidade.KG, Modalidade.DOACAO, null, null, 5, true, 0xF4A300),
                new Modelo(maria, "Limão tahiti", "Limões do pé, ótimos para suco e tempero.",
                        "Frutas", "Água Verde", "3", Unidade.KG, Modalidade.VENDA, "4.50", null, 6, true, 0x7CB518),
                new Modelo(maria, "Alface crespa", "Alfaces colhidas hoje de manhã. Preço a combinar.",
                        "Verduras e legumes", "Velha", "10", Unidade.UNIDADE, Modalidade.VENDA, null, null, 5, false, 0x3F8F3F),
                new Modelo(maria, "Mandioca fresca", "Mandioca colhida esta semana, boa para cozinhar ou fritar.",
                        "Verduras e legumes", "Itoupava Norte", "4", Unidade.KG, Modalidade.DOACAO, null, null, 7, false, 0xB08968),
                new Modelo(maria, "Bananas prata", "Cachos de banana prata maduras, colhidas no sítio.",
                        "Frutas", "Vila Nova", "6", Unidade.KG, Modalidade.DOACAO, null, null, 5, true, 0xF2D45C),
                new Modelo(maria, "Couve manteiga", "Maços de couve fresca, colhida hoje.",
                        "Verduras e legumes", "Itoupava Central", "8", Unidade.MACO, Modalidade.DOACAO, null, null, 5, true, 0x2E7D32),
                new Modelo(maria, "Tomate caqui", "Tomates firmes, sem agrotóxico, direto da horta.",
                        "Verduras e legumes", "Badenfurt", "5", Unidade.KG, Modalidade.VENDA, "6.00", null, 6, false, 0xD64545),
                new Modelo(maria, "Goiabas vermelhas", "Goiabas maduras de pé, ótimas para doce e suco.",
                        "Frutas", "Fortaleza Alta", "4", Unidade.KG, Modalidade.DOACAO, null, null, 6, true, 0xE57373),
                new Modelo(joao, "Ovos caipiras", "Ovos de galinhas criadas soltas, coletados nesta semana.",
                        "Ovos", "Fortaleza", "2", Unidade.DUZIA, Modalidade.VENDA, "12.00", hoje.plusDays(15), 6, true, 0xF2E8CF),
                new Modelo(joao, "Mel de laranjeira", "Mel puro de laranjeira, pote de 500 g.",
                        "Mel", "Ponta Aguda", "6", Unidade.UNIDADE, Modalidade.VENDA, "35.00", hoje.plusDays(180), 7, true, 0xE0A100),
                new Modelo(joao, "Ovos de codorna", "Ovos de codorna frescos, bandejas de uma dúzia.",
                        "Ovos", "Testo Salto", "3", Unidade.DUZIA, Modalidade.VENDA, "9.00", hoje.plusDays(12), 7, false, 0xE8DCC4),
                new Modelo(joao, "Mel silvestre", "Mel silvestre de colmeias próprias, pote de 300 g.",
                        "Mel", "Salto do Norte", "4", Unidade.UNIDADE, Modalidade.VENDA, "28.00", hoje.plusDays(200), 7, true, 0xC27C0E),
                new Modelo(joao, "Abóbora cabotiá", "Abóboras grandes, doces, para sopa e purê.",
                        "Verduras e legumes", "Progresso", "6", Unidade.KG, Modalidade.DOACAO, null, null, 7, true, 0xE69138),
                new Modelo(joao, "Bergamotas", "Bergamotas (mexericas) doces, colhidas esta semana.",
                        "Frutas", "Velha Central", "8", Unidade.KG, Modalidade.VENDA, "3.50", null, 5, false, 0xF6A04D),
                new Modelo(joao, "Cheiro-verde", "Maços de salsinha e cebolinha. Preço a combinar.",
                        "Verduras e legumes", "Escola Agrícola", "12", Unidade.MACO, Modalidade.VENDA, null, null, 5, false, 0x558B2F));

        int criados = 0;
        for (Modelo modelo : modelos) {
            publicar(modelo, categorias, bairros, hoje);
            criados++;
        }
        return criados;
    }

    private Usuario garantirUsuario(String nome, String email, TipoParticipante tipo, String whatsapp) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseGet(() -> usuarioService.cadastrar(new CadastroRequest(nome, email, senha, tipo)));
        if (!usuario.podePublicar()) {
            usuarioService.atualizarPerfil(usuario.getId(), new PerfilRequest(nome, tipo, whatsapp, true));
        }
        return usuario;
    }

    private void publicar(Modelo m, Map<String, Long> categorias, Map<String, Long> bairros, LocalDate hoje) {
        boolean animal = m.validade() != null;
        AnuncioForm form = new AnuncioForm(m.titulo(), m.descricao(), categorias.get(m.categoria()),
                bairros.get(m.bairro()), new BigDecimal(m.quantidade()), m.unidade(), m.modalidade(),
                m.preco() == null ? null : new BigDecimal(m.preco()), "Manter em local fresco e arejado",
                m.validade(), animal ? "Produção própria (dado fictício)" : null, animal ? DADO_FICTICIO : null,
                hoje.plusDays(m.diasDisponivel()), true, false);
        MultipartFile foto = m.comFoto()
                ? new ArquivoEmMemoria(ImagemDemo.png(640, 480, 0xFFFFFF, m.corDaFoto(), 0xFFFFFF))
                : null;
        anuncioService.publicar(m.autor().getId(), form, foto);
    }

    private record Modelo(Usuario autor, String titulo, String descricao, String categoria, String bairro,
            String quantidade, Unidade unidade, Modalidade modalidade, String preco, LocalDate validade,
            int diasDisponivel, boolean comFoto, int corDaFoto) {
    }

    /** MultipartFile que vive só na memória: o app nunca grava a foto em disco (AGENTS.md). */
    private record ArquivoEmMemoria(byte[] bytes) implements MultipartFile {

        @Override
        public String getName() {
            return "foto";
        }

        @Override
        public String getOriginalFilename() {
            return "demo.png";
        }

        @Override
        public String getContentType() {
            return "image/png";
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File destino) throws IOException {
            throw new IOException("Não é permitido gravar fotos em disco.");
        }
    }
}
