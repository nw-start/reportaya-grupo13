import {useState,type FormEvent} from 'react';
import {api} from './api.mjs';
import {localDate,validateForm,safePhoto} from './rules.mjs';
import {Modal} from './components';
import type {Incidencia,Tipo,Usuario,Alta} from './types';
interface Props {incidencia:Incidencia|null;tipos:Tipo[];usuarios:Usuario[];cerrar:()=>void;guardado:()=>void}
export default function Formulario({incidencia,tipos,usuarios,cerrar,guardado}:Props) {
 const [datos,setDatos]=useState<Alta>({
  descripcion:incidencia?.descripcion||'',ubicacion:incidencia?.ubicacion||'',
  idTipoIncidencia:incidencia?.idTipoIncidencia||tipos[0]?.idTipoIncidencia||0,
  idUsuarioRegistro:incidencia?.idUsuarioRegistro||usuarios[0]?.idUsuario||0,
  fechaRegistro:incidencia?.fechaRegistro||localDate(),horaRegistro:incidencia?.horaRegistro||new Date().toTimeString().slice(0,8),fotoUrl:incidencia?.fotoUrl||null
 });
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 async function guardar(e:FormEvent) {
  e.preventDefault();
  const errors=validateForm(datos);
  if(!incidencia&&datos.fotoUrl&&!safePhoto(datos.fotoUrl))errors.push('Ingresa una URL HTTP o HTTPS para la foto.');
  if(errors.length){setError(errors.join(' '));return}
  setBusy(true);setError('');
  try {
   const fields={descripcion:datos.descripcion.trim(),ubicacion:datos.ubicacion.trim(),idTipoIncidencia:datos.idTipoIncidencia};
   if(incidencia)await api.actualizar(incidencia.idIncidencia,fields);
   else await api.registrar({...datos,...fields,fotoUrl:datos.fotoUrl?.trim()||null});
   guardado();
  } catch(e){setError((e as Error).message)}finally{setBusy(false)}
 }
 return <Modal titulo={incidencia?'Editar '+incidencia.codigo:'Registrar incidencia'} cerrar={cerrar} busy={busy}>
  {error&&<div className="error modal-error" role="alert">{error}</div>}
  <form onSubmit={guardar}>
   <div className="form-grid">
    <label className="field">Tipo de incidencia<select required disabled={busy} value={datos.idTipoIncidencia} onChange={e=>setDatos({...datos,idTipoIncidencia:Number(e.target.value)})}>
     <option value="0" disabled>Seleccionar tipo</option>{tipos.map(t=><option value={t.idTipoIncidencia} key={t.idTipoIncidencia}>{t.nombre}</option>)}
    </select></label>
    <label className="field">Usuario que reporta<select required disabled={busy||!!incidencia} value={datos.idUsuarioRegistro} onChange={e=>setDatos({...datos,idUsuarioRegistro:Number(e.target.value)})}>
     <option value="0" disabled>Seleccionar usuario</option>{usuarios.map(u=><option value={u.idUsuario} key={u.idUsuario}>{u.nombres}</option>)}
    </select></label>
   </div>
   {!usuarios.length&&!incidencia&&<p className="api-warning">No hay usuarios activos. Debe existir un usuario en la API antes de registrar una incidencia.</p>}
   <label className="field">Descripción<textarea required minLength={10} maxLength={250} rows={4} disabled={busy} value={datos.descripcion} onChange={e=>setDatos({...datos,descripcion:e.target.value})}/><small>{datos.descripcion.length}/250 caracteres. Mínimo 10.</small></label>
   <label className="field">Ubicación<input required maxLength={60} disabled={busy} value={datos.ubicacion} onChange={e=>setDatos({...datos,ubicacion:e.target.value})} placeholder="Torre, piso o área común"/></label>
   {!incidencia&&<label className="field">Enlace de fotografía <span className="optional">opcional</span><input type="url" maxLength={500} disabled={busy} value={datos.fotoUrl||''} onChange={e=>setDatos({...datos,fotoUrl:e.target.value||null})} placeholder="https://..."/><small>La API recibe enlaces de fotos, no archivos.</small></label>}
   <p className="hint">{incidencia?'Se actualizarán el tipo, la descripción y la ubicación.':'La fecha y hora se toman al abrir el formulario. La incidencia se registrará como Pendiente.'}</p>
   <div className="dialog-actions"><button type="button" className="secondary" disabled={busy} onClick={cerrar}>Cancelar</button><button className="primary" disabled={busy||(!usuarios.length&&!incidencia)||!tipos.length}>{busy?'Guardando…':'Guardar incidencia'}</button></div>
  </form>
 </Modal>;
}
