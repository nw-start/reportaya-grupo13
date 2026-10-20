import {test} from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {request} from '../src/api.mjs';

test('contrato HTTP de registro, edición, búsqueda y baja lógica', async () => {
 const received=[];
 const server=http.createServer(async(req,res)=>{
  let raw='';for await(const chunk of req)raw+=chunk;
  received.push({method:req.method,path:req.url,body:raw?JSON.parse(raw):null});
  res.setHeader('Content-Type','application/json');
  if(req.method==='DELETE'){res.writeHead(204);res.end();return}
  if(req.url==='/invalid'){res.writeHead(422);res.end(JSON.stringify({detail:[{msg:'Descripción demasiado corta'}]}));return}
  if(req.method==='POST'){res.writeHead(201);res.end(JSON.stringify({idIncidencia:50,estado:'Pendiente'}));return}
  res.end(JSON.stringify({idIncidencia:50,descripcion:'Descripción actualizada'}));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const base='http://127.0.0.1:'+server.address().port;
 try{
  const alta=await request('/api/incidencias','POST',{descripcion:'Fuga de agua en la entrada'},base);
  assert.equal(alta.estado,'Pendiente');
  await request('/api/incidencias/50','PATCH',{descripcion:'Descripción actualizada'},base);
  await request('/api/incidencias/buscar?criterio=Fuga%20de%20agua','GET',undefined,base);
  assert.equal(await request('/api/incidencias/50','DELETE',undefined,base),null);
  await assert.rejects(request('/invalid','GET',undefined,base),/Descripción demasiado corta/);
  assert.equal(received[0].body.descripcion,'Fuga de agua en la entrada');
  assert.equal(received[1].method,'PATCH');
  assert.equal(received[2].path,'/api/incidencias/buscar?criterio=Fuga%20de%20agua');
  assert.equal(received[3].method,'DELETE');
 }finally{await new Promise(resolve=>server.close(resolve))}
});
