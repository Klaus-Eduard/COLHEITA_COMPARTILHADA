// Sessão do usuário. O cookie é HttpOnly; GET /me é a fonte de verdade (06_FRONTEND §1, §3.4, §5).
import { api, marcarPaginaProtegida, paginaAtual } from './api.js';

let usuario;       // undefined = ainda não carregado; null = visitante
let promessa = null;

export function limparUsuario() { usuario = undefined; promessa = null; }
// POST /sessao devolve o mesmo corpo de GET /me (04_API §3): evita uma chamada extra após o login
export function definirUsuario(u) { usuario = u; promessa = null; }

export async function getUsuario() {
  if (usuario !== undefined) return usuario;
  if (!promessa) {
    promessa = api('GET', '/me', { semRedirecionar: true })
      .then((u) => { usuario = u; return u; })
      .catch((e) => {
        promessa = null;
        if (e.status === 401 || e.status === 403) { usuario = null; return null; }
        throw e;
      });
  }
  return promessa;
}

export async function exigirLogin() {
  marcarPaginaProtegida();
  const u = await getUsuario();
  if (!u) { location.href = 'entrar.html?voltar=' + encodeURIComponent(paginaAtual()); return null; }
  return u;
}

// Usuário comum vê só "Acesso restrito", sem dados (§3.9). Retorna o usuário ADMIN ou null.
export async function exigirAdmin() {
  const u = await exigirLogin();
  if (!u) return null;
  if (u.papel !== 'ADMIN') {
    const main = document.querySelector('main') || document.body;
    const p = document.createElement('p');
    p.setAttribute('role', 'alert');
    p.textContent = 'Acesso restrito.';
    main.replaceChildren(p);
    return null;
  }
  return u;
}

// ?voltar só aceita caminho local: começa com "/" (e não "//") ou é uma página local como "anuncio.html?id=3".
export function voltarSeguro(valor) {
  if (typeof valor !== 'string' || valor === '' || valor.includes('\\')) return null;
  if (valor.startsWith('/') && !valor.startsWith('//')) return valor;
  return /^[a-z0-9-]+\.html(\?[^\s]*)?(#[^\s]*)?$/i.test(valor) ? valor : null;
}

export function destinoAposLogin(u) {
  const voltar = voltarSeguro(new URLSearchParams(location.search).get('voltar'));
  return voltar || (u && u.papel === 'ADMIN' ? 'admin.html' : 'index.html');
}
