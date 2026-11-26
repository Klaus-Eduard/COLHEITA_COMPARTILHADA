// admin.html: fila de denúncias e moderação (UC11, UC12, 06_FRONTEND §3.9, 04_API endpoints 17, 18 e 19, 02_REGRAS RN10)
import { api } from '../api.js';
import { exigirAdmin } from '../sessao.js';
import { montarLayout } from '../layout.js';
import { preco, quantidadeUnidade, dataBR, dataDisponivelAte, rotuloTipo } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const ROTULO_STATUS = { ATIVO: 'Ativo', ENCERRADO: 'Encerrado', EXPIRADO: 'Expirado', REMOVIDO: 'Removido' };
const MSG_SERVICO = 'Serviço indisponível. Tente novamente.';
const MSG_NAO_ENCONTRADA = 'Denúncia não encontrada.';

const $ = (i) => document.getElementById(i);
const params = new URLSearchParams(location.search);
const idDenuncia = params.get('id');
const status = params.get('status') === 'RESOLVIDA' ? 'RESOLVIDA' : 'ABERTA';
const msgOk = $('msg-ok');
const msgErro = $('msg-erro');
const form = $('form-decisao');
const dialogo = $('dialogo-bloqueio');

let requisicao = 0;
let denuncia = null;      // detalhe carregado

// Só textContent/createElement: nunca innerHTML com dados da API (06_FRONTEND §5)
function el(tag, atributos = {}, filhos = []) {
  const n = document.createElement(tag);
  Object.entries(atributos).forEach(([k, v]) => (k === 'texto' ? (n.textContent = v) : n.setAttribute(k, v)));
  filhos.forEach((f) => n.append(f));
  return n;
}
const selo = (texto, classe) => el('span', { class: 'selo ' + classe, texto });
const paginaDaUrl = () => Math.max(0, Number.parseInt(params.get('pagina') || '0', 10) || 0);
const urlDaFila = (pagina) => {
  const p = new URLSearchParams();
  if (status === 'RESOLVIDA') p.set('status', 'RESOLVIDA');
  if (pagina > 0) p.set('pagina', String(pagina));
  const t = p.toString();
  return location.pathname + (t ? '?' + t : '');
};
function mostrarVista(qual) {
  $('estado').hidden = qual !== 'estado';
  $('vista-fila').hidden = qual !== 'fila';
  $('vista-detalhe').hidden = qual !== 'detalhe';
}
function estadoTexto(texto, botao) {
  const caixa = document.createDocumentFragment();
  caixa.append(el('p', { texto }));
  if (botao) caixa.append(botao);
  $('estado').replaceChildren(caixa);
  mostrarVista('estado');
}
function botaoTentar(fn) {
  const b = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Tentar novamente' });
  b.addEventListener('click', fn);
  return b;
}
const linkFila = () => el('a', { class: 'botao botao--auto', href: 'admin.html', texto: 'Voltar à fila' });
// A API devolve 403 para usuário comum, mas exigirAdmin já barra antes; se acontecer, não mostramos nada
function acessoRestrito() { $('principal').replaceChildren(el('p', { role: 'alert', texto: 'Acesso restrito.' })); }

// ---------- fila ----------
function criarItem(d) {
  const aAnuncio = el('p', { class: 'fila__meta' }, [document.createTextNode('Anúncio: ' + d.anuncio.titulo + ' '), selo(ROTULO_STATUS[d.anuncio.status] || d.anuncio.status, 'selo--' + d.anuncio.status.toLowerCase())]);
  const aPessoas = el('p', { class: 'fila__meta' }, [document.createTextNode('Anunciante: ' + d.anunciante.nome + ' ')]);
  if (!d.anunciante.ativo) aPessoas.append(selo('Bloqueado', 'selo--bloqueado'), document.createTextNode(' '));
  aPessoas.append(document.createTextNode('· Denunciado por ' + d.autor.nome + ' · ' + dataBR(d.criadoEm)));
  const abrir = el('a', { class: 'botao botao--secundario', href: 'admin.html?id=' + encodeURIComponent(d.id), texto: d.status === 'ABERTA' ? 'Analisar' : 'Ver' });
  abrir.setAttribute('aria-label', (d.status === 'ABERTA' ? 'Analisar' : 'Ver') + ' denúncia sobre ' + d.anuncio.titulo);
  return el('li', { class: 'fila__item' }, [
    el('div', {}, [el('p', { class: 'fila__motivo', texto: d.motivo }), aAnuncio, aPessoas]),
    el('div', { class: 'fila__acoes' }, [abrir]),
  ]);
}

function desenharPaginacao(d) {
  const nav = $('paginacao');
  nav.replaceChildren();
  if (d.totalPaginas <= 1) { nav.hidden = true; return; }
  const anterior = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Anterior' });
  const proxima = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Próxima' });
  anterior.disabled = d.pagina <= 0;
  proxima.disabled = d.pagina >= d.totalPaginas - 1;
  const ir = (p) => { history.pushState(null, '', urlDaFila(p)); params.set('pagina', String(p)); carregarFila(p, true); };
  anterior.addEventListener('click', () => ir(d.pagina - 1));
  proxima.addEventListener('click', () => ir(d.pagina + 1));
  nav.append(anterior, el('span', { texto: 'Página ' + (d.pagina + 1) + ' de ' + d.totalPaginas }), proxima);
  nav.hidden = false;
}

async function carregarFila(pagina, focar = false) {
  const minha = ++requisicao;
  $('aba-abertas').toggleAttribute('aria-current', status === 'ABERTA');
  $('aba-resolvidas').toggleAttribute('aria-current', status === 'RESOLVIDA');
  if (status === 'ABERTA') $('aba-abertas').setAttribute('aria-current', 'page'); else $('aba-resolvidas').setAttribute('aria-current', 'page');
  $('lista').replaceChildren();
  $('paginacao').hidden = true;
  $('resumo').textContent = '';
  let d;
  try {
    d = await api('GET', '/admin/denuncias', { query: { status, pagina } });
  } catch (e) {
    if (minha !== requisicao) return;
    if (e.status === 403) { acessoRestrito(); return; }
    estadoTexto(MSG_SERVICO, botaoTentar(() => carregarFila(paginaDaUrl())));
    return;
  }
  if (minha !== requisicao) return;
  if (d.conteudo.length === 0 && d.totalElementos > 0) { history.replaceState(null, '', urlDaFila(d.totalPaginas - 1)); params.set('pagina', String(d.totalPaginas - 1)); return carregarFila(d.totalPaginas - 1, focar); }
  mostrarVista('fila');
  if (d.totalElementos === 0) {
    $('resumo').textContent = status === 'ABERTA' ? 'Não há denúncias abertas.' : 'Nenhuma denúncia resolvida ainda.';
    return;
  }
  $('resumo').textContent = d.totalElementos === 1 ? '1 denúncia' : d.totalElementos + ' denúncias';
  $('lista').replaceChildren(...d.conteudo.map(criarItem));
  desenharPaginacao(d);
  if (focar) $('titulo-fila').focus();
}

// ---------- detalhe ----------

function desenharDetalhe(d) {
  denuncia = d;
  const a = d.anuncioDetalhe;
  const pessoa = d.anunciante;
  $('d-status').textContent = d.status === 'ABERTA' ? 'Aberta' : 'Resolvida';
  $('d-status').className = 'selo ' + (d.status === 'ABERTA' ? 'selo--expirado' : 'selo--encerrado');
  $('d-meta').textContent = 'Enviada em ' + dataBR(d.criadoEm) + ' por ' + d.autor.nome;
  $('d-motivo').textContent = d.motivo;

  const foto = $('a-foto');
  if (typeof a.fotoUrl === 'string' && /^\/fotos\/[0-9a-f-]{36}$/i.test(a.fotoUrl)) { foto.src = a.fotoUrl; foto.alt = 'Foto: ' + a.titulo; } else { foto.src = 'img/placeholder.svg'; foto.alt = ''; }
  $('a-titulo').textContent = a.titulo;
  $('a-status').textContent = ROTULO_STATUS[a.status] || a.status;
  $('a-status').className = 'selo selo--' + a.status.toLowerCase();
  const doacao = a.modalidade === 'DOACAO';
  $('a-modalidade').textContent = doacao ? 'Doação' : 'Venda';
  $('a-modalidade').className = 'selo ' + (doacao ? 'selo--doacao' : 'selo--venda');
  $('a-visibilidade').textContent = a.visivel ? 'Visível ao público' : 'Oculto do público';
  $('a-visibilidade').className = 'selo ' + (a.visivel ? 'selo--doacao' : 'selo--encerrado');
  $('a-categoria').textContent = a.categoria.nome;
  $('a-bairro').textContent = a.bairro.nome;
  $('a-quantidade').textContent = quantidadeUnidade(a.quantidade, a.unidade) + (doacao ? '' : ' · ' + preco(a.preco, a.unidade, a.modalidade));
  $('a-disponivel').textContent = dataDisponivelAte(a.expiraEm);
  $('a-descricao').textContent = a.descricao;
  const animal = !!(a.origemProduto || a.inspecao);
  $('a-declaradas').hidden = !animal;
  if (animal) {
    $('a-origem').textContent = a.origemProduto || '';
    $('a-validade').textContent = a.validadeAlimento ? dataBR(a.validadeAlimento) : '';
    $('a-inspecao').textContent = a.inspecao || '';
  }
  $('a-link-publico').hidden = !a.visivel;                       // anúncio invisível dá 404 na página pública
  if (a.visivel) $('a-link').href = 'anuncio.html?id=' + encodeURIComponent(a.id);

  $('p-nome').textContent = pessoa.nome;
  $('p-email').textContent = pessoa.email || '';
  $('p-tipo').textContent = rotuloTipo(a.anunciante && a.anunciante.tipo) || '—';
  $('p-papel').textContent = pessoa.papel === 'ADMIN' ? 'Administrador' : 'Usuário';
  $('p-conta').textContent = pessoa.ativo ? 'Ativa' : 'Bloqueada';

  const aberta = d.status === 'ABERTA';
  form.hidden = !aberta;
  $('decisao-leitura').hidden = aberta;
  if (aberta) {
    form.reset();
    limparErros(form);
    mostrarMensagem($('msg-form'), '');
    atualizarContador();
    atualizarBloqueio();
  } else {
    $('r-decisao').textContent = d.decisao || '';
    $('r-moderador').textContent = d.moderador ? d.moderador.nome : '';
    $('r-data').textContent = d.resolvidaEm ? dataBR(d.resolvidaEm) : '';
  }
  document.title = 'Denúncia sobre ' + a.titulo + ' - Moderação';
  mostrarVista('detalhe');
}

async function carregarDetalhe(id) {
  if (!UUID.test(id)) { estadoTexto(MSG_NAO_ENCONTRADA, linkFila()); return; }
  estadoTexto('Carregando denúncia…');
  try {
    desenharDetalhe(await api('GET', '/admin/denuncias/' + encodeURIComponent(id)));
  } catch (e) {
    if (e.status === 403) acessoRestrito();
    else if (e.status === 404) estadoTexto(MSG_NAO_ENCONTRADA, linkFila());
    else estadoTexto(MSG_SERVICO, botaoTentar(() => carregarDetalhe(id)));
  }
}

// ---------- formulário de decisão (RN10.6) ----------
const procedente = () => (form.querySelector('[name="procedente"]:checked') || {}).value;
function atualizarContador() { $('contador-decisao').textContent = $('decisao').value.length + '/500'; }
function atualizarBloqueio() {
  const caixa = form.bloquearAnunciante;
  const p = denuncia.anunciante;
  let dica = '';
  if (p.papel !== 'USUARIO') dica = 'Administradores não podem ser bloqueados.';
  else if (!p.ativo) dica = 'Esta conta já está bloqueada.';
  else if (procedente() !== 'true') dica = 'Disponível quando a denúncia procede.';
  caixa.disabled = dica !== '';
  if (caixa.disabled) caixa.checked = false;
  $('dica-bloqueio').textContent = dica || 'Todos os anúncios dele ficarão ocultos.';
}
$('decisao').addEventListener('input', atualizarContador);
form.addEventListener('change', (ev) => { if (ev.target.name === 'procedente') atualizarBloqueio(); });

async function enviar() {
  const botao = $('registrar');
  botao.disabled = true;
  botao.textContent = 'Registrando...';
  try {
    await api('PATCH', '/admin/denuncias/' + encodeURIComponent(denuncia.id), {
      json: { procedente: procedente() === 'true', decisao: $('decisao').value.trim(), bloquearAnunciante: form.bloquearAnunciante.checked },
    });
    location.href = 'admin.html?aviso=resolvida';
    return;
  } catch (e) {                                                     // o formulário continua preenchido
    if (e.status === 409) {
      mostrarMensagem(msgErro, 'Esta denúncia já foi resolvida.', 'erro');
      await carregarDetalhe(denuncia.id);                           // volta em modo somente leitura
    } else if (e.codigo === 'BLOQUEIO_NAO_PERMITIDO') {
      mostrarMensagem($('msg-form'), 'Não é possível bloquear este usuário.', 'erro');
    } else if (e.status === 404) {
      estadoTexto(MSG_NAO_ENCONTRADA, linkFila());
    } else if (e.status === 403) {
      acessoRestrito();
    } else if (!mostrarErros(form, e.campos)) {
      mostrarMensagem($('msg-form'), e.status >= 500 ? MSG_SERVICO : e.mensagem, 'erro');
    }
    botao.disabled = false;
    botao.textContent = 'Registrar decisão';
  }
}

form.addEventListener('submit', (ev) => {
  ev.preventDefault();
  limparErros(form);
  mostrarMensagem($('msg-form'), '');
  const erros = {};
  if (procedente() === undefined) erros.procedente = 'Informe se a denúncia procede.';
  const decisao = $('decisao').value.trim();
  if (decisao.length < 1 || decisao.length > 500) erros.decisao = 'Registre a decisão.';
  if (mostrarErros(form, erros)) return;
  if (form.bloquearAnunciante.checked) {                            // confirmação antes de bloquear (06_FRONTEND §3.9)
    $('texto-bloqueio').textContent = 'Bloquear ' + denuncia.anunciante.nome + '? Todos os anúncios dele ficarão ocultos.';
    dialogo.showModal();
    $('cancelar-bloqueio').focus();
    return;
  }
  enviar();
});
$('cancelar-bloqueio').addEventListener('click', () => dialogo.close());
$('confirmar-bloqueio').addEventListener('click', () => { dialogo.close(); enviar(); });
dialogo.addEventListener('close', () => { if (!denuncia || denuncia.status === 'ABERTA') $('registrar').focus(); });

async function iniciar() {
  const admin = await exigirAdmin();                                // usuário comum vê só "Acesso restrito." e nenhuma chamada de dados é feita
  if (!admin) return;
  await montarLayout('admin.html');
  const AVISOS = { resolvida: 'Decisão registrada.' };
  const aviso = AVISOS[params.get('aviso')];
  if (aviso) { mostrarMensagem(msgOk, aviso, 'ok'); params.delete('aviso'); history.replaceState(null, '', urlDaFila(paginaDaUrl())); }
  window.addEventListener('popstate', () => location.reload());
  if (idDenuncia !== null) carregarDetalhe(idDenuncia); else carregarFila(paginaDaUrl());
}

iniciar();
