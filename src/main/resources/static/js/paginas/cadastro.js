// cadastro.html (UC01, 06_FRONTEND §3.5)
import { api } from '../api.js';
import { montarLayout } from '../layout.js';
import { destinoAposLogin } from '../sessao.js';
import { emailValido } from '../formatos.js';
import { limparErros, mostrarErros, mostrarMensagem } from '../form-erros.js';

const SENHA_MIN = 8;
// Regras e mensagens: tabela de validação de campos em 02_REGRAS
const SENHA_MAX = 72; // limite do BCrypt

const form = document.getElementById('form');
const erroGlobal = document.getElementById('mensagem-erro');
const botao = document.getElementById('enviar');
document.getElementById('dica-senha').textContent = 'De ' + SENHA_MIN + ' a ' + SENHA_MAX + ' caracteres.';

const logado = await montarLayout('cadastro.html');
if (logado) location.replace(destinoAposLogin(logado));

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  limparErros(form);
  mostrarMensagem(erroGlobal, '');
  const dados = {
    nome: form.nome.value.trim(),
    email: form.email.value.trim(),
    senha: form.senha.value,
    tipo: (form.querySelector('[name="tipo"]:checked') || {}).value,
  };

  const erros = {};
  if (dados.nome.length < 2 || dados.nome.length > 120) erros.nome = 'Informe seu nome (2 a 120 caracteres).';
  if (!emailValido(dados.email)) erros.email = 'Informe um e-mail válido.';
  if (dados.senha.length < SENHA_MIN || dados.senha.length > SENHA_MAX) erros.senha = 'A senha deve ter de ' + SENHA_MIN + ' a ' + SENHA_MAX + ' caracteres.';
  if (!dados.tipo) erros.tipo = 'Selecione o tipo de participante.';
  if (mostrarErros(form, erros)) return;

  botao.disabled = true;
  botao.textContent = 'Criando...';
  try {
    await api('POST', '/usuarios', { json: dados });
    location.href = 'entrar.html?conta=criada';
  } catch (e) {
    const campos = Object.assign({}, e.campos);
    if (e.status === 409 && !campos.email) campos.email = 'E-mail já cadastrado.';
    if (!mostrarErros(form, campos)) mostrarMensagem(erroGlobal, e.mensagem, 'erro');
    botao.disabled = false;
    botao.textContent = 'Criar conta';
  }
});
