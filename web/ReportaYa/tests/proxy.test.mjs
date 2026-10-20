import {test} from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {spawn} from 'node:child_process';
import {fileURLToPath} from 'node:url';

for (const published of [false,true]) {
test('servidor reenvía login y Authorization al backend: '+(published?'HTTPS publicado':'local'),async()=>{
 const received=[];
 const backend=http.createServer(async(req,res)=>{
  let body='';for await(const chunk of req)body+=chunk;
  received.push({path:req.url,authorization:req.headers.authorization,body:body?JSON.parse(body):null});
  res.setHeader('Content-Type','application/json');
  if(req.url==='/api/auth/login'){res.end(JSON.stringify({accessToken:'local-token',tokenType:'bearer'}));return}
  if(req.headers.authorization!=='Bearer local-token'){res.writeHead(401);res.end(JSON.stringify({detail:'Debe iniciar sesion.'}));return}
  res.end(JSON.stringify({rol:'Administrador',activo:true}));
 });
 await new Promise(resolve=>backend.listen(0,'127.0.0.1',resolve));
 const root=fileURLToPath(new URL('../',import.meta.url));
 const child=spawn(process.execPath,['server.mjs'],{cwd:root,env:{...process.env,PORT:'0',API_URL:'http://127.0.0.1:'+backend.address().port,RENDER:published?'true':'',RENDER_EXTERNAL_URL:published?'https://portal-prueba.onrender.com':'',PUBLIC_URL:''},stdio:['ignore','pipe','pipe']});
 try{
  const base=await new Promise((resolve,reject)=>{
   const timer=setTimeout(()=>reject(new Error('El servidor no inició.')),10000);
   child.once('error',reject);
   child.stdout.on('data',chunk=>{const match=chunk.toString().match(/http:\/\/127\.0\.0\.1:\d+/);if(match){clearTimeout(timer);resolve(match[0])}});
  });
  const login=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json',Origin:published?'https://portal-prueba.onrender.com':base},body:JSON.stringify({correo:'cuenta@example.test',contrasena:'clave-prueba'})});
  assert.equal(login.status,200);
  assert.equal((await login.json()).accessToken,'local-token');
  const me=await fetch(base+'/api/auth/me',{headers:{Authorization:'Bearer local-token'}});
  assert.equal(me.status,200);
  assert.equal(received[1].authorization,'Bearer local-token');
  assert.equal((await fetch(base+'/api/incidencias')).status,401);
  assert.equal((await fetch(base+'/api/no-existe')).status,404);
  const blocked=await fetch(base+'/api/auth/login',{method:'POST',headers:{Origin:'http://otro-origen.test'}});
  assert.equal(blocked.status,403);
 }finally{
  const exit=new Promise(resolve=>child.once('exit',resolve));child.kill();await exit;
  await new Promise(resolve=>backend.close(resolve));
 }
});
}
