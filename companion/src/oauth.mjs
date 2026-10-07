import { execFile } from 'node:child_process';
import { createRequire } from 'node:module';
import { promisify } from 'node:util';
const require=createRequire(import.meta.url);
const exec=promisify(execFile);
export async function oauthStatus(env, runner=exec) {
  try {
    const {stdout,stderr}=await runner(process.execPath,[require.resolve('@openai/codex/bin/codex.js'),'login','status'],{env,timeout:10000,maxBuffer:16384});
    // Inspect only the CLI's reported auth method; never expose its raw output.
    return { signedIn:/logged in using chatgpt/i.test(stdout+'\n'+stderr), method:'official-codex-chatgpt' };
  } catch { return {signedIn:false,method:'official-codex-chatgpt'}; }
}
