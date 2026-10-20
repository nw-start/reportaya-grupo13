import {useEffect,useRef,useState} from 'react';
import {createRoot} from 'react-dom/client';
import {Building2,ClipboardList,LayoutDashboard,Search,RefreshCw,Plus,ArrowUpRight,MapPin,Check,X,Menu,ChevronRight,LogOut} from 'lucide-react';
import {api} from './api.mjs';
import {ESTADOS,type Incidencia,type Tipo,type Usuario,type UsuarioSesion} from './types';
import {Badge} from './components';
import Formulario from './Formulario';
import Detalle from './Detalle';
import './styles.css';
import Login from './Login';
import {clearToken} from './session.mjs';
function Panel({usuario,salir}:{usuario:UsuarioSesion;salir:()=>void}){
 const [pagina,setPagina]=useState('resumen');
 const [items,setItems]=useState<Incidencia[]>([]),[tipos,setTipos]=useState<Tipo[]>([]),[usuarios,setUsuarios]=useState<Usuario[]>([]);
 const [busqueda,setBusqueda]=useState(''),[criterio,setCriterio]=useState(''),[estado,setEstado]=useState('Todos');
 const [loading,setLoading]=useState(true),[error,setError]=useState(''),[catalogError,setCatalogError]=useState(''),[aviso,setAviso]=useState('');
 const [detalle,setDetalle]=useState<number|null>(null),[formulario,setFormulario]=useState(false),[editando,setEditando]=useState<Incidencia|null>(null),[menu,setMenu]=useState(false);
 const requestNumber=useRef(0);
 const mounted=useRef(true);
 useEffect(()=>{mounted.current=true;return()=>{mounted.current=false;requestNumber.current++}},[]);
 async function cargar(query=''){
  const n=++requestNumber.current;
  setLoading(true);setError('');
  try{
   const result:Incidencia[]=query?await api.buscar(query):await api.listar();
   if(n===requestNumber.current&&mounted.current){setItems(result);setCriterio(query)}
  }catch(e){if(n===requestNumber.current&&mounted.current){setItems([]);setError((e as Error).message)}}
  finally{if(n===requestNumber.current&&mounted.current)setLoading(false)}
 }
 async function catalogos(){
  setCatalogError('');
  try{const [t,u]=await Promise.all([api.tipos(),api.usuarios()]);if(mounted.current){setTipos(t);setUsuarios(u)}}
  catch(e){if(mounted.current)setCatalogError((e as Error).message)}
 }
 useEffect(()=>{void cargar();void catalogos()},[]);
 useEffect(()=>{if(!aviso)return;const timer=setTimeout(()=>setAviso(''),5000);return()=>clearTimeout(timer)},[aviso]);
 const visibles=items.filter(i=>!i.eliminado&&(estado==='Todos'||i.estado===estado));
 function navegar(destino:string){setPagina(destino);setMenu(false);setEstado('Todos');setBusqueda('');void cargar()}
 function nuevo(){setEditando(null);setFormulario(true);if(!tipos.length||!usuarios.length)void catalogos()}
 function guardado(mensaje:string){setFormulario(false);setDetalle(null);setAviso(mensaje);void cargar(criterio)}
 const title=pagina==='resumen'?'Resumen de incidencias':'Gestión de incidencias';
 return <div className="shell"><aside className={menu?'sidebar open':'sidebar'}>
  <a className="brand" href="#" onClick={e=>{e.preventDefault();navegar('resumen')}}><span className="brandmark"><Building2 size={25}/></span><span>Reporta<span className="brand-accent">Ya</span><small>PORTAL ADMINISTRATIVO</small></span></a>
  <span className="nav-label">CONJUNTO RESIDENCIAL</span>
  <nav><button className={'nav '+(pagina==='resumen'?'active':'')} onClick={()=>navegar('resumen')}><LayoutDashboard size={19}/>Resumen</button><button className={'nav '+(pagina==='incidencias'?'active':'')} onClick={()=>navegar('incidencias')}><ClipboardList size={19}/>Incidencias</button></nav>
  <div className="side-help"><Building2 size={22}/><strong>Gestión de la comunidad</strong><p>Registra los reportes y consulta su atención.</p></div>
  <div className="profile"><span className="avatar">RY</span><div><strong>{usuario.nombres}</strong><small>Administrador</small></div></div>
 </aside><div className="workspace"><header className="topbar"><div className="breadcrumb"><button className="icon mobile-only" aria-label="Abrir menú" onClick={()=>setMenu(!menu)}><Menu size={20}/></button>ReportaYa <ChevronRight size={14}/><strong>{pagina==='resumen'?'Resumen':'Incidencias'}</strong></div><span className="mode"><i/>{loading?'Consultando API':error?'Sin conexión confirmada':'Datos del servidor'}</span><button className="secondary logout-button" onClick={salir}><LogOut size={16}/>Cerrar sesión</button></header>
 <main>
 <div className="page-heading"><div><span className="eyebrow">ADMINISTRACIÓN RESIDENCIAL</span><h1>{title}</h1><p>Registro y seguimiento de los reportes de la comunidad.</p></div><button className="primary" onClick={nuevo} disabled={loading||!!error}><Plus size={18}/>Nueva incidencia</button></div>
 {error&&<div className="error" role="alert">{error}<button onClick={()=>void cargar(criterio)}>Reintentar</button></div>}
 {catalogError&&<div className="error" role="alert">No se cargaron los catálogos: {catalogError}<button onClick={()=>void catalogos()}>Reintentar catálogos</button></div>}
 <section className="stats" aria-label="Indicadores de incidencias">{ESTADOS.map((s,i)=><button className={'stat stat'+i} key={s} onClick={()=>{setEstado(s);setPagina('incidencias')}}><div><span className="stat-label">{['Pendientes','En proceso','Atendidas','Rechazadas'][i]}</span><span className="stat-symbol">{i===2?<Check size={19}/>:<ClipboardList size={19}/>}</span></div><strong>{loading?'—':items.filter(a=>!a.eliminado&&a.estado===s).length}</strong><small>{['Por revisar','En seguimiento','Casos resueltos','Casos descartados'][i]}<ArrowUpRight size={14}/></small></button>)}</section>
 {criterio&&<p className="dashboard-summary">Resultados de búsqueda: <strong>{criterio}</strong>. Los contadores corresponden a estos resultados.</p>}
 <section className="panel"><div className="panel-heading"><div><h2>Incidencias registradas <span className="count">{visibles.length}</span></h2><p>Selecciona un reporte para revisar su detalle y seguimiento.</p></div></div>
 <div className="tools"><form className="search" onSubmit={e=>{e.preventDefault();void cargar(busqueda.trim())}}><Search size={18}/><input aria-label="Buscar incidencias" maxLength={250} placeholder="Código, tipo, ubicación o descripción" value={busqueda} onChange={e=>setBusqueda(e.target.value)}/><button disabled={loading}>Buscar</button></form><button className="secondary" disabled={loading} onClick={()=>{setBusqueda('');setEstado('Todos');void cargar();void catalogos()}}><RefreshCw size={16} className={loading?'spin':''}/>Actualizar</button></div>
 <div className="tabs">{['Todos',...ESTADOS].map(s=><button key={s} className={estado===s?'selected':''} onClick={()=>setEstado(s)}>{s}</button>)}</div>
 <div className="table-wrap"><table><thead><tr><th>INCIDENCIA</th><th>UBICACIÓN</th><th>ESTADO</th><th>REGISTRO</th><th aria-label="Acciones"/></tr></thead><tbody>{!loading&&visibles.map(i=><tr key={i.idIncidencia}><td><button className="incident-title" onClick={()=>setDetalle(i.idIncidencia)}><span className="incident-icon"><ClipboardList size={20}/></span><span><strong>{i.tipoIncidencia.nombre}</strong><small>{i.codigo} · {i.descripcion}</small></span></button></td><td><span className="location"><MapPin size={15}/>{i.ubicacion}</span></td><td><Badge estado={i.estado}/></td><td><span className="date">{i.fechaRegistro.split('-').reverse().join('/')}<small>{i.usuarioRegistro.nombres}</small></span></td><td><button className="icon" aria-label={'Ver '+i.codigo} onClick={()=>setDetalle(i.idIncidencia)}><ArrowUpRight size={18}/></button></td></tr>)}</tbody></table>
 {loading&&<div className="empty"><RefreshCw className="spin" size={24}/><strong>Consultando la API…</strong><p>Si el servicio está iniciando puede tardar unos segundos.</p></div>}
 {!loading&&!visibles.length&&<div className="empty"><ClipboardList size={30}/><strong>{error?'No fue posible cargar los datos':'No hay incidencias para mostrar'}</strong><p>{error?'Revisa la conexión y vuelve a intentarlo.':'Prueba otra búsqueda o registra una incidencia.'}</p></div>}
 </div><div className="table-footer"><span>{visibles.length} {visibles.length===1?'incidencia':'incidencias'}</span><span>Fuente: API ReportaYa</span></div></section>
 <div className="bottom-note"><Building2 size={16}/><span>ReportaYa · Grupo 13</span><span>Desarrollo de Aplicaciones Móviles I</span></div>
 </main></div>
 {formulario&&<Formulario incidencia={editando} tipos={tipos} usuarios={usuarios} cerrar={()=>setFormulario(false)} guardado={()=>guardado('Incidencia guardada en el servidor.')}/>}
 {detalle!==null&&!formulario&&<Detalle id={detalle} cerrar={()=>setDetalle(null)} editar={item=>{setEditando(item);setFormulario(true)}} actualizado={guardado}/>}
 {aviso&&<div className="toast" role="status"><Check size={18}/>{aviso}<button className="icon" aria-label="Cerrar mensaje" onClick={()=>setAviso('')}><X size={16}/></button></div>}
 </div>;
}
function App(){
 const [usuario,setUsuario]=useState<UsuarioSesion|null>(null);
 const [expiracion,setExpiracion]=useState(0);
 const [mensaje,setMensaje]=useState('');
 function salir(){clearToken();setUsuario(null);setExpiracion(0);setMensaje('Has cerrado sesión.');}
 useEffect(()=>{
  function vencida(){clearToken();setUsuario(null);setExpiracion(0);setMensaje('Tu sesión venció. Inicia sesión nuevamente.');}
  window.addEventListener('session-expired',vencida);
  const timer=expiracion?setTimeout(vencida,Math.max(0,expiracion-Date.now())):undefined;
  return()=>{window.removeEventListener('session-expired',vencida);if(timer)clearTimeout(timer)};
 },[expiracion]);
 if(!usuario)return <Login mensaje={mensaje} ingresar={(user,expires)=>{setUsuario(user);setExpiracion(expires);setMensaje('')}}/>;
 return <Panel usuario={usuario} salir={salir}/>;
}
createRoot(document.getElementById('root')!).render(<App/>);
