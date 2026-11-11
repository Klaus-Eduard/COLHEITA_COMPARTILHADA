// Único ponto de chamada HTTP (06_FRONTEND §2).
export class ApiErro extends Error {
  constructor(status, codigo, mensagem, campos) {
    super(mensagem);
    this.status = status;
    this.codigo = codigo;
    this.mensagem = mensagem;
    this.campos = campos || {};
    this.statusAtual = null;   // 409 ESTADO_INVALIDO traz o status atual do recurso (04_API §2)
  }
}

let tokenCsrf = null;
let nomeCabecalhoCsrf = 'X-XSRF-TOKEN';
let paginaProtegida = false;

export function invalidarCsrf() { tokenCsrf = null; }
export function marcarPaginaProtegida() { paginaProtegida = true; }
export function paginaAtual() { return (location.pathname.split('/').pop() || 'index.html') + location.search; }

function erroDeRede() { return new ApiErro(0, 'REDE', 'Sem conexão. Tente novamente.'); }

async function obterCsrf() {
  if (tokenCsrf) return tokenCsrf;
  let resp;
  try { resp = await fetch('/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } }); }
  catch (e) { throw erroDeRede(); }
  if (!resp.ok) throw new ApiErro(resp.status, 'CSRF', 'Serviço indisponível. Tente novamente.');
  const dados = await resp.json().catch(() => null);
  // Contrato (04_API §1.1): GET /csrf -> { token, headerName }
  tokenCsrf = (dados && dados.token) || null;
  if (dados && dados.headerName) nomeCabecalhoCsrf = dados.headerName;
  return tokenCsrf;
}

function montarUrl(url, query) {
  if (!query) return url;
  const p = new URLSearchParams();
  Object.entries(query).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.append(k, v); });
  const texto = p.toString();
  return texto ? url + (url.includes('?') ? '&' : '?') + texto : url;
}

// opcoes: { json, formData, query, semRedirecionar }
export async function api(metodo, url, { json, formData, query, semRedirecionar } = {}) {
  const headers = { Accept: 'application/json' };
  let corpo;
  if (formData) corpo = formData;                         // o navegador define o boundary
  else if (json !== undefined) { headers['Content-Type'] = 'application/json'; corpo = JSON.stringify(json); }

  if (metodo !== 'GET') {
    const token = await obterCsrf();
    if (token) headers[nomeCabecalhoCsrf] = token;
  }

  let resp;
  try { resp = await fetch(montarUrl(url, query), { method: metodo, headers, body: corpo, credentials: 'same-origin' }); }
  catch (e) { throw erroDeRede(); }

  if (resp.status === 204) return null;
  const dados = await resp.json().catch(() => null);
  if (resp.ok) return dados;

  const padrao = resp.status >= 500 ? 'Serviço indisponível. Tente novamente.' : 'Não foi possível concluir a operação.';
  const erro = new ApiErro(resp.status, (dados && dados.codigo) || 'ERRO', (dados && dados.mensagem) || padrao, dados && dados.campos);
  if (dados && dados.statusAtual) erro.statusAtual = dados.statusAtual;
  if (resp.status === 401 && paginaProtegida && !semRedirecionar) {
    location.href = 'entrar.html?voltar=' + encodeURIComponent(paginaAtual());
  }
  throw erro;
}
