import { useEffect, useState } from 'react';
import { Alert, Button, Modal } from 'react-bootstrap';
import { msg } from '../api';
import { Campo } from './ui';

export const METODOS = {
  TARJETA: { texto: 'Tarjeta', icono: 'credit-card' },
  YAPE: { texto: 'Yape', icono: 'phone' },
  PLIN: { texto: 'Plin', icono: 'phone-vibrate' },
  EFECTIVO: { texto: 'Efectivo', icono: 'cash-coin' },
};
export const money = (n) => `S/ ${Number(n || 0).toFixed(2)}`;

const luhn = (n) => {
  let s = 0; let d = false;
  for (let i = n.length - 1; i >= 0; i--) { let x = Number(n[i]); if (d) { x *= 2; if (x > 9) x -= 9; } s += x; d = !d; }
  return s % 10 === 0;
};

const VACIO = { numeroTarjeta: '', titular: '', vencimiento: '', cvv: '', celular: '', montoRecibido: '' };

/**
 * Pasarela de pago (simulada). Los datos de tarjeta se envían al servidor solo para validarlos y se descartan:
 * el sistema guarda únicamente los últimos 4 dígitos. `permitirEfectivo` solo para ventanilla/farmacia.
 */
export default function PagoModal({ show, onHide, monto, concepto, permitirEfectivo = false, onPagar, titulo = 'Pagar' }) {
  const [metodo, setMetodo] = useState('TARJETA');
  const [f, setF] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => { if (show) { setMetodo('TARJETA'); setF(VACIO); setErrores({}); setError(''); } }, [show]);

  const set = (k, v) => setF((x) => ({ ...x, [k]: v }));

  const validar = () => {
    const e = {};
    if (metodo === 'TARJETA') {
      const n = f.numeroTarjeta.replace(/\s/g, '');
      if (!/^\d{13,19}$/.test(n) || !luhn(n)) e.numeroTarjeta = 'El número de tarjeta no es válido';
      if (f.titular.trim().length < 3) e.titular = 'Ingrese el nombre del titular';
      const m = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(f.vencimiento);
      if (!m) e.vencimiento = 'Use el formato MM/AA';
      else { const hoy = new Date(); const v = 2000 + Number(m[2]); if (v < hoy.getFullYear() || (v === hoy.getFullYear() && Number(m[1]) < hoy.getMonth() + 1)) e.vencimiento = 'La tarjeta está vencida'; }
      if (!/^\d{3,4}$/.test(f.cvv)) e.cvv = 'CVV de 3 o 4 dígitos';
    } else if (metodo === 'YAPE' || metodo === 'PLIN') {
      if (!/^9\d{8}$/.test(f.celular.replace(/\s/g, ''))) e.celular = 'Ingrese un celular de 9 dígitos que empiece con 9';
    } else if (f.montoRecibido !== '' && Number(f.montoRecibido) < Number(monto)) {
      e.montoRecibido = 'El monto recibido es menor al total';
    }
    setErrores(e);
    return Object.keys(e).length === 0;
  };

  const pagar = async () => {
    if (!validar()) return;
    setEnviando(true); setError('');
    const body = { metodo };
    if (metodo === 'TARJETA') Object.assign(body, { numeroTarjeta: f.numeroTarjeta.replace(/\s/g, ''), titular: f.titular, vencimiento: f.vencimiento, cvv: f.cvv });
    if (metodo === 'YAPE' || metodo === 'PLIN') body.celular = f.celular.replace(/\s/g, '');
    if (metodo === 'EFECTIVO' && f.montoRecibido !== '') body.montoRecibido = Number(f.montoRecibido);
    try {
      await onPagar(body);
      onHide(true);
    } catch (ex) {
      setError(msg(ex));
    } finally { setEnviando(false); }
  };

  const metodos = permitirEfectivo ? ['TARJETA', 'YAPE', 'PLIN', 'EFECTIVO'] : ['TARJETA', 'YAPE', 'PLIN'];
  const vuelto = metodo === 'EFECTIVO' && f.montoRecibido !== '' ? Number(f.montoRecibido) - Number(monto) : null;

  return (
    <Modal show={show} onHide={() => onHide(false)} centered backdrop="static">
      <Modal.Header closeButton><Modal.Title>{titulo}</Modal.Title></Modal.Header>
      <Modal.Body>
        <div className="text-center mb-3">
          <div className="text-muted small">{concepto}</div>
          <div className="display-6 fw-bold" style={{ color: 'var(--azul-osc)' }}>{money(monto)}</div>
        </div>
        <div className="d-flex gap-2 mb-3" role="radiogroup" aria-label="Método de pago">
          {metodos.map((m) => (
            <button type="button" key={m} role="radio" aria-checked={metodo === m} className={`btn flex-fill ${metodo === m ? 'btn-azul' : 'btn-outline-azul'}`} onClick={() => { setMetodo(m); setErrores({}); }}>
              <i className={`bi bi-${METODOS[m].icono} d-block fs-4`} />{METODOS[m].texto}
            </button>
          ))}
        </div>
        {error && <Alert variant="danger" role="alert">{error}</Alert>}

        {metodo === 'TARJETA' && (
          <>
            <Campo id="pg-num" etiqueta="Número de tarjeta" error={errores.numeroTarjeta}>
              <input id="pg-num" className="form-control" inputMode="numeric" autoComplete="cc-number" placeholder="0000 0000 0000 0000" maxLength={23}
                value={f.numeroTarjeta} onChange={(e) => set('numeroTarjeta', e.target.value.replace(/[^\d]/g, '').replace(/(.{4})/g, '$1 ').trim())} />
            </Campo>
            <Campo id="pg-tit" etiqueta="Titular" error={errores.titular}><input id="pg-tit" className="form-control" autoComplete="cc-name" value={f.titular} onChange={(e) => set('titular', e.target.value)} /></Campo>
            <div className="row">
              <div className="col-6"><Campo id="pg-ven" etiqueta="Vencimiento (MM/AA)" error={errores.vencimiento}>
                <input id="pg-ven" className="form-control" inputMode="numeric" autoComplete="cc-exp" placeholder="MM/AA" maxLength={5} value={f.vencimiento}
                  onChange={(e) => { let v = e.target.value.replace(/[^\d]/g, '').slice(0, 4); if (v.length > 2) v = `${v.slice(0, 2)}/${v.slice(2)}`; set('vencimiento', v); }} /></Campo></div>
              <div className="col-6"><Campo id="pg-cvv" etiqueta="CVV" error={errores.cvv}>
                <input id="pg-cvv" className="form-control" type="password" inputMode="numeric" autoComplete="cc-csc" maxLength={4} value={f.cvv} onChange={(e) => set('cvv', e.target.value.replace(/[^\d]/g, ''))} /></Campo></div>
            </div>
          </>
        )}

        {(metodo === 'YAPE' || metodo === 'PLIN') && (
          <Campo id="pg-cel" etiqueta={`Celular asociado a ${METODOS[metodo].texto}`} error={errores.celular} ayuda="Se simula la aprobación del pago desde su aplicación.">
            <input id="pg-cel" className="form-control" inputMode="numeric" maxLength={9} placeholder="9XXXXXXXX" value={f.celular} onChange={(e) => set('celular', e.target.value.replace(/[^\d]/g, ''))} />
          </Campo>
        )}

        {metodo === 'EFECTIVO' && (
          <Campo id="pg-rec" etiqueta="Monto recibido (S/)" error={errores.montoRecibido} ayuda={vuelto != null && vuelto >= 0 ? `Vuelto: ${money(vuelto)}` : 'Déjelo vacío si recibió el monto exacto.'}>
            <input id="pg-rec" type="number" min="0" step="0.10" className="form-control" value={f.montoRecibido} onChange={(e) => set('montoRecibido', e.target.value)} />
          </Campo>
        )}

        <Alert variant="light" className="border small mb-0">
          <i className="bi bi-shield-lock me-1" />Pasarela de <strong>demostración</strong>: no se realiza ningún cargo real y no se guardan los datos de su tarjeta (solo los últimos 4 dígitos).
          Pruebe con <code>4111 1111 1111 1111</code> (aprobada) o <code>4000 0000 0000 0002</code> (rechazada).
        </Alert>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={() => onHide(false)} disabled={enviando}>Cancelar</Button>
        <Button className="btn-azul" onClick={pagar} disabled={enviando}>{enviando ? 'Procesando…' : `Pagar ${money(monto)}`}</Button>
      </Modal.Footer>
    </Modal>
  );
}
