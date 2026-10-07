import { createHash } from 'node:crypto';

export const OUTPUT_SCHEMA = {
  type: 'object', additionalProperties: false,
  properties: {
    flow: { type: 'string' }, bestMoveReason: { type: 'string' }, plan: { type: 'string' },
  }, required: ['flow', 'bestMoveReason', 'plan'],
};
const move = /^[a-h][1-8][a-h][1-8][qrbn]?$/;
const integer = (n, bound = 100000) => Number.isInteger(n) && Math.abs(n) <= bound;
function validFen(fen) {
  if (typeof fen !== 'string' || fen.length > 110) return false;
  const parts = fen.split(' ');
  if (parts.length !== 6 || !/^[wb]$/.test(parts[1]) || !/^(?:-|K?Q?k?q?)$/.test(parts[2]) || !/^(?:-|[a-h][36])$/.test(parts[3]) || !/^\d{1,5}$/.test(parts[4]) || !/^[1-9]\d{0,4}$/.test(parts[5])) return false;
  const ranks = parts[0].split('/');
  return ranks.length === 8 && ranks.every(r => /^[prnbqkPRNBQK1-8]+$/.test(r) && [...r].reduce((n,c) => n + (/\d/.test(c) ? Number(c) : 1),0) === 8)
    && [...parts[0]].filter(c => c === 'K').length === 1 && [...parts[0]].filter(c => c === 'k').length === 1;
}
function score(s) {
  if (!s || !integer(s.cp) || !(s.mate === null || integer(s.mate,1000)) || !integer(s.depth,128) || s.depth < 0) throw new Error('Invalid score');
  return { cp:s.cp, mate:s.mate, depth:s.depth };
}
export function validate(input, models, defaultModel) {
  if (!input || !validFen(input.before) || !validFen(input.after) || !move.test(input.played)) throw new Error('Invalid position or move');
  const model = input.model || defaultModel;
  if (!models.includes(model)) throw new Error('Model not allowed');
  if (!Array.isArray(input.candidates) || input.candidates.length < 1 || input.candidates.length > 3) throw new Error('Provide 1–3 candidates');
  const candidates = input.candidates.map(c => {
    if (!move.test(c.move) || !Array.isArray(c.pv) || c.pv.length < 1 || c.pv.length > 6 || c.pv[0] !== c.move || !c.pv.every(p => typeof p === 'string' && move.test(p))) throw new Error('Invalid variation');
    return { move:c.move, ...score(c), pv:c.pv };
  });
  if (!Array.isArray(input.trend) || input.trend.length > 6 || !input.trend.every(n=>integer(n))) throw new Error('Invalid trend');
  const focus=input.learningFocus??null;
  if(focus!==null&&!['candidate-comparison','defensive-resource','forcing-move','good-decision'].includes(focus))throw new Error('Invalid learning focus');
  return { before:input.before,after:input.after,played:input.played,model,candidates,playedScore:input.playedScore?score(input.playedScore):null,trend:input.trend,learningFocus:focus };
}
export function prompt(data) {
  // Short, stateless engine evidence instead of history, PGN, screenshots, or agent tools.
  return `한국어 체스 코치. 아래 Stockfish 19 분석을 설명하라. cp와 mate는 백 기준이며 depth는 제한된 탐색이다. 후보 순서는 착수 전 차례 쪽의 선호도다. FEN/수는 UCI다. 과거 추세 cp는 백 기준이다.\n`+
    `flow:이번 수의 평가 변화와 전체 흐름. bestMoveReason:최상위 후보의 근거를 기물/칸/제공된 PV와 연결. plan:양측의 다음 계획과 learningFocus가 있으면 그 판단을 재현할 연습. 각 필드 1~2문장, 전체 700자 이내. 근거 없는 강제수·탁월수·승리 확정을 금지. PV만으로 확인되지 않는 전략은 추정이라고 표시. 메이트는 cp보다 우선. 외부 자료·파일·도구 사용 금지. JSON 스키마대로만 답하라.\n`+
    JSON.stringify({ ...data, model:undefined });
}
export function createCoach(provider, { models, defaultModel, maxCache=128, ttl=86400000 }={}) {
  const cache = new Map(); let running = false;
  return {
    models, defaultModel,
    async explain(raw) {
      const data=validate(raw,models,defaultModel);
      const key=createHash('sha256').update('prompt-v1:'+JSON.stringify(data)).digest('hex');
      const old=cache.get(key);
      if (old && old.until > Date.now()) return { ...old.value, cached:true };
      if (running) { const e=new Error('Coach busy');e.status=429;throw e; }
      running=true;
      try {
        const turn = await provider(data.model,prompt(data),OUTPUT_SCHEMA);
        const explanation = typeof turn.text === 'string' ? JSON.parse(turn.text) : turn.text;
        if (!explanation || Object.keys(explanation).length !== 3 || !['flow','bestMoveReason','plan'].every(k=>typeof explanation[k]==='string' && explanation[k].length>0 && explanation[k].length<=1500)) throw new Error('Invalid coach output');
        const value={ explanation,model:data.model,cached:false,usage:turn.usage??null };
        cache.set(key,{ value,until:Date.now()+ttl });
        while(cache.size>maxCache)cache.delete(cache.keys().next().value);
        return value;
      } finally { running=false; }
    }
  };
}
