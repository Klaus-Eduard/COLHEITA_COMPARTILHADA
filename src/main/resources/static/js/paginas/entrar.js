// entrar.html (UC02, 06_FRONTEND §3.4)
import { api, invalidarCsrf } from '../api.js';
import { definirUsuario, destinoAposLogin } from '../sessao.js';
import { montarLayout } from '../layout.js';
import { emailValido } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const form = document.getElementById('form');
const erroGlobal = document.getElementById('mensagem-erro');
const okGlobal = document.getElementById('mensagem-ok');
const botao = document.getElementById('enviar');

const logado = await montarLayout('entrar.html');
if (logado) location.replace(destinoAposLogin(logado));

if (new URLSearchParams(location.search).get('conta') === 'criada') {
  mostrarMensagem(okGlobal, 'Conta criada. Entre para continuar.', 'ok');
}

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErros(form);
  mostrarMensagem(erroGlobal, '');
  const email = form.email.value.trim();
  const senha = form.senha.value;

  const erros = {};
  if (!emailValido(email)) erros.email = 'Informe um e-mail válido.';
  if (!senha) erros.senha = 'Informe sua senha.';
  if (mostrarErros(form, erros)) return;

  botao.disabled = true;
  botao.textContent = 'Entrando...';
  try {
    const usuario = await api('POST', '/sessao', { json: { email, senha } });
    invalidarCsrf();   // a sessão mudou: o token antigo deixa de valer
    definirUsuario(usuario);
    location.href = destinoAposLogin(usuario);
  } catch (e) {
    const msg = e.status === 401 ? 'E-mail ou senha inválidos.'
      : e.status === 429 ? 'Muitas tentativas. Aguarde alguns minutos.' : e.mensagem;
    mostrarMensagem(erroGlobal, msg, 'erro');
    botao.disabled = false;
    botao.textContent = 'Entrar';
  }
});
