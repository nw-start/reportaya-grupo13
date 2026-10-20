import {useState,type FormEvent} from 'react';
import {Building2,LockKeyhole,Mail,ArrowRight,Eye,EyeOff} from 'lucide-react';
import {api} from './api.mjs';
import {setToken,clearToken,tokenExpiresAt} from './session.mjs';
import type {UsuarioSesion} from './types';
interface Props {ingresar:(usuario:UsuarioSesion,expiracion:number)=>void;mensaje:string}
export default function Login({ingresar,mensaje}:Props) {
 const [correo,setCorreo]=useState(''),[contrasena,setContrasena]=useState('');
 const [error,setError]=useState(''),[busy,setBusy]=useState(false),[visible,setVisible]=useState(false);
 async function enviar(e:FormEvent) {
  e.preventDefault();setError('');setBusy(true);clearToken();
  try {
   const response=await api.login({correo:correo.trim().toLowerCase(),contrasena});
   if(!response.accessToken || response.tokenType?.toLowerCase()!=='bearer')throw new Error('La respuesta de inicio de sesión no es válida.');
   setToken(response.accessToken);
   const usuario:UsuarioSesion=await api.me();
   if(!usuario.activo || usuario.rol!=='Administrador')throw new Error('Este portal es para administradores. Ingresa con una cuenta de administrador.');
   const expires=tokenExpiresAt(response.accessToken);
   if(expires<=Date.now())throw new Error('El token no es válido o ya venció. Inicia sesión nuevamente.');
   setContrasena('');
   ingresar(usuario,expires);
  } catch(e){clearToken();setError((e as Error).message)}
  finally{setBusy(false)}
 }
 return <main className="login-page"><section className="login-info"><a className="brand login-brand" href="#"><span className="brandmark"><Building2 size={27}/></span><span>Reporta<span className="brand-accent">Ya</span><small>PORTAL ADMINISTRATIVO</small></span></a><div><span className="eyebrow">ADMINISTRACIÓN RESIDENCIAL</span><h1>Una comunidad<br/>mejor atendida.</h1><p>Consulta los reportes, organiza su atención y da seguimiento a cada incidencia.</p></div><span className="login-footer">ReportaYa · Grupo 13</span></section>
  <section className="login-card"><span className="login-lock"><LockKeyhole size={26}/></span><h2>Bienvenido a ReportaYa</h2><p>Inicia sesión con tu cuenta de administrador.</p>
   {mensaje&&!error&&<div className="session-message" role="status">{mensaje}</div>}
   {error&&<div className="error" role="alert">{error}</div>}
   <form onSubmit={enviar}>
    <label className="field">Correo electrónico<div className="login-input"><Mail size={18}/><input type="email" required autoComplete="username" maxLength={120} disabled={busy} value={correo} onChange={e=>setCorreo(e.target.value)} placeholder="correo@ejemplo.com"/></div></label>
    <label className="field">Contraseña<div className="login-input"><LockKeyhole size={18}/><input type={visible?'text':'password'} required minLength={8} maxLength={72} autoComplete="current-password" disabled={busy} value={contrasena} onChange={e=>setContrasena(e.target.value)} placeholder="Ingresa tu contraseña"/><button type="button" className="icon" disabled={busy} aria-label={visible?'Ocultar contraseña':'Mostrar contraseña'} onClick={()=>setVisible(!visible)}>{visible?<EyeOff size={18}/>:<Eye size={18}/>}</button></div></label>
    <button className="primary login-submit" disabled={busy}>{busy?'Verificando acceso…':'Iniciar sesión'}{!busy&&<ArrowRight size={17}/>}</button>
   </form><p className="login-hint">Acceso exclusivo para la administración del conjunto residencial.</p>
  </section></main>;
}
