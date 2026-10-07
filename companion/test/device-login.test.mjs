import test from 'node:test';
import assert from 'node:assert/strict';
import {EventEmitter} from 'node:events';
import {PassThrough} from 'node:stream';
import {createDeviceLogin} from '../src/device-login.mjs';
function fake(){const p=new EventEmitter();p.stdout=new PassThrough();p.stderr=new PassThrough();p.kill=()=>{};return p;}
test('official device prompt is parsed across chunks; secrets and raw output stay private',async()=>{
  const p=fake();let signedIn=false,launches=0;
  const login=createDeviceLogin({}, {cliPath:'fake-cli',status:async()=>({signedIn}),spawnImpl:()=>{launches++;return p;}});
  const starting=login.start();await new Promise(r=>setImmediate(r));
  p.stderr.write('https://auth.openai.com/codex/device\n2. Enter this one-time code (expires in 15 minutes)\n\x1b[32mABCD-');
  p.stderr.write('12345\x1b[0m\nprivate-token-that-must-not-escape\n');
  const result=await starting;assert.equal(result.userCode,'ABCD-12345');assert.equal(result.verificationUrl,'https://auth.openai.com/codex/device');assert(!JSON.stringify(result).includes('private-token'));
  assert.equal((await login.start()).status,'waiting');assert.equal(launches,1);
  signedIn=true;p.emit('close',0);await new Promise(r=>setImmediate(r));assert.deepEqual(login.get(),{status:'signedIn'});login.close();
});
test('CLI failure clears the device code and does not disclose stderr',async()=>{
 const p=fake(),login=createDeviceLogin({}, {cliPath:'fake-cli',status:async()=>({signedIn:false}),spawnImpl:()=>p});
 const start=login.start();await new Promise(r=>setImmediate(r));p.stderr.write('private auth failure');p.emit('close',1);assert.deepEqual(await start,{status:'failed'});login.close();
});
test('already signed in does not spawn a second OAuth process',async()=>{
 const login=createDeviceLogin({}, {status:async()=>({signedIn:true}),spawnImpl:()=>{throw Error('should not launch');}});
 assert.deepEqual(await login.start(),{status:'signedIn'});login.close();
});
