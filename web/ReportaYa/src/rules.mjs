export function allowedTransitions(state) {
 return {Pendiente:['En proceso','Rechazado'], 'En proceso':['Atendido'], Atendido:[], Rechazado:[]}[state] ?? [];
}
export function validateForm(data) {
 const errors = [];
 if (data.descripcion.trim().length < 10 || data.descripcion.trim().length > 250) errors.push('La descripción debe tener entre 10 y 250 caracteres.');
 if (!data.ubicacion.trim() || data.ubicacion.trim().length > 60) errors.push('La ubicación debe tener entre 1 y 60 caracteres.');
 if (!Number.isInteger(data.idTipoIncidencia) || data.idTipoIncidencia < 1) errors.push('Selecciona un tipo de incidencia.');
 if (!Number.isInteger(data.idUsuarioRegistro) || data.idUsuarioRegistro < 1) errors.push('Selecciona un residente.');
 return errors;
}
export function safePhoto(url) { try {const u=new URL(url);return ['http:','https:'].includes(u.protocol) ? u.href : null;} catch {return null;} }
export function localDate(date = new Date()) { return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`; }
