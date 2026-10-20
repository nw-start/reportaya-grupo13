export type Estado = 'Pendiente' | 'En proceso' | 'Atendido' | 'Rechazado';
export interface Tipo { idTipoIncidencia: number; nombre: string; activo: boolean }
export interface Usuario { idUsuario: number; nombres: string }
export interface Incidencia {
 idIncidencia: number; codigo: string; fechaRegistro: string; horaRegistro: string;
 descripcion: string; ubicacion: string; fotoUrl: string | null; estado: Estado;
 fechaAtencion: string | null; observacionAtencion: string | null; eliminado: boolean;
 fechaActualizacion: string; idTipoIncidencia: number; idUsuarioRegistro: number;
 tipoIncidencia: Tipo; usuarioRegistro: Usuario;
}
export interface Alta { fechaRegistro: string; horaRegistro: string; descripcion: string; ubicacion: string; fotoUrl: string | null; idTipoIncidencia: number; idUsuarioRegistro: number }
export interface Cambio { descripcion?: string; ubicacion?: string; idTipoIncidencia?: number; estado?: Estado; observacionAtencion?: string }
export const ESTADOS: Estado[] = ['Pendiente','En proceso','Atendido','Rechazado'];

export interface UsuarioSesion extends Usuario {
 correo: string;
 rol: 'Residente' | 'Administrador';
 activo: boolean;
 fechaCreacion: string;
}
