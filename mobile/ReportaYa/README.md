# ReportaYa Android

Aplicación Android para residentes de ReportaYa.

## Funcionalidades

- Inicio de sesión de residentes.
- Registro, listado, búsqueda y detalle de incidencias.
- Persistencia local con SQLite.
- Operaciones pendientes cuando no hay conexión.
- Sincronización con la API REST mediante Retrofit.

## API

La aplicación usa por defecto:

```text
https://reportaya-api.onrender.com/
```

El backend debe exponer `POST /api/auth/login` para autenticar al residente. El resto de operaciones utiliza los endpoints de incidencias y tipos de incidencia del proyecto.
