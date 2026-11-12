// Generación de PDF en el navegador (jsPDF). Se carga bajo demanda para no pesar en la carga inicial.
// Los comprobantes son internos y de demostración: no tienen validez tributaria.

const AZUL = [30, 136, 200];
const AZUL_OSC = [11, 60, 107];
const GRIS = [95, 105, 118];

const money = (n) => `S/ ${Number(n || 0).toFixed(2)}`;
const fF = (f) => (f ? new Date(f.length === 10 ? `${f}T00:00:00` : f).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—');
const fFH = (f) => (f ? new Date(f).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—');
const hh = (h) => (h ? h.substring(0, 5) : '');
const METODO = { EFECTIVO: 'Efectivo', TARJETA: 'Tarjeta', YAPE: 'Yape', PLIN: 'Plin' };
const ESTADO_CITA = { RESERVADA: 'Reservada', CONFIRMADA: 'Confirmada', REPROGRAMADA: 'Reprogramada', CANCELADA: 'Cancelada', ASISTIO: 'Asistió', ATENDIDA: 'Atendida', NO_ASISTIO: 'No asistió' };

async function nuevo(formato = 'a4') {
  const { jsPDF } = await import('jspdf');
  const mod = await import('jspdf-autotable');
  const autoTable = typeof mod.default === 'function' ? mod.default : (mod.default?.default || mod.autoTable);
  const doc = new jsPDF({ unit: 'mm', format: formato });
  return { doc, autoTable, W: doc.internal.pageSize.getWidth(), H: doc.internal.pageSize.getHeight() };
}

function cabecera(ctx, titulo, subtitulo) {
  const { doc, W } = ctx;
  doc.setFillColor(...AZUL_OSC);
  doc.rect(0, 0, W, 24, 'F');
  doc.setFillColor(...AZUL);
  doc.rect(0, 24, W, 1.6, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14);
  doc.text('HOSPITAL HERMILIO VALDIZÁN', 12, 11);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.text(W > 170 ? 'Hospital especializado en salud mental · Av. Mendiburu, Ate / Santa Eugenia, Lima (referencial)' : 'Hospital especializado en salud mental', 12, 17);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.text(titulo, W - 12, 11, { align: 'right' });
  if (subtitulo) { doc.setFont('helvetica', 'normal'); doc.setFontSize(9); doc.text(subtitulo, W - 12, 17, { align: 'right' }); }
  doc.setTextColor(40, 44, 52);
  return 34;
}

function pie(ctx, nota) {
  const { doc, W, H } = ctx;
  const n = doc.internal.getNumberOfPages();
  for (let i = 1; i <= n; i++) {
    doc.setPage(i);
    doc.setDrawColor(210, 216, 224);
    doc.line(12, H - 14, W - 12, H - 14);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(...GRIS);
    doc.text(doc.splitTextToSize(nota, W - 24), 12, H - 10.5);
    doc.text(`Generado: ${fFH(new Date().toISOString())} · Página ${i} de ${n}`, W - 12, H - 4.5, { align: 'right' });
  }
}

function seccion(ctx, y, texto) {
  const { doc, W, H } = ctx;
  if (y > H - 40) { doc.addPage(); y = 20; }
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.setTextColor(...AZUL_OSC);
  doc.text(texto.toUpperCase(), 12, y);
  doc.setDrawColor(...AZUL);
  doc.setLineWidth(0.5);
  doc.line(12, y + 1.6, W - 12, y + 1.6);
  doc.setTextColor(40, 44, 52);
  return y + 7;
}

function parrafo(ctx, y, etiqueta, texto) {
  const { doc, W, H } = ctx;
  doc.setFontSize(9.5);
  const lineas = doc.splitTextToSize(texto || '—', W - 24);
  if (y + 5 + lineas.length * 4.6 > H - 18) { doc.addPage(); y = 20; }
  doc.setFont('helvetica', 'bold');
  doc.text(etiqueta, 12, y);
  doc.setFont('helvetica', 'normal');
  doc.text(lineas, 12, y + 4.8);
  return y + 5 + lineas.length * 4.6 + 2;
}

function marcaAgua(ctx, texto, color = [200, 30, 30]) {
  const { doc, W, H } = ctx;
  doc.saveGraphicsState();
  doc.setGState(new doc.GState({ opacity: 0.16 }));
  doc.setTextColor(...color);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(54);
  doc.text(texto, W / 2, H / 2, { align: 'center', angle: 32 });
  doc.restoreGraphicsState();
  doc.setTextColor(40, 44, 52);
}

const tablaDatos = (ctx, y, filas) => {
  ctx.autoTable(ctx.doc, {
    startY: y, body: filas, theme: 'plain', margin: { left: 12, right: 12 },
    styles: { fontSize: 9.5, cellPadding: 1.4 },
    columnStyles: { 0: { fontStyle: 'bold', cellWidth: 42, textColor: AZUL_OSC } },
  });
  return ctx.doc.lastAutoTable.finalY + 5;
};

const th = (t, a = 'left') => ({ content: t, styles: { halign: a } });
const guardar = (doc, nombre) => doc.save(nombre);
const NOTA_CONFIDENCIAL = 'Documento confidencial. Datos personales protegidos por la Ley N.° 29733. Uso exclusivo del paciente, su tutor/apoderado y personal autorizado.';
const NOTA_INTERNA = 'Comprobante interno de demostración, sin validez tributaria. No reemplaza una boleta electrónica SUNAT.';

// ------------------------------------------------------------------ Boleta de farmacia
export async function boletaFarmacia(d, { devolverDoc = false } = {}) {
  const ctx = await nuevo('a5');
  const { doc, W, autoTable } = ctx;
  const num = d.pago?.comprobante || `DISP-${String(d.idDispensacion).padStart(6, '0')}`;
  let y = cabecera(ctx, 'BOLETA DE VENTA', `N.° ${num}`);
  doc.setFontSize(9);
  y = tablaDatos(ctx, y, [
    ['Fecha', fFH(d.fecha)], ['Paciente', `${d.paciente} (Doc. ${d.pacienteDocumento})`],
    ['Receta', `N.° ${d.recetaId} · Dr(a). ${d.medico}`], ['Atendido por', d.farmaceutico],
  ]);
  autoTable(doc, {
    startY: y, margin: { left: 12, right: 12 }, theme: 'striped',
    head: [[th('Medicamento'), th('Cant.', 'center'), th('P. unit.', 'right'), th('Subtotal', 'right')]],
    body: d.lineas.map((l) => [`${l.medicamento}${l.presentacion ? `\n${l.presentacion}` : ''}`, l.cantidad, money(l.precioUnitario), money(l.subtotal)]),
    headStyles: { fillColor: AZUL_OSC, fontSize: 9 }, styles: { fontSize: 9 },
    columnStyles: { 1: { halign: 'center' }, 2: { halign: 'right' }, 3: { halign: 'right' } },
  });
  y = doc.lastAutoTable.finalY + 6;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(13);
  doc.setTextColor(...AZUL_OSC);
  doc.text(`TOTAL: ${money(d.total)}`, W - 12, y, { align: 'right' });
  doc.setTextColor(40, 44, 52);
  y += 8;
  if (d.pago) {
    const p = d.pago;
    const filas = [['Método de pago', METODO[p.metodo] || p.metodo], ['Referencia', p.referencia || '—']];
    if (p.tarjetaUltimos4) filas.push(['Tarjeta', `**** **** **** ${p.tarjetaUltimos4}`]);
    if (p.montoRecibido != null) filas.push(['Recibido / vuelto', `${money(p.montoRecibido)} / ${money(p.vuelto)}`]);
    filas.push(['Estado', p.estado === 'PAGADO' ? 'PAGADO' : 'ANULADO']);
    y = tablaDatos(ctx, y, filas);
  } else {
    doc.setFont('helvetica', 'italic');
    doc.setFontSize(9);
    doc.text('Medicamentos entregados sin costo.', 12, y);
  }
  if (d.anulada) marcaAgua(ctx, 'ANULADA');
  pie(ctx, NOTA_INTERNA);
  if (devolverDoc) return doc;
  guardar(doc, `boleta-farmacia-${num}.pdf`);
  return null;
}

// ------------------------------------------------------------------ Boleta / comprobante de consulta
export async function boletaConsulta(pago) {
  const ctx = await nuevo('a5');
  const { doc, W, autoTable } = ctx;
  let y = cabecera(ctx, 'BOLETA DE VENTA', `N.° ${pago.comprobante}`);
  y = tablaDatos(ctx, y, [['Fecha', fFH(pago.fecha)], ['Paciente', `${pago.paciente} (Doc. ${pago.pacienteDocumento})`], ['Registrado por', pago.pagador || '—']]);
  autoTable(doc, {
    startY: y, margin: { left: 12, right: 12 }, theme: 'striped',
    head: [[th('Concepto'), th('Importe', 'right')]], body: [[pago.concepto, money(pago.monto)]],
    headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9.5 }, columnStyles: { 1: { halign: 'right' } },
  });
  y = doc.lastAutoTable.finalY + 6;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(13);
  doc.setTextColor(...AZUL_OSC);
  doc.text(`TOTAL: ${money(pago.monto)}`, W - 12, y, { align: 'right' });
  doc.setTextColor(40, 44, 52);
  y += 8;
  const filas = [['Método de pago', METODO[pago.metodo] || pago.metodo], ['Referencia', pago.referencia || '—']];
  if (pago.tarjetaUltimos4) filas.push(['Tarjeta', `**** **** **** ${pago.tarjetaUltimos4}`]);
  if (pago.montoRecibido != null) filas.push(['Recibido / vuelto', `${money(pago.montoRecibido)} / ${money(pago.vuelto)}`]);
  filas.push(['Estado', pago.estado === 'PAGADO' ? 'PAGADO' : `ANULADO${pago.motivoAnulacion ? ` (${pago.motivoAnulacion})` : ''}`]);
  tablaDatos(ctx, y, filas);
  if (pago.estado === 'ANULADO') marcaAgua(ctx, 'ANULADO');
  pie(ctx, NOTA_INTERNA);
  guardar(doc, `boleta-consulta-${pago.comprobante}.pdf`);
}

// ------------------------------------------------------------------ Reporte de la cita médica
export async function reporteCita(cita, h) {
  const ctx = await nuevo('a4');
  const { doc, autoTable } = ctx;
  let y = cabecera(ctx, 'REPORTE DE CITA MÉDICA', `Cita N.° ${cita.idCita}`);

  y = seccion(ctx, y, 'Datos de la cita');
  y = tablaDatos(ctx, y, [
    ['Estado', ESTADO_CITA[cita.estado] || cita.estado], ['Fecha y hora', `${fF(cita.fecha)} · ${hh(cita.horaInicio)} a ${hh(cita.horaFin)}`],
    ['Especialidad', cita.especialidad], ['Médico', cita.medico], ['Ubicación', cita.ubicacion || `Pabellón ${cita.pabellon}, Room ${cita.room}`],
    ['Registrada', `${fFH(cita.fechaHoraReserva)}${cita.reservadaPor ? ` por ${cita.reservadaPor}` : ''}`],
    ['Pago de la consulta', Number(cita.precio) > 0 ? `${money(cita.precio)} — ${cita.pagada ? 'PAGADA' : 'PENDIENTE'}` : 'Sin costo'],
  ]);

  y = seccion(ctx, y, 'Paciente');
  y = tablaDatos(ctx, y, [['Nombre', cita.paciente], ['Documento', cita.pacienteDocumento]]);

  if (h) {
    if (h.triaje) {
      y = seccion(ctx, y, 'Triaje');
      y = tablaDatos(ctx, y, [['Presión arterial', h.triaje.presionArterial || '—'], ['Peso', h.triaje.peso != null ? `${h.triaje.peso} kg` : '—'], ['Otros signos', h.triaje.signosVitales || '—']]);
    }
    y = seccion(ctx, y, 'Atención médica');
    y = parrafo(ctx, y, 'Diagnóstico', h.diagnostico);
    y = parrafo(ctx, y, 'Tratamiento / indicaciones', h.tratamiento);
    if (h.observaciones) y = parrafo(ctx, y, 'Observaciones', h.observaciones);
    if (h.receta?.detalles?.length) {
      y = seccion(ctx, y, `Receta médica (${h.receta.estado})`);
      if (h.receta.indicaciones) y = parrafo(ctx, y, 'Indicaciones generales', h.receta.indicaciones);
      autoTable(doc, {
        startY: y, margin: { left: 12, right: 12 }, theme: 'striped',
        head: [['Medicamento', 'Presentación', 'Dosis', 'Cant.', 'Entregado']],
        body: h.receta.detalles.map((d) => [d.medicamento, d.presentacion || '—', d.dosis, d.cantidad, d.cantidadDispensada]),
        headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9 },
      });
      y = doc.lastAutoTable.finalY + 6;
    }
    y = parrafo(ctx, y, 'Registrado por', `Dr(a). ${h.medico} el ${fFH(h.fechaRegistro)}`);
  } else if (cita.estado === 'ATENDIDA') {
    y = seccion(ctx, y, 'Atención médica');
    parrafo(ctx, y, 'Nota', 'El detalle clínico no está disponible para su perfil.');
  }
  if (cita.estado === 'CANCELADA') marcaAgua(ctx, 'CANCELADA', [120, 120, 120]);
  pie(ctx, NOTA_CONFIDENCIAL);
  guardar(doc, `reporte-cita-${cita.idCita}.pdf`);
}

// ------------------------------------------------------------------ Receta médica
export async function recetaPdf(h, pacienteDoc) {
  const ctx = await nuevo('a5');
  const { doc, autoTable } = ctx;
  let y = cabecera(ctx, 'RECETA MÉDICA', `Receta N.° ${h.receta.idReceta}`);
  y = tablaDatos(ctx, y, [
    ['Paciente', h.paciente + (pacienteDoc ? ` (Doc. ${pacienteDoc})` : '')], ['Fecha', fFH(h.receta.fecha)],
    ['Médico', `Dr(a). ${h.medico} — ${h.especialidad}`], ['Diagnóstico', h.diagnostico],
  ]);
  autoTable(doc, {
    startY: y, margin: { left: 12, right: 12 }, theme: 'striped',
    head: [[th('Medicamento'), th('Dosis'), th('Cant.', 'center')]],
    body: h.receta.detalles.map((d) => [`${d.medicamento}${d.presentacion ? `\n${d.presentacion}` : ''}`, d.dosis, d.cantidad]),
    headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9 }, columnStyles: { 2: { halign: 'center' } },
  });
  y = doc.lastAutoTable.finalY + 6;
  if (h.receta.indicaciones) y = parrafo(ctx, y, 'Indicaciones', h.receta.indicaciones);
  doc.setFont('helvetica', 'italic');
  doc.setFontSize(8.5);
  doc.text('Presente esta receta en la farmacia del hospital para el recojo de sus medicamentos.', 12, y + 2);
  pie(ctx, NOTA_CONFIDENCIAL);
  guardar(doc, `receta-${h.receta.idReceta}.pdf`);
}

// ------------------------------------------------------------------ Reporte administrativo
export async function reporteAdmin({ desde, hasta, rep, ina, ing }) {
  const ctx = await nuevo('a4');
  const { doc, autoTable } = ctx;
  let y = cabecera(ctx, 'REPORTE DE GESTIÓN', `${fF(desde)} al ${fF(hasta)}`);
  if (rep) {
    y = seccion(ctx, y, `Citas por estado (${rep.total} en total)`);
    autoTable(doc, {
      startY: y, margin: { left: 12, right: 12 }, theme: 'striped', head: [[th('Estado'), th('Cantidad', 'right')]],
      body: Object.entries(rep.porEstado).map(([k, v]) => [ESTADO_CITA[k] || k, v]), headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9.5 },
      columnStyles: { 1: { halign: 'right' } },
    });
    y = doc.lastAutoTable.finalY + 8;
  }
  if (ina?.length) {
    y = seccion(ctx, y, 'Inasistencia por especialidad');
    autoTable(doc, {
      startY: y, margin: { left: 12, right: 12 }, theme: 'striped', head: [[th('Especialidad'), th('Citas', 'right'), th('No asistió', 'right'), th('%', 'right')]],
      body: ina.map((i) => [i.especialidad, i.total, i.noAsistio, `${i.porcentaje}%`]), headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9.5 },
      columnStyles: { 1: { halign: 'right' }, 2: { halign: 'right' }, 3: { halign: 'right' } },
    });
    y = doc.lastAutoTable.finalY + 8;
  }
  if (ing) {
    y = seccion(ctx, y, `Ingresos (${money(ing.total)} · ${ing.cantidadPagos} pagos · ${ing.anulados} anulados)`);
    autoTable(doc, {
      startY: y, margin: { left: 12, right: 12 }, theme: 'striped', head: [[th('Concepto'), th('Monto', 'right')]],
      body: [...Object.entries(ing.porTipo).map(([k, v]) => [`Por tipo: ${k === 'CONSULTA' ? 'Consultas' : 'Farmacia'}`, money(v)]),
        ...Object.entries(ing.porMetodo).map(([k, v]) => [`Por método: ${METODO[k]}`, money(v)])],
      headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9.5 }, columnStyles: { 1: { halign: 'right' } },
    });
    y = doc.lastAutoTable.finalY + 8;
    if (ing.porDia?.length) {
      y = seccion(ctx, y, 'Ingresos por día');
      autoTable(doc, {
        startY: y, margin: { left: 12, right: 12 }, theme: 'striped', head: [[th('Fecha'), th('Monto', 'right')]],
        body: ing.porDia.map((d) => [fF(d.fecha), money(d.monto)]), headStyles: { fillColor: AZUL_OSC }, styles: { fontSize: 9.5 }, columnStyles: { 1: { halign: 'right' } },
      });
    }
  }
  pie(ctx, 'Reporte administrativo de uso interno. Información confidencial.');
  guardar(doc, `reporte-gestion-${desde}_${hasta}.pdf`);
}

// ------------------------------------------------------------------ CSV
export function descargarCSV(nombre, cabeceras, filas) {
  const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
  const texto = [cabeceras, ...filas].map((f) => f.map(esc).join(',')).join('\r\n');
  const blob = new Blob(['﻿', texto], { type: 'text/csv;charset=utf-8;' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = nombre;
  a.click();
  URL.revokeObjectURL(a.href);
}
