// Erros junto aos campos (06_FRONTEND §4). Campo "x": <input name="x" id="x"> e <span id="erro-x" class="erro">.
export function limparErros(form) {
  form.querySelectorAll('[id^="erro-"]').forEach((s) => { s.textContent = ''; });
  form.querySelectorAll('[aria-invalid]').forEach((el) => el.removeAttribute('aria-invalid'));
}

// campos: { campo: 'mensagem' } (aceita também lista [{campo, mensagem}]). Foca o primeiro campo com erro.
export function mostrarErros(form, campos) {
  const mapa = Array.isArray(campos)
    ? Object.fromEntries(campos.filter((c) => c && c.campo).map((c) => [c.campo, c.mensagem]))
    : (campos || {});
  let primeiro = null;
  Object.entries(mapa).forEach(([nome, msg]) => {
    const span = form.querySelector('#erro-' + nome);
    const entradas = form.querySelectorAll('[name="' + nome + '"]');
    if (!span || !entradas.length) return;
    span.textContent = msg;
    entradas.forEach((el) => el.setAttribute('aria-invalid', 'true'));
    if (!primeiro) primeiro = entradas[0];
  });
  if (primeiro) primeiro.focus();
  return !!primeiro;
}

// Região global: tipo 'erro' (role="alert") ou 'ok' (aria-live="polite"). Texto vazio limpa.
export function mostrarMensagem(regiao, texto, tipo = 'erro') {
  regiao.textContent = texto || '';
  regiao.className = 'mensagem' + (texto ? ' mensagem--' + tipo : '');
}
