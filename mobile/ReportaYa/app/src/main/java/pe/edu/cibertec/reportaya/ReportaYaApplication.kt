package pe.edu.cibertec.reportaya

import android.app.Application
import pe.edu.cibertec.reportaya.data.IncidentDataSourceRegistry
import pe.edu.cibertec.reportaya.data.UserSession

class ReportaYaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UserSession.initialize(this)
        IncidentDataSourceRegistry.initialize(this)
    }
}
