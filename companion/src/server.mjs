import http from 'node:http';
import https from 'node:https';
import { readFileSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { createCoach } from './coach.mjs';
import { oauthStatus } from './oauth.mjs';
import {createDeviceLogin} from './device-login.mjs';
import { fileURLToPath } from 'node:url';

export function createHandler(coach, token) {
  if (typeof token !== 'string' || token.length < 32) throw new Error('COACH_PAIRING_TOKEN must have at least 32 characters');
  return async (req,res) => {
    const reply=(status,value)=>{res.writeHead(status,{'content-type':'application/json; charset=utf-8','cache-control':'no-store'});res.end(JSON.stringify(value));};
    if(req.method==='GET' && req.url==='/health')return reply(200,{status:'ok',service:'chess-coach',oauth:'checked when explaining'});
    const actual=Buffer.from(req.headers.authorization||'');const expected=Buffer.from('Bearer '+token);
    if(actual.length!==expected.length||!timingSafeEqual(actual,expected))return reply(401,{error:'Unauthorized'});
    if(req.method==='GET' && req.url==='/v1/models')return reply(200,{models:coach.models,defaultModel:coach.defaultModel});
    if(req.method==='GET' && req.url==='/v1/auth/status')return reply(200,await coach.authStatus());
    if(req.method==='GET'&&req.url==='/v1/auth/device')return reply(200,coach.deviceLogin?.get()||{status:'unavailable'});
    if(req.method==='POST'&&req.url==='/v1/auth/device'){
      if(!coach.deviceLogin)return reply(503,{error:'Device login unavailable'});
      let size=0;for await(const part of req){size+=part.length;if(size>1024)return reply(413,{error:'Payload too large'});}
      try{return reply(200,await coach.deviceLogin.start());}catch{return reply(502,{error:'Official Codex device login unavailable'});}
    }
    if(req.method!=='POST'||req.url!=='/v1/explain')return reply(404,{error:'Not found'});
    if(!req.headers['content-type']?.startsWith('application/json'))return reply(415,{error:'JSON required'});
    let size=0,parts=[];
    try {
      for await(const part of req) {
        size+=part.length;if(size>8192){reply(413,{error:'Payload too large'});return;}
        parts.push(part);
      }
      let raw;try{raw=JSON.parse(Buffer.concat(parts).toString('utf8'));}catch{return reply(400,{error:'Invalid JSON'});}
      // Validate separately to keep provider/auth failures distinct from client input errors.
      const {validate}=await import('./coach.mjs');
      try{validate(raw,coach.models,coach.defaultModel);}catch(e){return reply(400,{error:e.message});}
      const result=await coach.explain(raw);reply(200,result);
    }catch(e){reply(e.status||502,{error:e.status===429?'Coach busy':'Codex unavailable; check OAuth login and model access on the companion'});}
  };
}

export async function start(env=process.env) {
  const models=(env.COACH_MODELS||'gpt-5.4,gpt-5.3-codex').split(',').map(s=>s.trim()).filter(Boolean);
  if(!models.length||models.some(s=>! /^[a-zA-Z0-9._-]+$/.test(s)))throw new Error('Invalid COACH_MODELS');
  const defaultModel=env.COACH_DEFAULT_MODEL||models[0];if(!models.includes(defaultModel))throw new Error('Default model must be in COACH_MODELS');
  const token=env.COACH_PAIRING_TOKEN;
  if(!token||token.length<32)throw new Error('Set COACH_PAIRING_TOKEN (32+ characters); never use a ChatGPT OAuth token');
  const {Codex}=await import('@openai/codex-sdk');
  // Keep the official CLI's OAuth credential handling. No secret files are read by this server.
  // API-key injection would silently change billing/auth: deliberately remove those bindings.
  const oauthEnv={...env};delete oauthEnv.OPENAI_API_KEY;delete oauthEnv.CODEX_API_KEY;
  const workspace=mkdtempSync(join(tmpdir(),'chess-coach-'));
  const provider=async(model,content,outputSchema)=>{
    const auth=await oauthStatus(oauthEnv);
    if(!auth.signedIn)throw new Error('Sign in using official Codex ChatGPT OAuth');
    const sdk=new Codex({codexPathOverride:fileURLToPath(new URL('../bin/codex-isolated.mjs',import.meta.url)),env:oauthEnv,
      config:{web_search:'disabled',forced_login_method:'chatgpt',sqlite_home:join(workspace,'state'),log_dir:join(workspace,'logs'),history:{persistence:'none'},features:{shell_tool:false,apply_patch_freeform:false}}});
    const thread=sdk.startThread({model,workingDirectory:workspace,skipGitRepoCheck:true,sandboxMode:'read-only',approvalPolicy:'never',networkAccessEnabled:false,webSearchMode:'disabled',modelReasoningEffort:'low'});
    const controller=new AbortController();const timeout=setTimeout(()=>controller.abort(),75000);
    try {
      const turn=await thread.run(content,{outputSchema,signal:controller.signal});
      return {text:turn.finalResponse,usage:turn.usage};
    }finally{clearTimeout(timeout);}
  };
  const coach=createCoach(provider,{models,defaultModel});
  coach.authStatus=()=>oauthStatus(oauthEnv);
  coach.deviceLogin=createDeviceLogin(oauthEnv);
  const handler=createHandler(coach,token);
  if(Boolean(env.COACH_TLS_CERT)!==Boolean(env.COACH_TLS_KEY))throw new Error('Provide both TLS certificate and key');
  const server=env.COACH_TLS_CERT?https.createServer({cert:readFileSync(env.COACH_TLS_CERT),key:readFileSync(env.COACH_TLS_KEY)},handler):http.createServer(handler);
  server.requestTimeout=90000;server.headersTimeout=10000;
  const host=env.COACH_HOST||'127.0.0.1',port=Number(env.COACH_PORT||8787);
  if(host!=='127.0.0.1'&&host!=='::1'&&!env.COACH_TLS_CERT)throw new Error('Remote listeners require TLS; use a local reverse proxy for HTTP');
  await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(port,host,resolve);});
  console.log(`Chess coach listening on ${host}:${port}; OAuth checked on first explanation. Models: ${models.join(', ')}`);
  server.on('close',()=>{coach.deviceLogin.close();rmSync(workspace,{recursive:true,force:true});});return server;
}
if(process.argv[1] && import.meta.url===pathToFileURL(process.argv[1]).href)start().catch(e=>{console.error(e.message);process.exitCode=1;});
