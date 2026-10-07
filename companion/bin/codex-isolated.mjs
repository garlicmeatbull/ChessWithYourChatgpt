#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';

// Official CLI authentication is retained. User hooks/MCP/project configuration
// and conversation transcripts are not needed for a bounded chess explanation.
const require=createRequire(import.meta.url);
const cli=require.resolve('@openai/codex/bin/codex.js');
const args=process.argv.slice(2);
if(args[0]==='exec')args.splice(1,0,'--ignore-user-config','--ignore-rules','--ephemeral');
const child=spawn(process.execPath,[cli,...args],{stdio:'inherit',env:process.env});
for(const signal of ['SIGTERM','SIGINT'])process.on(signal,()=>child.kill(signal));
child.on('error',()=>{console.error('Could not start official Codex CLI');process.exitCode=1;});
child.on('exit',(code,signal)=>{process.exitCode=code??(signal?1:0);});
