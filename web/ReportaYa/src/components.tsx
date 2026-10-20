import {useEffect,useRef,type ReactNode} from 'react';
import {X} from 'lucide-react';
import {ESTADOS,type Estado} from './types';
export function Badge({estado}:{estado:Estado}) {
 return <span className={'badge s'+ESTADOS.indexOf(estado)}><i/>{estado}</span>;
}
export function Modal({titulo,children,cerrar,busy=false}:{titulo:string;children:ReactNode;cerrar:()=>void;busy?:boolean}) {
 const ref=useRef<HTMLDivElement>(null);
 const close=useRef<HTMLButtonElement>(null);
 const busyRef=useRef(busy);
 const closeRef=useRef(cerrar);
 busyRef.current=busy;
 closeRef.current=cerrar;
 useEffect(()=>{
  const previous=document.activeElement as HTMLElement;
  const overflow=document.body.style.overflow;
  document.body.style.overflow='hidden';
  close.current?.focus();
  function keyboard(e:KeyboardEvent) {
   if(e.key==='Escape'&&!busyRef.current)closeRef.current();
   if(e.key==='Tab') {
    const controls=ref.current?.querySelectorAll<HTMLElement>('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]');
    if(!controls?.length)return;
    const first=controls[0],last=controls[controls.length-1];
    if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus()}
    if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus()}
   }
  }
  document.addEventListener('keydown',keyboard);
  return ()=>{document.removeEventListener('keydown',keyboard);document.body.style.overflow=overflow;previous?.focus()};
 },[]);
 return <div className="overlay" onMouseDown={e=>{if(e.target===e.currentTarget&&!busy)cerrar()}}>
  <div className="dialog" role="dialog" aria-modal="true" aria-labelledby="modal-title" ref={ref}>
   <div className="dialog-top"><h2 id="modal-title">{titulo}</h2><button className="icon" aria-label="Cerrar ventana" ref={close} onClick={cerrar} disabled={busy}><X/></button></div>
   {children}
  </div>
 </div>;
}
