# ReportaYa Backend

API REST para el registro y seguimiento de incidencias en conjuntos residenciales.

## Requisitos

- Python 3.11 o superior

## Ejecución local

Crear y activar un entorno virtual:

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
```

Instalar las dependencias:

```powershell
pip install -r requirements.txt
```

La base de datos central del proyecto es MySQL. Para configurar la conexión,
copiar `.env.example` como `.env` y completar los datos del servidor:

```env
DB_HOST=mysql-host.aivencloud.com
DB_PORT=12345
DB_NAME=defaultdb
DB_USER=avnadmin
DB_PASSWORD=reemplazar_con_la_contrasena
```

En Aiven, estos valores se encuentran en la sección `Overview` del servicio.
El archivo `.env` es privado y no debe subirse al repositorio.

Mientras no exista un archivo `.env`, la aplicación crea `reportaya_dev.db`, una base SQLite local
para facilitar el desarrollo. Esta base es temporal y no reemplaza la base
MySQL del proyecto.

Iniciar el servidor:

```powershell
uvicorn main:app --reload
```

La API estará disponible en `http://127.0.0.1:8000` y su documentación en
`http://127.0.0.1:8000/docs`.
