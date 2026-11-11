// Cabeçalho, navegação e rodapé em <header id="topo"> e <footer id="rodape"> (06_FRONTEND §3.1).
// Só createElement/textContent: nunca innerHTML com dados (§5).
import { api, invalidarCsrf } from './api.js';
import { getUsuario, limparUsuario } from './sessao.js';

const EMAIL_EQUIPE = 'colheitacompartilhada.blumenau@gmail.com';   // contato para correção ou exclusão de dados (RNF02)

function criar(tag, attrs = {}, filhos = []) {
  const n = document.createElement(tag);
  Object.entries(attrs).forEach(([k, v]) => (k === 'texto' ? (n.textContent = v) : n.setAttribute(k, v)));
  filhos.forEach((f) => n.append(f));
  return n;
}
function item(texto, href, atual, classe) {
  const a = criar('a', { href, texto });
  if (classe) a.className = classe;
  if (atual === href) a.setAttribute('aria-current', 'page');
  return criar('li', {}, [a]);
}

// Marca: círculo verde com folha (SVG, decorativo)
function marca() {
  const NS = 'http://www.w3.org/2000/svg';
  const mk = (tag, attrs) => { const n = document.createElementNS(NS, tag); Object.entries(attrs).forEach(([k, v]) => n.setAttribute(k, v)); return n; };
  const svg = mk('svg', { viewBox: '0 0 32 32', width: '30', height: '30', 'aria-hidden': 'true', focusable: 'false' });
  svg.append(
    mk('circle', { cx: '16', cy: '16', r: '16', fill: '#1f5c3f' }),
    mk('path', { d: 'M9 23C9 14 15 9 24 9C24 18 18 24 10 24Z', fill: '#e3a72f' }),
    mk('path', { d: 'M10 23L19 14', stroke: '#1f5c3f', 'stroke-width': '1.6', 'stroke-linecap': 'round', fill: 'none' }),
  );
  return svg;
}

async function sair() {
  try { await api('DELETE', '/sessao'); } catch (e) { /* mesmo com falha, volta ao início */ }
  invalidarCsrf();
  limparUsuario();
  location.href = 'index.html';
}

export async function montarLayout(paginaAtual) {
  let u = null;
  try { u = await getUsuario(); } catch (e) { u = null; } // sem rede: cabeçalho de visitante

  const menu = criar('ul', { class: 'topo__menu' });
  if (!u) {
    menu.append(item('Entrar', 'entrar.html', paginaAtual), item('Cadastrar', 'cadastro.html', paginaAtual, 'topo__cta'));
  } else {
    menu.append(
      item('Publicar', 'anuncio-form.html', paginaAtual),
      item('Meus anúncios', 'meus-anuncios.html', paginaAtual),
      item('Perfil', 'perfil.html', paginaAtual),
    );
    if (u.papel === 'ADMIN') menu.append(item('Moderação', 'admin.html', paginaAtual));
    const botao = criar('button', { type: 'button', texto: 'Sair' });
    botao.addEventListener('click', sair);
    menu.append(criar('li', {}, [botao]));
  }

  const topo = document.getElementById('topo');
  if (topo) {
    topo.className = 'topo';
    topo.replaceChildren(criar('div', { class: 'topo__interno' }, [
      criar('a', { class: 'topo__logo', href: 'index.html' }, [marca(), criar('span', { texto: 'Colheita Compartilhada' })]),
      criar('nav', { 'aria-label': 'Principal' }, [menu]),
    ]));
  }

  const rodape = document.getElementById('rodape');
  if (rodape) {
    rodape.className = 'rodape';
    const email = criar('a', { href: 'mailto:' + EMAIL_EQUIPE, texto: EMAIL_EQUIPE });
    rodape.replaceChildren(
      criar('p', { texto: 'O Colheita Compartilhada apenas divulga anúncios. Negociação, retirada e conservação são responsabilidade de anunciante e interessado.' }),
      criar('p', { texto: 'Para correção ou exclusão de dados, contate: ' }, [email]),
    );
  }
  return u;
}
