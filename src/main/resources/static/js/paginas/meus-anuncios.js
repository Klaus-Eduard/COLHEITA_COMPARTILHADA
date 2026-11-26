// meus-anuncios.html: lista do dono e encerramento (UC07, UC08, 06_FRONTEND §3.8, 04_API endpoints 7 e 14, 02_REGRAS RN09)
import { api } from '../api.js';
import { exigirLogin } from '../sessao.js';
import { montarLayout } from '../layout.js';
import { preco, quantidadeUnidade, dataDisponivelAte, dataBR } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const ROTULO_STATUS = { ATIVO: 'Ativo', ENCERRADO: 'Encerrado', EXPIRADO: 'Expirado', REMOVIDO: 'Removido' };
const ROTULO_RESULTADO = { DOADO: 'Doado', VENDIDO: 'Vendido', NAO_INFORMADO: 'Não informado' };
const RESULTADOS = { DOACAO: ['DOADO', 'NAO_INFORMADO'], VENDA: ['VENDIDO', 'NAO_INFORMADO'] };   // RN09: compatível com a modalidade
const MSG_SERVICO = 'Serviço indisponível. Tente novamente.';
const MSG_INDISPONIVEL = 'Este anúncio não está mais disponível.';

const $ = (i) => document.getElementById(i);
const lista = $('lista');
const estado = $('estado');
const paginacao = $('paginacao');
const msgOk = $('msg-ok');
const msgErro = $('msg-erro');
const dialogo = $('dialogo-encerrar');
const formEncerrar = $('form-encerrar');

let requisicao = 0;
let alvo = null;          // anúncio que está sendo encerrado
let gatilho = null;       // botão que abriu o diálogo

// Só textContent/createElement: nunca innerHTML com dados da API (06_FRONTEND §5)
function el(tag, atributos = {}, filhos = []) {
  const n = document.createElement(tag);
  Object.entries(atributos).forEach(([k, v]) => (k === 'texto' ? (n.textContent = v) : n.setAttribute(k, v)));
  filhos.forEach((f) => n.append(f));
  return n;
}
const paginaDaUrl = () => Math.max(0, Number.parseInt(new URLSearchParams(location.search).get('pagina') || '0', 10) || 0);
const urlDaPagina = (p) => location.pathname + (p > 0 ? '?pagina=' + p : '');
const fotoValida = (u) => typeof u === 'string' && /^\/fotos\/[0-9a-f-]{36}$/i.test(u);

function criarLinha(a) {
  const doacao = a.modalidade === 'DOACAO';
  const foto = fotoValida(a.fotoUrl)
    ? el('img', { class: 'meu__foto', src: a.fotoUrl, alt: 'Foto: ' + a.titulo, loading: 'lazy' })
    : el('img', { class: 'meu__foto', src: 'img/placeholder.svg', alt: '' });
  // anúncio invisível dá 404 na página pública, então só linkamos quando está visível (RN08)
  const titulo = el('h2', { class: 'meu__titulo' }, [a.visivel ? el('a', { href: 'anuncio.html?id=' + encodeURIComponent(a.id), texto: a.titulo }) : document.createTextNode(a.titulo)]);
  const selos = el('div', { class: 'meu__selos' }, [
    el('span', { class: 'selo selo--' + a.status.toLowerCase(), texto: ROTULO_STATUS[a.status] || a.status }),
    el('span', { class: 'selo ' + (doacao ? 'selo--doacao' : 'selo--venda'), texto: doacao ? 'Doação' : 'Venda' }),
  ]);
  const info = quantidadeUnidade(a.quantidade, a.unidade) + (doacao ? '' : ' · ' + preco(a.preco, a.unidade, a.modalidade));
  let situacao = '';
  if (a.status === 'ATIVO') situacao = 'Disponível até ' + dataDisponivelAte(a.expiraEm);
  else if (a.status === 'ENCERRADO') situacao = 'Encerrado em ' + dataBR(a.encerradoEm) + (a.resultado ? ' · ' + (ROTULO_RESULTADO[a.resultado] || a.resultado) : '');
  else if (a.status === 'EXPIRADO') situacao = 'Prazo encerrado em ' + dataDisponivelAte(a.expiraEm);
  else situacao = 'Removido pela moderação';

  const corpo = el('div', { class: 'meu__corpo' }, [
    titulo, selos,
    el('p', { class: 'meu__meta', texto: a.categoria.nome + ' · ' + a.bairro.nome }),
    el('p', { class: 'meu__meta', texto: info }),
    el('p', { class: 'meu__meta', texto: situacao }),
  ]);
  // Ativo mas invisível enquanto ainda dentro do prazo: o motivo é a autorização de contato (RN02.5, RN08)
  if (a.status === 'ATIVO' && !a.visivel && a.podeEditar) {
    corpo.append(el('p', { class: 'nota-info', texto: 'Oculto: verifique a autorização de contato no perfil.' }));
  }
  const acoes = el('div', { class: 'meu__acoes' });
  if (a.podeEditar) {
    // aria-label diferencia os vários "Editar" e "Encerrar" para quem usa leitor de tela
    acoes.append(el('a', { class: 'botao botao--secundario botao--auto', href: 'anuncio-form.html?id=' + encodeURIComponent(a.id), texto: 'Editar', 'aria-label': 'Editar anúncio ' + a.titulo }));
    const encerrar = el('button', { type: 'button', class: 'botao botao--secundario botao--auto', texto: 'Encerrar' });
    encerrar.setAttribute('aria-label', 'Encerrar anúncio ' + a.titulo);
    encerrar.addEventListener('click', () => abrirEncerrar(a, encerrar));
    acoes.append(encerrar);
  }
  return el('li', { class: 'meu' }, [foto, corpo, acoes]);
}

function mostrarEstado(tipo) {
  lista.replaceChildren();
  paginacao.hidden = true;
  paginacao.replaceChildren();
  estado.replaceChildren();
  estado.hidden = false;
  if (tipo === 'carregando') {
    estado.append(el('p', { texto: 'Carregando seus anúncios…' }));
  } else if (tipo === 'vazio') {
    estado.append(el('p', { texto: 'Você ainda não tem anúncios.' }), el('a', { class: 'botao botao--auto', href: 'anuncio-form.html', texto: 'Publicar o primeiro anúncio' }));
  } else {
    const tentar = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Tentar novamente' });
    tentar.addEventListener('click', () => carregar(paginaDaUrl()));
    estado.append(el('p', { role: 'alert', texto: MSG_SERVICO }), tentar);
  }
}

function desenharPaginacao(d) {
  paginacao.replaceChildren();
  if (d.totalPaginas <= 1) { paginacao.hidden = true; return; }
  const anterior = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Anterior' });
  const proxima = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Próxima' });
  anterior.disabled = d.pagina <= 0;
  proxima.disabled = d.pagina >= d.totalPaginas - 1;
  const ir = (p) => { history.pushState(null, '', urlDaPagina(p)); carregar(p, { focar: true }); };
  anterior.addEventListener('click', () => ir(d.pagina - 1));
  proxima.addEventListener('click', () => ir(d.pagina + 1));
  paginacao.append(anterior, el('span', { texto: 'Página ' + (d.pagina + 1) + ' de ' + d.totalPaginas }), proxima);
  paginacao.hidden = false;
}

async function carregar(pagina, { focar = false } = {}) {
  const minha = ++requisicao;
  mostrarEstado('carregando');
  let d;
  try {
    d = await api('GET', '/me/anuncios', { query: { pagina } });
  } catch (e) {
    if (minha === requisicao) mostrarEstado('erro');
    return;
  }
  if (minha !== requisicao) return;
  if (d.conteudo.length === 0 && d.totalElementos > 0) {      // página além do fim (por exemplo, após encerrar o último item)
    history.replaceState(null, '', urlDaPagina(d.totalPaginas - 1));
    return carregar(d.totalPaginas - 1, { focar });
  }
  if (d.totalElementos === 0) { mostrarEstado('vazio'); return; }
  estado.hidden = true;
  lista.replaceChildren(...d.conteudo.map(criarLinha));
  desenharPaginacao(d);
  if (focar) $('titulo-pagina').focus();
}

// ---------- encerrar ----------
function abrirEncerrar(a, botao) {
  alvo = a;
  gatilho = botao;
  formEncerrar.reset();
  limparErros(formEncerrar);
  mostrarMensagem($('dialogo-erro'), '');
  $('encerrar-titulo').textContent = a.titulo;
  $('opcoes-resultado').replaceChildren(...(RESULTADOS[a.modalidade] || ['NAO_INFORMADO']).map((r) => {
    const radio = el('input', { type: 'radio', name: 'resultado', value: r });
    return el('label', { class: 'opcao' }, [radio, document.createTextNode(ROTULO_RESULTADO[r])]);
  }));
  dialogo.showModal();
  formEncerrar.querySelector('input[name="resultado"]').focus();
}
$('cancelar-encerrar').addEventListener('click', () => dialogo.close());
dialogo.addEventListener('close', () => {
  // depois de recarregar a lista o botão antigo some; nesse caso o foco vai para o título da página
  if (gatilho && document.contains(gatilho)) gatilho.focus(); else $('titulo-pagina').focus();
});

formEncerrar.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErros(formEncerrar);
  mostrarMensagem($('dialogo-erro'), '');
  const escolhido = formEncerrar.querySelector('input[name="resultado"]:checked');
  if (!escolhido) { mostrarErros(formEncerrar, { resultado: 'Selecione o que aconteceu com o alimento.' }); return; }
  const botao = $('confirmar-encerrar');
  botao.disabled = true;
  botao.textContent = 'Encerrando...';
  try {
    await api('POST', '/anuncios/' + encodeURIComponent(alvo.id) + '/encerramento', { json: { resultado: escolhido.value } });
    dialogo.close();
    mostrarMensagem(msgOk, 'Anúncio encerrado.', 'ok');
    await carregar(paginaDaUrl());
  } catch (e) {
    if (e.status === 409) {                                   // mudou de situação (moderação, expiração ou outra aba)
      dialogo.close();
      mostrarMensagem(msgErro, 'O anúncio mudou de situação: ' + (ROTULO_STATUS[e.statusAtual] || 'indisponível') + '.', 'erro');
      await carregar(paginaDaUrl());
    } else if (e.status === 403 || e.status === 404) {
      mostrarMensagem($('dialogo-erro'), MSG_INDISPONIVEL, 'erro');
    } else if (!mostrarErros(formEncerrar, e.campos)) {
      mostrarMensagem($('dialogo-erro'), e.status >= 500 || e.status === 0 ? MSG_SERVICO : e.mensagem, 'erro');
    }
  } finally {
    botao.disabled = false;
    botao.textContent = 'Encerrar anúncio';
  }
});

async function iniciar() {
  const usuario = await exigirLogin();      // sem sessão, vai para entrar.html?voltar=meus-anuncios.html
  if (!usuario) return;
  await montarLayout('meus-anuncios.html');
  if (usuario.papel === 'ADMIN') {          // decisão da equipe: o administrador não publica, edita nem encerra anúncios
    $('btn-publicar').hidden = true;
    estado.replaceChildren(el('p', { texto: 'Administradores não publicam anúncios. Use a Moderação para tratar denúncias.' }), el('a', { class: 'botao botao--auto', href: 'admin.html', texto: 'Ir para a moderação' }));
    estado.hidden = false;
    return;
  }
  // vindo do formulário: só aceitamos textos fixos (nada da URL vai para a tela)
  const AVISOS = { publicado: 'Anúncio publicado.', editado: 'Anúncio atualizado.' };
  const aviso = AVISOS[new URLSearchParams(location.search).get('aviso')];
  if (aviso) {
    mostrarMensagem(msgOk, aviso, 'ok');
    history.replaceState(null, '', urlDaPagina(paginaDaUrl()));
  }
  window.addEventListener('popstate', () => carregar(paginaDaUrl()));
  carregar(paginaDaUrl());
}

iniciar();
