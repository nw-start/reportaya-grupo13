import {useEffect,useState} from 'react';
import {Pencil,Trash2,ArrowUpRight} from 'lucide-react';
import {api} from './api.mjs';
import {allowedTransitions,safePhoto} from './rules.mjs';
import {Modal,Badge} from './components';
import type {Incidencia,Estado,Cambio} from './types';
interface Props {id:number;cerrar:()=>void;editar:(item:Incidencia)=>void;actualizado:(mensaje:string)=>void}
export default function Detalle({id,cerrar,editar,actualizado}:Props) {
 const [item,setItem]=useState<Incidencia|null>(null),[estado,setEstado]=useState<Estado>('Pendiente'),[observacion,setObservacion]=useState('');
 const [error,setError]=useState(''),[loading,setLoading]=useState(true),[busy,setBusy]=useState(false),[confirmar,setConfirmar]=useState(false);
 async function cargar(){setLoading(true);setError('');try{const result=await api.detalle(id);setItem(result);setEstado(result.estado);setObservacion(result.observacionAtencion||'')}catch(e){setError((e as Error).message)}finally{setLoading(false)}}
 useEffect(()=>{let active=true;api.detalle(id).then(result=>{if(active){setItem(result);setEstado(result.estado);setObservacion(result.observacionAtencion||'')}}).catch(e=>{if(active)setError(e.message)}).finally(()=>{if(active)setLoading(false)});return()=>{active=false}},[id]);
 async function guardar(){
  if(!item)return;
  const fields:Cambio={};
  if(estado!==item.estado)fields.estado=estado;
  if(observacion.trim()!==(item.observacionAtencion||'')){
   if(!observacion.trim()){setError('La API actual no permite quitar una observación dejándola vacía.');return}
   fields.observacionAtencion=observacion.trim();
  }
  if(!Object.keys(fields).length)return;
  setBusy(true);setError('');try{await api.actualizar(id,fields);actualizado('Seguimiento actualizado.')}catch(e){setError((e as Error).message)}finally{setBusy(false)}
 }
 async function eliminar(){setBusy(true);setError('');try{await api.eliminar(id);actualizado('Incidencia eliminada lógicamente.')}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 const photo=item?.fotoUrl?safePhoto(item.fotoUrl):null;
 return <Modal titulo={confirmar?'Eliminar incidencia':item?.codigo||'Detalle de incidencia'} cerrar={cerrar} busy={busy}>
  {error&&<div className="error" role="alert">{error}{!item&&<button onClick={()=>void cargar()}>Reintentar</button>}</div>}
  {loading&&<p className="loading-indicator">Consultando detalle…</p>}
  {item&&!loading&&(confirmar?<><p>¿Eliminar <strong>{item.codigo}</strong>? El registro dejará de aparecer en el listado. Esta API no ofrece una opción para restaurarlo desde el portal.</p><div className="dialog-actions"><button className="secondary" disabled={busy} onClick={()=>setConfirmar(false)}>Cancelar</button><button className="danger" disabled={busy} onClick={()=>void eliminar()}>{busy?'Eliminando…':'Confirmar eliminación'}</button></div></>:<>
   <div className="detail-title"><h3>{item.tipoIncidencia.nombre}</h3><Badge estado={item.estado}/></div>
   <p className="description">{item.descripcion}</p>
   <div className="detail-meta"><div><small>UBICACIÓN</small><strong>{item.ubicacion}</strong></div><div><small>REPORTADO POR</small><strong>{item.usuarioRegistro.nombres}</strong></div><div><small>REGISTRO</small><strong>{item.fechaRegistro.split('-').reverse().join('/')} · {item.horaRegistro}</strong></div><div><small>FECHA DE ATENCIÓN</small><strong>{item.fechaAtencion?item.fechaAtencion.replace('T',' '):'Sin atención registrada'}</strong></div></div>
   {photo&&<a href={photo} target="_blank" rel="noreferrer" className="photo-link">Ver fotografía <ArrowUpRight size={16}/></a>}
   <div className="followup"><h3>Seguimiento de la incidencia</h3><label className="field">Estado<select disabled={busy} value={estado} onChange={e=>setEstado(e.target.value as Estado)}><option value={item.estado}>{item.estado}</option>{allowedTransitions(item.estado).map((s:string)=><option value={s} key={s}>{s}</option>)}</select></label>
   <label className="field">Observación de atención<textarea maxLength={250} rows={3} disabled={busy} value={observacion} onChange={e=>setObservacion(e.target.value)}/><small>{observacion.length}/250 caracteres</small></label>
   <button className="primary" disabled={busy||(estado===item.estado&&observacion===(item.observacionAtencion||''))} onClick={()=>void guardar()}>{busy?'Guardando…':'Guardar seguimiento'}</button></div>
   <div className="dialog-actions split"><button className="text-danger" disabled={busy} onClick={()=>{setError('');setConfirmar(true)}}><Trash2 size={16}/>Eliminar</button><button className="secondary" disabled={busy} onClick={()=>editar(item)}><Pencil size={16}/>Editar datos</button></div>
  </>)}
 </Modal>;
}
