import {test} from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {request} from '../src/api.mjs';
import {getToken,setToken,clearToken,tokenExpiresAt} from '../src/session.mjs';

test('login público, token Bearer y expiración de sesión',async()=>{
 const received=[];
 const server=http.createServer(async(req,res)=>{
  let body='';for await(const chunk of req)body+=chunk;
  received.push({url:req.url,authorization:req.headers.authorization,body:body?JSON.parse(body):null});
  res.setHeader('Content-Type','application/json');
  if(req.url==='/api/auth/login'){res.end(JSON.stringify({accessToken:'token-prueba',tokenType:'bearer'}));return}
  if(req.url==='/api/auth/me'){res.end(JSON.stringify({rol:'Administrador',activo:true}));return}
  if(req.url==='/expired'){res.writeHead(401);res.end(JSON.stringify({detail:'Token invalido o expirado.'}));return}
  res.writeHead(403);res.end(JSON.stringify({detail:'No autorizado.'}));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const base='http://127.0.0.1:'+server.address().port;
 try{
  setToken('token-anterior');
  await request('/api/auth/login','POST',{correo:'cuenta@example.test',contrasena:'clave-prueba'},base);
  assert.equal(received[0].authorization,undefined);
  assert.equal(received[0].body.correo,'cuenta@example.test');
  setToken('token-prueba');
  const user=await request('/api/auth/me','GET',undefined,base);
  assert.equal(user.rol,'Administrador');
  assert.equal(received[1].authorization,'Bearer token-prueba');
  await assert.rejects(request('/forbidden','GET',undefined,base),/No autorizado/);
  assert.equal(getToken(),'token-prueba');
  await assert.rejects(request('/expired','GET',undefined,base),/expirado/);
  assert.equal(getToken(),'');
 }finally{clearToken();await new Promise(resolve=>server.close(resolve))}
});

test('fecha de expiración del token y cierre local',()=>{
 const exp=Math.floor(Date.now()/1000)+3600;
 const part=Buffer.from(JSON.stringify({exp})).toString('base64url');
 assert.equal(tokenExpiresAt('header.'+part+'.signature'),exp*1000);
 assert.equal(tokenExpiresAt('token-invalido'),0);
 setToken('temporal');clearToken();assert.equal(getToken(),'');
});
