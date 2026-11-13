// perfil.html (UC03, 06_FRONTEND §3.6). Regras do aceite: RN02.
import { api } from '../api.js';
import { montarLayout } from '../layout.js';
import { definirUsuario, exigirLogin } from '../sessao.js';
import { apenasDigitos, mascaraWhatsApp } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const AVISO_TROCA = 'Ao trocar o número, a autorização será renovada.';
const AVISO_REVOGACAO = 'Seus anúncios ficarão ocultos enquanto a autorização estiver desligada.';

const form = document.getElementById('form');
const erroGlobal = document.getElementById('mensagem-erro');
const okGlobal = document.getElementById('mensagem-ok');
const botao = document.getElementById('enviar');
const aviso = document.getElementById('aviso-aceite');

let salvo = null; // último perfil gravado: base para decidir qual aviso mostrar

function numeroLocal(whatsapp) {
  return whatsapp ? apenasDigitos(whatsapp).replace(/^55/, '') : '';
}

function preencher(u) {
  salvo = u;
  form.nome.value = u.nome;
  form.email.value = u.email;
  form.querySelectorAll('[name="tipo"]').forEach((r) => { r.checked = r.value === u.tipo; });
  form.whatsapp.value = mascaraWhatsApp(numeroLocal(u.whatsapp));
  form.aceiteContato.checked = !!u.aceiteContatoEm;
  atualizarAviso();
}

function atualizarAviso() {
  const tinhaAceite = !!(salvo && salvo.aceiteContatoEm);
  const numeroMudou = apenasDigitos(form.whatsapp.value) !== numeroLocal(salvo && salvo.whatsapp);
  let texto = '';
  if (tinhaAceite && !form.aceiteContato.checked) texto = AVISO_REVOGACAO;
  else if (tinhaAceite && numeroMudou) texto = AVISO_TROCA;
  aviso.textContent = texto;
  aviso.hidden = !texto;
}

form.whatsapp.addEventListener('input', () => {
  form.whatsapp.value = mascaraWhatsApp(form.whatsapp.value);
  atualizarAviso();
});
form.aceiteContato.addEventListener('change', atualizarAviso);

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErros(form);
  mostrarMensagem(erroGlobal, '');
  mostrarMensagem(okGlobal, '', 'ok');
  const digitos = apenasDigitos(form.whatsapp.value);
  const dados = {
    nome: form.nome.value.trim(),
    tipo: (form.querySelector('[name="tipo"]:checked') || {}).value,
    whatsapp: digitos || null,
    aceiteContato: form.aceiteContato.checked,
  };

  // Regras e mensagens: tabela de validação de campos em 02_REGRAS
  const erros = {};
  if (dados.nome.length < 2 || dados.nome.length > 120) erros.nome = 'Informe seu nome (2 a 120 caracteres).';
  if (!dados.tipo) erros.tipo = 'Selecione o tipo de participante.';
  if (digitos && digitos.length !== 10 && digitos.length !== 11) erros.whatsapp = 'Informe o WhatsApp com DDD.';
  if (!digitos && dados.aceiteContato) erros.whatsapp = 'Informe o WhatsApp com DDD.';
  if (mostrarErros(form, erros)) return;

  botao.disabled = true;
  botao.textContent = 'Salvando...';
  try {
    const u = await api('PATCH', '/me', { json: dados });
    definirUsuario(u);
    preencher(u);
    mostrarMensagem(okGlobal, u.podePublicar
      ? 'Perfil salvo. Você já pode publicar anúncios.'
      : 'Perfil salvo. Para publicar, cadastre seu WhatsApp e autorize o contato.', 'ok');
  } catch (e) {
    if (!mostrarErros(form, e.campos)) mostrarMensagem(erroGlobal, e.mensagem, 'erro');
  } finally {
    botao.disabled = false;
    botao.textContent = 'Salvar';
  }
});

const usuario = await exigirLogin();
if (usuario) {
  await montarLayout('perfil.html');
  preencher(usuario);
  form.hidden = false;
}
