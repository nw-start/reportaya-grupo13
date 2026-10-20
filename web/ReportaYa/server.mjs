import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {resolve,extname,sep} from 'node:path';

const port = Number(process.env.PORT || 5174);
const host = process.env.HOST || (process.env.RENDER ? '0.0.0.0' : '127.0.0.1');
const publicOrigin = process.env.PUBLIC_URL || process.env.RENDER_EXTERNAL_URL;
const allowedOrigin = publicOrigin ? new URL(publicOrigin).origin : null;
const target = new URL(process.env.API_URL || 'https://reportaya-api.onrender.com');
const root = resolve('dist');
const mime = {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.json':'application/json'};
const routes = [
 [/^\/api\/auth\/login$/, ['POST']],
 [/^\/api\/auth\/me$/, ['GET']],
 [/^\/api\/incidencias$/, ['GET','POST']],
 [/^\/api\/incidencias\/buscar$/, ['GET']],
 [/^\/api\/incidencias\/\d+$/, ['GET','PATCH','DELETE']],
 [/^\/api\/tipos-incidencia$/, ['GET']],
 [/^\/api\/usuarios$/, ['GET']]
];
function sendError(res,status,detail) {
 res.writeHead(status,{'Content-Type':'application/json'});
 res.end(JSON.stringify({detail}));
}
const server = http.createServer(async (req,res) => {
 try {
  const url = new URL(req.url, 'http://localhost');
  if (url.pathname.startsWith('/api/')) {
   const route = routes.find(([pattern]) => pattern.test(url.pathname));
   if (!route) return sendError(res,404,'Ruta no disponible en el portal.');
   if (!route[1].includes(req.method)) return sendError(res,405,'Método no permitido.');
   const origin = req.headers.origin;
   if (origin && origin !== (allowedOrigin || 'http://' + req.headers.host)) return sendError(res,403,'Origen no permitido.');
   const chunks = [];
   let size = 0;
   for await (const chunk of req) {
    size += chunk.length;
    if (size > 65536) return sendError(res,413,'La solicitud es demasiado grande.');
    chunks.push(chunk);
   }
   const body = Buffer.concat(chunks);
   const headers = body.length ? {'Content-Type':'application/json'} : {};
   if (req.headers.authorization) headers.Authorization = req.headers.authorization;
   const response = await fetch(new URL(url.pathname + url.search, target), {
    method: req.method,
    headers,
    body: body.length ? body : undefined,
    signal: AbortSignal.timeout(60000),
    redirect: 'error'
   });
   res.writeHead(response.status, {'Content-Type':response.headers.get('content-type') || 'application/json','Cache-Control':'no-store'});
   return res.end(Buffer.from(await response.arrayBuffer()));
  }
  if (req.method !== 'GET' && req.method !== 'HEAD') return sendError(res,405,'Método no permitido.');
  const file = resolve(root, '.' + decodeURIComponent(url.pathname === '/' ? '/index.html' : url.pathname));
  if (!file.startsWith(root + sep)) return sendError(res,403,'Ruta no permitida.');
  try {
   const data = await readFile(file);
   res.writeHead(200,{'Content-Type':mime[extname(file)] || 'application/octet-stream'});
   res.end(req.method === 'HEAD' ? undefined : data);
  } catch {
   sendError(res,404,'Archivo no encontrado. Ejecuta npm run build antes de npm start.');
  }
 } catch {
  sendError(res,502,'No fue posible comunicarse con la API. Intenta nuevamente.');
 }
});
server.listen(port,host,()=>console.log(`ReportaYa: http://127.0.0.1:${server.address().port}${allowedOrigin ? ' | ' + allowedOrigin : ''}`));
