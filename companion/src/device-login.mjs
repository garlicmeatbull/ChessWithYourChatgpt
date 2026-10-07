import {spawn} from 'node:child_process';
import {createRequire} from 'node:module';
import {oauthStatus} from './oauth.mjs';
const require=createRequire(import.meta.url);
// Only the official CLI handles OAuth tokens. Phone receives an expiring device code.
export function createDeviceLogin(env,{spawnImpl=spawn,status=()=>oauthStatus(env),cliPath}={}) {
  let child=null,session={status:'idle'},timer=null,output='',ready=null;
  const view=()=>({...session});
  const finish=(phase)=>{clearTimeout(timer);session={status:phase};if(ready){ready(view());ready=null;}};
  return {
    get:view,
    async start(){
      if((await status()).signedIn)return {status:'signedIn'};
      if(child){if(session.status==='waiting')return view();return {status:'starting'};}
      session={status:'starting'};output='';
      return new Promise(resolve=>{
        ready=resolve;
        try{child=spawnImpl(process.execPath,[cliPath||require.resolve('@openai/codex/bin/codex.js'),'login','--device-auth'],{env,stdio:['ignore','pipe','pipe']});}
        catch{finish('failed');child=null;return;}
        const proc=child;
        const consume=chunk=>{
          output=(output+chunk.toString()).replace(/\x1b\[[0-9;]*m/g,'').slice(-16384);
          const code=output.match(/one-time code[^\n]*\n\s*([A-Z0-9-]{6,24})[ \t]*\r?\n/i);
          if(code&&output.includes('https://auth.openai.com/codex/device')){
            session={status:'waiting',verificationUrl:'https://auth.openai.com/codex/device',userCode:code[1].toUpperCase()};
            if(ready){ready(view());ready=null;}
          }
        };
        proc.stdout.on('data',consume);proc.stderr.on('data',consume);
        proc.once('error',()=>{if(child===proc){child=null;finish('failed');}});
        proc.once('close',async code=>{
          if(child!==proc)return;child=null;
          if(session.status==='expired')return;
          finish(code===0&&(await status()).signedIn?'signedIn':'failed');
        });
        timer=setTimeout(()=>{child=null;proc.kill();finish('expired');},15*60*1000);timer.unref();
        // Never keep an HTTP request open while a slow CLI starts.
        const prompt=setTimeout(()=>{if(ready){ready(view());ready=null;}},8000);prompt.unref();
      });
    },
    close(){clearTimeout(timer);const proc=child;child=null;proc?.kill();finish('idle');output='';}
  };
}
