package pe.edu.cibertec.reportaya.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ReportaYaDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_INCIDENT_TABLE)
        db.execSQL(CREATE_INDEX_STATE)
        db.execSQL(CREATE_INDEX_SYNC)
        db.execSQL(CREATE_INDEX_UPDATED)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            //  se retiran únicamente los cinco registros demostrativos que
            // venían precargados en versiones anteriores. Los reportes creados por el usuario se conservan.
            db.delete(
                TABLE_INCIDENT,
                "$COL_USER = ? AND $COL_CODE IN (?,?,?,?,?)",
                arrayOf(DEMO_USER, "INC-0001", "INC-0002", "INC-0003", "INC-0004", "INC-0005")
            )
        }
    }

    companion object {
        const val DATABASE_NAME = "reportaya_local.db"
        const val DATABASE_VERSION = 2
        const val TABLE_INCIDENT = "incidencia_local"

        const val COL_ID = "id_local"
        const val COL_SERVER_ID = "id_servidor"
        const val COL_LOCAL_UUID = "uuid_local"
        const val COL_CODE = "codigo"
        const val COL_TYPE = "tipo"
        const val COL_DESCRIPTION = "descripcion"
        const val COL_LOCATION = "ubicacion"
        const val COL_STATE = "estado"
        const val COL_REGISTERED_AT = "fecha_registro"
        const val COL_USER = "usuario"
        const val COL_PHOTO_URI = "foto_uri"
        const val COL_ATTENTION_DATE = "fecha_atencion"
        const val COL_ATTENTION_OBSERVATION = "observacion_atencion"
        const val COL_SYNC_STATUS = "estado_sincronizacion"
        const val COL_IS_DELETED = "eliminado"
        const val COL_UPDATED_AT = "fecha_actualizacion_epoch"
        const val COL_LAST_ERROR = "ultimo_error"

        private const val DEMO_USER = "residente.demo@reportaya.pe"

        private val CREATE_INCIDENT_TABLE = """
            CREATE TABLE $TABLE_INCIDENT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SERVER_ID INTEGER NULL,
                $COL_LOCAL_UUID TEXT NOT NULL UNIQUE,
                $COL_CODE TEXT NOT NULL UNIQUE,
                $COL_TYPE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL CHECK(length($COL_DESCRIPTION) BETWEEN 10 AND 250),
                $COL_LOCATION TEXT NOT NULL,
                $COL_STATE TEXT NOT NULL CHECK($COL_STATE IN ('Pendiente','En proceso','Atendido','Rechazado')),
                $COL_REGISTERED_AT TEXT NOT NULL,
                $COL_USER TEXT NOT NULL,
                $COL_PHOTO_URI TEXT NULL,
                $COL_ATTENTION_DATE TEXT NULL,
                $COL_ATTENTION_OBSERVATION TEXT NULL,
                $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'PENDIENTE_CREAR'
                    CHECK($COL_SYNC_STATUS IN ('SINCRONIZADO','PENDIENTE_CREAR','PENDIENTE_ACTUALIZAR','PENDIENTE_ELIMINAR','ERROR')),
                $COL_IS_DELETED INTEGER NOT NULL DEFAULT 0 CHECK($COL_IS_DELETED IN (0,1)),
                $COL_UPDATED_AT INTEGER NOT NULL,
                $COL_LAST_ERROR TEXT NULL
            )
        """.trimIndent()

        private const val CREATE_INDEX_STATE =
            "CREATE INDEX idx_incidencia_local_estado ON $TABLE_INCIDENT($COL_STATE)"
        private const val CREATE_INDEX_SYNC =
            "CREATE INDEX idx_incidencia_local_sync ON $TABLE_INCIDENT($COL_SYNC_STATUS)"
        private const val CREATE_INDEX_UPDATED =
            "CREATE INDEX idx_incidencia_local_actualizacion ON $TABLE_INCIDENT($COL_UPDATED_AT DESC)"
    }
}
