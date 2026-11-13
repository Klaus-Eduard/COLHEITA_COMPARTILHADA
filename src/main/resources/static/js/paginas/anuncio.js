// anuncio.html: detalhe, contato externo e denúncia (UC05, UC09, UC10, 06_FRONTEND §3.3, 04_API endpoints 11, 15 e 16)
import { api } from '../api.js';
import { montarLayout } from '../layout.js';
import { getUsuario } from '../sessao.js';
import { preco, quantidadeUnidade, dataDisponivelAte, dataBR, rotuloTipo } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MSG_INDISPONIVEL = 'Este anúncio não está mais disponível.';
const MSG_SERVICO = 'Serviço indisponível. Tente novamente.';
const MSG_MOTIVO = 'Descreva o motivo (mínimo 10 caracteres).';
const id = new URLSearchParams(location.search).get('id') || '';

const $ = (i) => document.getElementById(i);
const estado = $('estado');
const detalhe = $('detalhe');
const acoes = $('acoes');
const msgOk = $('msg-ok');
const msgErro = $('msg-erro');
const dialogo = $('dialogo-denuncia');
const formDenuncia = $('form-denuncia');

// Só textContent/createElement: nunca innerHTML com dados da API (06_FRONTEND §5)
function el(tag, atributos = {}) {
  const n = document.createElement(tag);
  Object.entries(atributos).forEach(([k, v]) => (k === 'texto' ? (n.textContent = v) : n.setAttribute(k, v)));
  return n;
}
function mostrarEstado(tipo) {
  detalhe.hidden = true;
  estado.hidden = false;
  estado.replaceChildren();
  if (tipo === 'carregando') {
    estado.append(el('p', { texto: 'Carregando anúncio…' }));
  } else if (tipo === 'indisponivel') {
    document.title = 'Anúncio indisponível - Colheita Compartilhada';
    estado.append(el('p', { texto: MSG_INDISPONIVEL }), el('a', { class: 'botao botao--auto', href: 'index.html', texto: 'Buscar outros anúncios' }));
  } else {
    const tentar = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Tentar novamente' });
    tentar.addEventListener('click', carregar);
    estado.append(el('p', { role: 'alert', texto: MSG_SERVICO }), tentar);
  }
}

const fotoValida = (u) => typeof u === 'string' && /^\/fotos\/[0-9a-f-]{36}$/i.test(u);
const paginaAtual = () => 'anuncio.html?id=' + encodeURIComponent(id);

function desenhar(a) {
  document.title = a.titulo + ' - Colheita Compartilhada';
  const foto = $('foto');
  if (fotoValida(a.fotoUrl)) { foto.src = a.fotoUrl; foto.alt = 'Foto: ' + a.titulo; } else { foto.src = 'img/placeholder.svg'; foto.alt = ''; }
  const doacao = a.modalidade === 'DOACAO';
  $('selo').textContent = doacao ? 'Doação' : 'Venda';
  $('selo').className = 'selo ' + (doacao ? 'selo--doacao' : 'selo--venda');
  $('preco').textContent = doacao ? '' : preco(a.preco, a.unidade, a.modalidade);
  $('titulo').textContent = a.titulo;
  $('categoria').textContent = a.categoria.nome;
  $('bairro').textContent = a.bairro.nome;
  $('quantidade').textContent = quantidadeUnidade(a.quantidade, a.unidade);
  $('disponivel').textContent = dataDisponivelAte(a.expiraEm);
  $('anunciante').textContent = a.anunciante.nome + (rotuloTipo(a.anunciante.tipo) ? ' · ' + rotuloTipo(a.anunciante.tipo) : '');
  $('descricao').textContent = a.descricao;
  $('conservacao').textContent = a.conservacao;

  const animal = !!(a.origemProduto || a.inspecao);     // categorias de origem animal exigem origem e inspeção (RN05)
  $('declaradas').hidden = !animal;
  if (animal) {
    $('origem').textContent = a.origemProduto || '';
    $('validade-animal').textContent = a.validadeAlimento ? dataBR(a.validadeAlimento) : '';
    $('inspecao').textContent = a.inspecao || '';
  }
  const mostrarValidade = !animal && !!a.validadeAlimento;
  $('rotulo-validade').hidden = !mostrarValidade;
  $('validade').hidden = !mostrarValidade;
  if (mostrarValidade) $('validade').textContent = dataBR(a.validadeAlimento);
  estado.hidden = true;
  detalhe.hidden = false;
}

function desenharAcoes(a, usuario) {
  acoes.replaceChildren();
  if (a.souDono) {                                                       // dono não contata nem denuncia a si mesmo
    acoes.append(el('a', { class: 'botao botao--auto', href: 'anuncio-form.html?id=' + encodeURIComponent(id), texto: 'Editar' }));
    return;
  }
  if (!usuario) {                                                        // visitante: entra e volta para este anúncio
    acoes.append(el('a', { class: 'botao botao--auto', href: 'entrar.html?voltar=' + encodeURIComponent(paginaAtual()), texto: 'Entrar em contato' }));
    return;
  }
  const contato = el('button', { type: 'button', class: 'botao botao--auto', id: 'btn-contato', texto: 'Entrar em contato' });
  contato.addEventListener('click', () => pedirContato(contato));
  acoes.append(contato);
  if (usuario.papel === 'ADMIN') return;                                 // decisão da equipe: o administrador não denuncia (ele modera)
  const denunciar = el('button', { type: 'button', class: 'botao botao--secundario botao--auto', id: 'btn-denunciar', texto: 'Denunciar' });
  denunciar.addEventListener('click', () => abrirDenuncia(denunciar));
  acoes.append(denunciar);
}

async function pedirContato(botao) {
  mostrarMensagem(msgErro, '');
  mostrarMensagem(msgOk, '');
  botao.disabled = true;
  try {
    const c = await api('GET', '/anuncios/' + encodeURIComponent(id) + '/contato');   // revalida a visibilidade no servidor (RN08)
    $('numero').textContent = c.whatsappFormatado;
    const abrir = $('abrir-whatsapp');
    if (typeof c.link === 'string' && c.link.startsWith('https://wa.me/')) { abrir.href = c.link; abrir.hidden = false; } else { abrir.hidden = true; }
    $('copiado').textContent = '';
    $('copiar').onclick = () => copiar(c.whatsappFormatado);
    $('painel-contato').hidden = false;
    botao.hidden = true;
    abrir.focus();
  } catch (e) {
    botao.disabled = false;
    if (e.status === 401) { location.href = 'entrar.html?voltar=' + encodeURIComponent(paginaAtual()); return; }
    mostrarMensagem(msgErro, e.status === 404 ? MSG_INDISPONIVEL : MSG_SERVICO, 'erro');
  }
}

async function copiar(texto) {
  try {
    await navigator.clipboard.writeText(texto);
    $('copiado').textContent = 'Número copiado.';
  } catch (e) {                                                          // sem permissão: seleciona o texto para o usuário copiar
    const r = document.createRange();
    r.selectNodeContents($('numero'));
    const s = getSelection();
    s.removeAllRanges();
    s.addRange(r);
    $('copiado').textContent = 'Número selecionado. Aperte Ctrl+C para copiar.';
  }
}

// ---------- denúncia ----------
let botaoDenunciar = null;
function atualizarContador() { $('contador').textContent = $('motivo').value.length + '/500 (mínimo 10)'; }
function abrirDenuncia(botao) {
  botaoDenunciar = botao;
  formDenuncia.reset();
  limparErros(formDenuncia);
  mostrarMensagem($('dialogo-erro'), '');
  atualizarContador();
  dialogo.showModal();
  $('motivo').focus();
}
$('motivo').addEventListener('input', atualizarContador);
$('cancelar-denuncia').addEventListener('click', () => dialogo.close());
dialogo.addEventListener('close', () => { if (botaoDenunciar) botaoDenunciar.focus(); });

formDenuncia.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErros(formDenuncia);
  mostrarMensagem($('dialogo-erro'), '');
  const motivo = $('motivo').value.trim();
  if (motivo.length < 10 || motivo.length > 500) { mostrarErros(formDenuncia, { motivo: MSG_MOTIVO }); return; }
  const botao = $('enviar-denuncia');
  botao.disabled = true;
  botao.textContent = 'Enviando...';
  try {
    await api('POST', '/anuncios/' + encodeURIComponent(id) + '/denuncias', { json: { motivo } });
    dialogo.close();
    mostrarMensagem(msgOk, 'Denúncia enviada. Obrigado.', 'ok');
  } catch (e) {
    if (e.status === 401) { location.href = 'entrar.html?voltar=' + encodeURIComponent(paginaAtual()); return; }
    if (e.status === 409) mostrarMensagem($('dialogo-erro'), 'Você já tem uma denúncia aberta para este anúncio.', 'erro');
    else if (e.status === 404) mostrarMensagem($('dialogo-erro'), MSG_INDISPONIVEL, 'erro');
    else if (!mostrarErros(formDenuncia, e.campos)) mostrarMensagem($('dialogo-erro'), e.status >= 500 || e.status === 0 ? MSG_SERVICO : e.mensagem, 'erro');
  } finally {
    botao.disabled = false;
    botao.textContent = 'Enviar denúncia';
  }
});

async function carregar() {
  if (!UUID.test(id)) { mostrarEstado('indisponivel'); return; }
  mostrarEstado('carregando');
  try {
    const [a, usuario] = await Promise.all([api('GET', '/anuncios/' + encodeURIComponent(id)), getUsuario().catch(() => null)]);
    desenhar(a);
    desenharAcoes(a, usuario);
  } catch (e) {
    mostrarEstado(e.status === 404 ? 'indisponivel' : 'erro');
  }
}

await montarLayout('anuncio.html');
carregar();
