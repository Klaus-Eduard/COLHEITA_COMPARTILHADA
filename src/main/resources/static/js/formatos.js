// Formatadores pt-BR (06_FRONTEND §1).
const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const NUMERO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });
const UNIDADES = {
  KG: ['kg', 'kg'], LITRO: ['litro', 'litros'], UNIDADE: ['unidade', 'unidades'],
  MACO: ['maço', 'maços'], DUZIA: ['dúzia', 'dúzias'],
};

export const apenasDigitos = (v) => String(v ?? '').replace(/\D/g, '');
export const emailValido = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(v ?? '').trim());

export function moeda(valor) { return MOEDA.format(Number(valor)); }

export function nomeUnidade(unidade, quantidade = 1) {
  const par = UNIDADES[unidade];
  return par ? (Number(quantidade) === 1 ? par[0] : par[1]) : String(unidade ?? '');
}

export function quantidadeUnidade(quantidade, unidade) {
  return NUMERO.format(Number(quantidade)) + ' ' + nomeUnidade(unidade, quantidade);
}

// "R$ 12,00 / dúzia" ou "a combinar"; doação: "Doação"
export function preco(valor, unidade, modalidade) {
  if (modalidade === 'DOACAO') return 'Doação';
  if (valor === null || valor === undefined || valor === '') return 'a combinar';
  return moeda(valor) + ' / ' + nomeUnidade(unidade, 1);
}

// Aceita "2026-11-20" (data) ou instante ISO. Saída dd/mm/aaaa.
export function dataBR(texto) {
  if (!texto) return '';
  if (/^\d{4}-\d{2}-\d{2}$/.test(texto)) { const [a, m, d] = texto.split('-'); return d + '/' + m + '/' + a; }
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(texto));
}

// (47) 99999-9999 ou (47) 9999-9999
export function mascaraWhatsApp(v) {
  const d = apenasDigitos(v).slice(0, 11);
  if (d.length <= 2) return d;
  if (d.length <= 6) return '(' + d.slice(0, 2) + ') ' + d.slice(2);
  const corte = d.length > 10 ? 7 : 6;
  return '(' + d.slice(0, 2) + ') ' + d.slice(2, corte) + '-' + d.slice(corte);
}

// "Disponível até dd/mm": expira_em é o limite exclusivo (início do dia seguinte em São Paulo, ou criado_em + 7 dias).
// Por isso formatamos 1 ms antes: 2026-10-25T03:00Z vira 24/10 (último dia de disponibilidade).
export function dataDisponivelAte(iso) {
  if (!iso) return '';
  const ultimoInstante = new Date(new Date(iso).getTime() - 1);
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'America/Sao_Paulo' }).format(ultimoInstante);
}

export const rotuloTipo = (t) => ({ MORADOR: 'Morador', PEQUENO_PRODUTOR: 'Pequeno produtor' }[t] || '');

// Datas de calendário em America/Sao_Paulo no formato aaaa-mm-dd (02_REGRAS: fuso para datas de calendário)
const FMT_ISO = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' });
export const hojeSP = () => FMT_ISO.format(new Date());
export function somarDias(iso, n) {
  const [a, m, d] = iso.split('-').map(Number);
  return new Date(Date.UTC(a, m - 1, d + n)).toISOString().slice(0, 10);
}
// Último dia de disponibilidade a partir de expiraEm (limite exclusivo): 1 ms antes, como em dataDisponivelAte
export const dataIsoDisponivelAte = (expiraEm) => FMT_ISO.format(new Date(new Date(expiraEm).getTime() - 1));
