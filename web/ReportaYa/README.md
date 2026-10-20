# ReportaYa

Portal administrativo para gestionar las incidencias de los residentes.

Proyecto del curso Desarrollo de Aplicaciones Móviles I — Grupo 13.

## Portal publicado

[Abrir el portal web ReportaYa](https://reportaya-portal.onrender.com)

Para ingresar se necesita una cuenta activa con rol Administrador.

## Funcionalidades

- Inicio de sesión con una cuenta de administrador.
- Listado, búsqueda y consulta del detalle de incidencias.
- Registro y edición de incidencias.
- Cambio de estado y registro de observaciones de atención.
- Eliminación lógica de incidencias.

## Tecnologías

- React y TypeScript.
- Vite.
- Node.js para servir el portal y comunicarlo con la API REST.

## Requisitos

- Node.js 22.12 o superior dentro de la versión 22.
- npm.
- Una cuenta activa con rol Administrador registrada en la API.

## Ejecución local

Abrir la carpeta del portal en Visual Studio Code y ejecutar en la terminal:

```sh
npm install
npm run dev
```

Abrir http://127.0.0.1:5174 y mantener la terminal abierta.

Si PowerShell bloquea npm, utilizar `npm.cmd install` y `npm.cmd run dev`.
Para aplicar cambios en el código, detener el servidor con Ctrl+C y volver a ejecutar el comando.

## API

El portal se conecta por defecto a:

```text
https://reportaya-api.onrender.com
```

Documentación: https://reportaya-api.onrender.com/docs

El inicio de sesión utiliza `POST /api/auth/login`. Las solicitudes protegidas
envían el token recibido mediante `Authorization: Bearer`. El token se mantiene
en memoria; al recargar la página o vencer la sesión se debe ingresar nuevamente.

El portal no se conecta directamente a MySQL ni necesita las credenciales de
la base de datos o JWT_SECRET.

## Despliegue en Render

Crear un Web Service conectado al repositorio y seleccionar la rama del portal.
Si los archivos están dentro de `web`, establecer Root Directory en `web`.
Si están en la raíz del repositorio, dejar ese campo vacío.

```text
Language: Node
Build Command: npm ci --include=dev && npm run build
Start Command: npm start
```

Agregar la variable de entorno:

```text
API_URL=https://reportaya-api.onrender.com
```

Render asigna automáticamente el puerto y la dirección pública del servicio.
Si se configura un dominio propio, establecer PUBLIC_URL con su dirección HTTPS completa.

No subir `node_modules` ni archivos privados `.env` al repositorio.
Después del despliegue, comprobar el inicio de sesión y las operaciones de
incidencias desde el enlace público.
