// index.html: busca pública de anúncios (UC04, 06_FRONTEND §3.2, 04_API endpoints 8, 9 e 10)
import { api } from '../api.js';
import { montarLayout } from '../layout.js';
import { preco, quantidadeUnidade, dataDisponivelAte } from '../formatos.js';

const FILTROS = ['texto', 'bairroId', 'categoriaId', 'modalidade'];
const form = document.getElementById('form-busca');
const lista = document.getElementById('lista');
const estado = document.getElementById('estado');
const resumo = document.getElementById('resumo');
const paginacao = document.getElementById('paginacao');
const titulo = document.getElementById('titulo-resultados');

let requisicao = 0;   // descarta respostas antigas quando o usuário busca de novo rapidamente

// Só criamos elementos com textContent: nunca innerHTML com dados da API (06_FRONTEND §5)
function el(tag, atributos = {}, filhos = []) {
  const n = document.createElement(tag);
  Object.entries(atributos).forEach(([k, v]) => (k === 'texto' ? (n.textContent = v) : n.setAttribute(k, v)));
  filhos.forEach((f) => n.append(f));
  return n;
}

function lerUrl() {
  const p = new URLSearchParams(location.search);
  const f = { pagina: Math.max(0, Number.parseInt(p.get('pagina') || '0', 10) || 0) };
  FILTROS.forEach((c) => { f[c] = p.get(c) || ''; });
  return f;
}
function filtrosDoFormulario() {
  const f = { pagina: 0 };
  FILTROS.forEach((c) => { f[c] = form.elements[c].value.trim(); });
  return f;
}
function preencherFormulario(f) { FILTROS.forEach((c) => { form.elements[c].value = f[c]; }); }
function urlDe(f) {
  const p = new URLSearchParams();
  FILTROS.forEach((c) => { if (f[c]) p.set(c, f[c]); });
  if (f.pagina > 0) p.set('pagina', String(f.pagina));
  const texto = p.toString();
  return location.pathname + (texto ? '?' + texto : '');
}

function adicionarOpcoes(select, itens) {
  itens.forEach((i) => select.append(el('option', { value: String(i.id), texto: i.nome })));
}
async function carregarOpcoes() {
  try {
    const [bairros, categorias] = await Promise.all([api('GET', '/bairros'), api('GET', '/categorias')]);
    adicionarOpcoes(form.elements.bairroId, bairros);
    adicionarOpcoes(form.elements.categoriaId, categorias);
  } catch (e) { /* a busca por texto continua funcionando sem os filtros */ }
}

// A URL da foto vem da API; só aceitamos o caminho esperado (04_API §20)
const fotoValida = (u) => typeof u === 'string' && /^\/fotos\/[0-9a-f-]{36}$/i.test(u);

function criarCard(a) {
  const foto = fotoValida(a.fotoUrl)
    ? el('img', { class: 'card__foto', src: a.fotoUrl, alt: 'Foto: ' + a.titulo, loading: 'lazy' })
    : el('img', { class: 'card__foto', src: 'img/placeholder.svg', alt: '' });
  const doacao = a.modalidade === 'DOACAO';
  const linhaPreco = el('p', { class: 'card__preco' }, [el('span', { class: 'selo ' + (doacao ? 'selo--doacao' : 'selo--venda'), texto: doacao ? 'Doação' : 'Venda' })]);
  if (!doacao) linhaPreco.append(el('span', { texto: preco(a.preco, a.unidade, a.modalidade) }));
  return el('li', { class: 'card' }, [
    foto,
    el('div', { class: 'card__corpo' }, [
      el('h3', { class: 'card__titulo' }, [el('a', { href: 'anuncio.html?id=' + encodeURIComponent(a.id), texto: a.titulo })]),
      el('p', { class: 'card__meta', texto: a.categoria.nome + ' · ' + a.bairro.nome }),
      linhaPreco,
      el('p', { class: 'card__info', texto: quantidadeUnidade(a.quantidade, a.unidade) + ' · Disponível até ' + dataDisponivelAte(a.expiraEm) }),
    ]),
  ]);
}

function mostrarEstado(tipo) {
  lista.replaceChildren();
  paginacao.hidden = true;
  paginacao.replaceChildren();
  estado.replaceChildren();
  estado.hidden = false;
  if (tipo === 'carregando') {
    resumo.textContent = '';
    estado.append(el('p', { texto: 'Carregando anúncios…' }));
  } else if (tipo === 'vazio') {
    resumo.textContent = 'Nenhum anúncio encontrado.';
    const limpar = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Limpar filtros' });
    limpar.addEventListener('click', limparFiltros);
    estado.append(el('p', { texto: 'Nenhum anúncio encontrado.' }), limpar);
  } else {
    resumo.textContent = '';
    const tentar = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Tentar novamente' });
    tentar.addEventListener('click', () => buscar(lerUrl(), false));
    estado.append(el('p', { role: 'alert', texto: 'Serviço indisponível. Tente novamente.' }), tentar);
  }
}

function desenharPaginacao(dados) {
  paginacao.replaceChildren();
  if (dados.totalPaginas <= 1) { paginacao.hidden = true; return; }
  const anterior = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Anterior' });
  const proxima = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Próxima' });
  anterior.disabled = dados.pagina <= 0;
  proxima.disabled = dados.pagina >= dados.totalPaginas - 1;
  anterior.addEventListener('click', () => irPara({ ...lerUrl(), pagina: dados.pagina - 1 }, true));
  proxima.addEventListener('click', () => irPara({ ...lerUrl(), pagina: dados.pagina + 1 }, true));
  paginacao.append(anterior, el('span', { texto: 'Página ' + (dados.pagina + 1) + ' de ' + dados.totalPaginas }), proxima);
  paginacao.hidden = false;
}

async function buscar(f, focar) {
  const minha = ++requisicao;
  mostrarEstado('carregando');
  let dados;
  try {
    dados = await api('GET', '/anuncios', { query: { texto: f.texto, bairroId: f.bairroId, categoriaId: f.categoriaId, modalidade: f.modalidade, pagina: f.pagina } });
  } catch (e) {
    if (minha === requisicao) mostrarEstado('erro');
    return;
  }
  if (minha !== requisicao) return;
  if (dados.conteudo.length === 0 && dados.totalElementos > 0) {      // página além do fim (URL antiga): volta à última
    history.replaceState(null, '', urlDe({ ...f, pagina: dados.totalPaginas - 1 }));
    return buscar({ ...f, pagina: dados.totalPaginas - 1 }, focar);
  }
  if (dados.totalElementos === 0) { mostrarEstado('vazio'); return; }
  estado.hidden = true;
  lista.replaceChildren(...dados.conteudo.map(criarCard));
  resumo.textContent = dados.totalElementos === 1 ? '1 anúncio encontrado' : dados.totalElementos + ' anúncios encontrados';
  desenharPaginacao(dados);
  if (focar) { titulo.focus(); titulo.scrollIntoView({ block: 'start' }); }
}

function irPara(f, focar) {
  history.pushState(null, '', urlDe(f));
  buscar(f, focar);
}
function limparFiltros() {
  form.reset();
  irPara({ texto: '', bairroId: '', categoriaId: '', modalidade: '', pagina: 0 }, true);
}

async function iniciar() {
  await montarLayout('index.html');
  await carregarOpcoes();
  preencherFormulario(lerUrl());
  buscar(lerUrl(), false);

  form.addEventListener('submit', (ev) => { ev.preventDefault(); irPara(filtrosDoFormulario(), true); });
  document.getElementById('limpar').addEventListener('click', limparFiltros);
  window.addEventListener('popstate', () => { preencherFormulario(lerUrl()); buscar(lerUrl(), false); });   // voltar/avançar mantêm a busca
}

iniciar();
