// anuncio-form.html: publicar e editar anúncio (UC06, UC07, 06_FRONTEND §3.7, 02_REGRAS RN02-RN07, 04_API endpoints 7b, 12 e 13)
import { api } from '../api.js';
import { exigirLogin } from '../sessao.js';
import { montarLayout } from '../layout.js';
import { hojeSP, somarDias, dataIsoDisponivelAte, dataBR } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const ROTULO_STATUS = { ATIVO: 'Ativo', ENCERRADO: 'Encerrado', EXPIRADO: 'Expirado', REMOVIDO: 'Removido' };
const INTEIRAS = ['UNIDADE', 'MACO', 'DUZIA'];            // RN06: quantidade inteira
const FOTO_MAX = 2 * 1024 * 1024;                         // RN06: 2 MB
const FOTO_TIPOS = ['image/jpeg', 'image/png'];
const MSG_SERVICO = 'Serviço indisponível. Tente novamente.';

const $ = (i) => document.getElementById(i);
const f = $('form');
const idParam = new URLSearchParams(location.search).get('id');
let categorias = [];
let anuncio = null;          // MeuAnuncio carregado (modo edição)
let previaUrl = null;

// Só textContent/createElement: nunca innerHTML com dados da API (06_FRONTEND §5)
function el(tag, atributos = {}) {
  const n = document.createElement(tag);
  Object.entries(atributos).forEach(([k, v]) => (k === 'texto' ? (n.textContent = v) : n.setAttribute(k, v)));
  return n;
}
function mostrarSo(qual, textoEstado) {
  $('estado').hidden = qual !== 'estado';
  $('bloqueio').hidden = qual !== 'bloqueio';
  $('area-form').hidden = qual !== 'form';
  if (qual === 'estado') $('estado').replaceChildren(textoEstado || el('p', { texto: 'Carregando…' }));
}
function bloquear(titulo, texto, linkTexto, href) {
  $('bloqueio-titulo').textContent = titulo;
  $('bloqueio-texto').textContent = texto;
  $('bloqueio-link').textContent = linkTexto;
  $('bloqueio-link').href = href;
  mostrarSo('bloqueio');
}
function estadoErro() {
  const tentar = el('button', { type: 'button', class: 'botao botao--secundario', texto: 'Tentar novamente' });
  tentar.addEventListener('click', iniciar);
  const caixa = document.createDocumentFragment();
  caixa.append(el('p', { role: 'alert', texto: MSG_SERVICO }), tentar);
  mostrarSo('estado', caixa);
}

// ---------- regras de tela ----------
const categoriaAtual = () => categorias.find((c) => String(c.id) === f.categoriaId.value) || null;
const ehAnimal = () => !!(categoriaAtual() && categoriaAtual().origemAnimal);
const modalidadeAtual = () => (f.querySelector('[name="modalidade"]:checked') || {}).value || '';

// RN07: hoje <= disponivelAte <= hoje + 7 dias, limitada pela validade; na edição só pode reduzir
function limitesDaData() {
  const min = hojeSP();
  let max = somarDias(min, 7);
  const validade = f.validadeAlimento.value;
  if (/^\d{4}-\d{2}-\d{2}$/.test(validade) && validade < max) max = validade;
  if (anuncio) { const atual = dataIsoDisponivelAte(anuncio.expiraEm); if (atual < max) max = atual; }
  return { min, max };
}
function atualizarData() {
  const { min, max } = limitesDaData();
  f.disponivelAte.min = min;
  f.disponivelAte.max = max;
  $('dica-prazo').textContent = anuncio
    ? 'Você só pode reduzir o prazo. Prazo atual: até ' + dataBR(dataIsoDisponivelAte(anuncio.expiraEm)) + '.'
    : 'Escolha uma data entre hoje e ' + dataBR(max) + '.';
}
function atualizarCondicionais() {
  $('campo-preco').hidden = modalidadeAtual() !== 'VENDA';                       // RN03: preço só na venda
  if (modalidadeAtual() !== 'VENDA') f.preco.value = '';
  const animal = ehAnimal();                                                      // RN05: bloco de origem animal
  $('bloco-animal').hidden = !animal;
  (animal ? $('slot-animal') : $('slot-vegetal')).append($('campo-validade'));   // a validade muda de lugar e mantém o valor
  $('rotulo-validade').textContent = animal ? 'Validade do alimento' : 'Validade do alimento (opcional)';
  atualizarData();
}

// ---------- foto ----------
function setErroFoto(msg) { limparErros(f); if (msg) mostrarErros(f, { foto: msg }); }
function esconderPrevia() {
  if (previaUrl) URL.revokeObjectURL(previaUrl);
  previaUrl = null;
  $('previa').hidden = true;
  $('removerFoto').disabled = false;
}
f.foto.addEventListener('change', () => {
  const arquivo = f.foto.files[0];
  $('erro-foto').textContent = '';
  f.foto.removeAttribute('aria-invalid');
  esconderPrevia();
  if (!arquivo) return;
  const erro = !FOTO_TIPOS.includes(arquivo.type) ? 'Escolha uma foto JPEG ou PNG.' : arquivo.size > FOTO_MAX ? 'A foto deve ter até 2 MB.' : '';
  if (erro) { f.foto.value = ''; $('erro-foto').textContent = erro; f.foto.setAttribute('aria-invalid', 'true'); return; }
  previaUrl = URL.createObjectURL(arquivo);
  $('previa-img').src = previaUrl;
  $('previa').hidden = false;
  $('removerFoto').checked = false;
  $('removerFoto').disabled = true;                      // foto nova já substitui a atual
});
$('limpar-foto').addEventListener('click', () => { f.foto.value = ''; esconderPrevia(); f.foto.focus(); });

// ---------- leitura e validação ----------
const casas = (t) => (t.split(/[.,]/)[1] || '').length;
const numero = (t) => Number(t.trim().replace(',', '.'));

function coletar() {
  const animal = ehAnimal();
  const venda = modalidadeAtual() === 'VENDA';
  const dados = {
    titulo: f.titulo.value.trim(),
    descricao: f.descricao.value.trim(),
    categoriaId: f.categoriaId.value ? Number(f.categoriaId.value) : null,
    bairroId: f.bairroId.value ? Number(f.bairroId.value) : null,
    quantidade: f.quantidade.value.trim() === '' ? null : numero(f.quantidade.value),
    unidade: f.unidade.value || null,
    modalidade: modalidadeAtual() || null,
    preco: venda && f.preco.value.trim() !== '' ? numero(f.preco.value) : null,
    conservacao: f.conservacao.value.trim(),
    validadeAlimento: f.validadeAlimento.value || null,
    origemProduto: animal ? f.origemProduto.value.trim() : null,
    inspecao: animal ? f.inspecao.value.trim() : null,
    disponivelAte: f.disponivelAte.value || null,
    excedenteDeclarado: f.excedenteDeclarado.checked,
  };
  if (anuncio) dados.removerFoto = $('removerFoto').checked && !f.foto.files[0];
  return dados;
}

// Mensagens da tabela de validação de 02_REGRAS. Os erros entram na mesma ordem dos campos na tela.
function validar(d) {
  const e = {};
  const hoje = hojeSP();
  if (d.titulo.length < 3 || d.titulo.length > 100) e.titulo = 'O título deve ter de 3 a 100 caracteres.';
  if (d.categoriaId === null) e.categoriaId = 'Selecione a categoria.';
  if (d.descricao.length < 10 || d.descricao.length > 1000) e.descricao = 'A descrição deve ter de 10 a 1000 caracteres.';
  const bruto = f.quantidade.value.trim();
  if (!/^\d+([.,]\d+)?$/.test(bruto) || d.quantidade <= 0) e.quantidade = 'Informe uma quantidade maior que zero.';
  else if (casas(bruto) > 3 || d.quantidade > 9999999.999) e.quantidade = 'Use no máximo 3 casas decimais e até 9.999.999,999.';
  else if (INTEIRAS.includes(d.unidade) && !Number.isInteger(d.quantidade)) e.quantidade = 'Para esta unidade, use número inteiro.';
  if (!d.unidade) e.unidade = 'Selecione a unidade.';
  if (!d.modalidade) e.modalidade = 'Selecione doação ou venda.';
  if (d.modalidade === 'VENDA' && f.preco.value.trim() !== '') {
    const p = f.preco.value.trim();
    if (!/^\d+([.,]\d+)?$/.test(p) || numero(p) <= 0) e.preco = 'O preço deve ser maior que zero.';
    else if (casas(p) > 2 || numero(p) > 99999999.99) e.preco = 'Use no máximo 2 casas decimais e até 99.999.999,99.';
  }
  if (d.bairroId === null) e.bairroId = 'Selecione o bairro.';
  if (d.conservacao.length < 1 || d.conservacao.length > 300) e.conservacao = 'Informe como o alimento deve ser conservado.';
  const animal = ehAnimal();
  if (animal && !d.validadeAlimento) e.validadeAlimento = 'Informe a validade.';
  else if (d.validadeAlimento && d.validadeAlimento < hoje) e.validadeAlimento = 'A validade já passou.';
  if (animal && !d.origemProduto) e.origemProduto = 'Informe a origem do produto.';
  if (animal && !d.inspecao) e.inspecao = 'Informe a identificação da inspeção.';
  const { min, max } = limitesDaData();
  const ate = d.disponivelAte;
  if (!ate || ate < min || ate > somarDias(min, 7)) e.disponivelAte = 'Escolha uma data entre hoje e 7 dias.';
  else if (d.validadeAlimento && ate > d.validadeAlimento) e.disponivelAte = 'Não pode passar da validade.';
  else if (anuncio && ate > max) e.disponivelAte = 'A edição não pode ampliar o prazo do anúncio.';
  if (!d.excedenteDeclarado) e.excedenteDeclarado = 'Confirme que é excedente próprio.';
  return e;
}

// ---------- envio ----------
f.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErros(f);
  mostrarMensagem($('msg-erro'), '');
  const dados = coletar();
  if (mostrarErros(f, validar(dados))) return;
  const arquivo = f.foto.files[0];
  const fd = new FormData();                                  // 06_FRONTEND §2: parte "dados" em JSON + parte "foto"
  fd.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
  if (arquivo) fd.append('foto', arquivo);

  const botao = $('enviar');
  botao.disabled = true;
  botao.textContent = 'Salvando...';
  try {
    await api(anuncio ? 'PATCH' : 'POST', anuncio ? '/anuncios/' + encodeURIComponent(idParam) : '/anuncios', { formData: fd });
    location.href = 'meus-anuncios.html?aviso=' + (anuncio ? 'editado' : 'publicado');
    return;
  } catch (e) {                                               // o formulário continua preenchido: nada é limpo aqui
    if (e.codigo === 'PERFIL_SEM_CONTATO') bloquear('Publicar anúncio', 'Para publicar, cadastre seu WhatsApp e autorize o contato.', 'Ir para o perfil', 'perfil.html');
    else if (e.status === 413) mostrarErros(f, { foto: 'A foto deve ter até 2 MB.' });
    else if (e.codigo === 'ARQUIVO_INVALIDO') mostrarErros(f, { foto: 'Escolha uma foto JPEG ou PNG.' });
    else if (e.status === 409) mostrarMensagem($('msg-erro'), 'Este anúncio mudou de situação: ' + (ROTULO_STATUS[e.statusAtual] || 'indisponível') + '. Ele não pode mais ser editado.', 'erro');
    else if (e.status === 403 || e.status === 404) mostrarMensagem($('msg-erro'), 'Este anúncio não está mais disponível para edição.', 'erro');
    else {
      const campos = e.campos || {};
      const mostrou = mostrarErros(f, campos);
      const solto = Object.keys(campos).some((k) => !$('erro-' + k));
      if (!mostrou || solto) mostrarMensagem($('msg-erro'), e.status >= 500 ? MSG_SERVICO : e.mensagem, 'erro');
    }
    botao.disabled = false;
    botao.textContent = anuncio ? 'Salvar alterações' : 'Publicar';
  }
});

// ---------- carga ----------
function preencher(a) {
  f.titulo.value = a.titulo;
  f.categoriaId.value = String(a.categoria.id);
  f.descricao.value = a.descricao;
  f.quantidade.value = String(a.quantidade).replace('.', ',');
  f.unidade.value = a.unidade;
  const radio = f.querySelector('[name="modalidade"][value="' + a.modalidade + '"]');
  if (radio) radio.checked = true;
  f.preco.value = a.preco === null || a.preco === undefined ? '' : Number(a.preco).toFixed(2).replace('.', ',');
  f.bairroId.value = String(a.bairro.id);
  f.conservacao.value = a.conservacao || '';
  f.validadeAlimento.value = a.validadeAlimento || '';
  f.origemProduto.value = a.origemProduto || '';
  f.inspecao.value = a.inspecao || '';
  f.disponivelAte.value = dataIsoDisponivelAte(a.expiraEm);
  f.excedenteDeclarado.checked = true;                       // já foi declarado ao publicar
  if (typeof a.fotoUrl === 'string' && /^\/fotos\/[0-9a-f-]{36}$/i.test(a.fotoUrl)) { $('foto-atual-img').src = a.fotoUrl; $('foto-atual').hidden = false; }
}

async function iniciar() {
  const usuario = await exigirLogin();
  if (!usuario) return;
  await montarLayout('anuncio-form.html');
  mostrarSo('estado');
  const editar = idParam !== null;
  if (usuario.papel === 'ADMIN') {       // decisão da equipe: o administrador só modera (a API não impede; o bloqueio é da tela)
    bloquear(editar ? 'Editar anúncio' : 'Publicar anúncio', 'Administradores não publicam nem editam anúncios. Use a Moderação para tratar denúncias.', 'Ir para a moderação', 'admin.html');
    return;
  }
  if (editar && !UUID.test(idParam)) { bloquear('Editar anúncio', 'Este anúncio não está mais disponível.', 'Voltar para Meus anúncios', 'meus-anuncios.html'); return; }
  try {
    const pedidos = [api('GET', '/categorias'), api('GET', '/bairros'), editar ? api('GET', '/me/anuncios/' + encodeURIComponent(idParam)) : api('GET', '/me')];
    const [cats, bairros, extra] = await Promise.all(pedidos);
    categorias = cats;
    if (editar) {
      anuncio = extra;
      if (!anuncio.podeEditar) { bloquear('Editar anúncio', 'Este anúncio não pode mais ser editado.', 'Voltar para Meus anúncios', 'meus-anuncios.html'); return; }
    } else if (!extra.podePublicar) {                       // RN02: publicar exige WhatsApp e autorização de contato
      bloquear('Publicar anúncio', 'Para publicar, cadastre seu WhatsApp e autorize o contato.', 'Ir para o perfil', 'perfil.html');
      return;
    }
    if (f.categoriaId.options.length === 1) {
      cats.forEach((c) => f.categoriaId.append(el('option', { value: String(c.id), texto: c.nome })));
      bairros.forEach((b) => f.bairroId.append(el('option', { value: String(b.id), texto: b.nome })));
    }
  } catch (e) {
    if (e.status === 403 || e.status === 404) bloquear('Editar anúncio', 'Este anúncio não está mais disponível.', 'Voltar para Meus anúncios', 'meus-anuncios.html');
    else estadoErro();
    return;
  }
  $('titulo-pagina').textContent = anuncio ? 'Editar anúncio' : 'Publicar anúncio';
  document.title = (anuncio ? 'Editar anúncio' : 'Publicar anúncio') + ' - Colheita Compartilhada';
  $('enviar').textContent = anuncio ? 'Salvar alterações' : 'Publicar';
  if (anuncio) preencher(anuncio);
  atualizarCondicionais();
  if (!anuncio) f.disponivelAte.value = limitesDaData().max;   // sugestão: o prazo máximo, que a pessoa pode reduzir
  mostrarSo('form');
}

f.categoriaId.addEventListener('change', atualizarCondicionais);
f.addEventListener('change', (ev) => { if (ev.target.name === 'modalidade') atualizarCondicionais(); });
f.validadeAlimento.addEventListener('change', atualizarData);

iniciar();
