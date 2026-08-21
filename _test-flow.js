const http = require('http');
function doRequest(method, path, token, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const opts = {
      hostname: 'localhost', port: 8080, path: '/api/v1' + path,
      method: method, headers: { 'Content-Type': 'application/json' }
    };
    if (data) opts.headers['Content-Length'] = Buffer.byteLength(data);
    if (token) opts.headers['Authorization'] = 'Bearer ' + token;
    const req = http.request(opts, (res) => {
      let chunks = '';
      res.on('data', (c) => chunks += c);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(chunks) }); }
        catch { resolve({ status: res.statusCode, raw: chunks }); }
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}
(async () => {
  // 1) Login back1
  let r = await doRequest('POST', '/auth/login', null, { username: 'back1', password: 'Back*123' });
  console.log('LOGIN BACK1 status:', r.status, 'rol:', r.body.rol);
  const backTk = r.body.token;
  // 2) Ver listado de pendientes para saber IDs actuales
  r = await doRequest('GET', '/ventas/pendientes?size=20', backTk);
  console.log('\nPENDIENTES (actual) status:', r.status, 'total:', r.body.totalElements);
  const ids = r.body.content.map(v => 'ID=' + v.id + '|ESTADO=' + v.estado + '|DNI=' + v.dniCliente);
  ids.forEach(i => console.log('  ', i));
  // 3) Intentar rechazar el primer PENDIENTE
  const target = r.body.content.find(v => v.estado === 'PENDIENTE');
  if (target) {
    console.log('\nRECHAZANDO ID=' + target.id + '...');
    r = await doRequest('POST', '/ventas/' + target.id + '/rechazar', backTk,
      { motivoRechazo: 'No cumple requisitos mínimos; el DNI no se pudo validar.' });
    console.log('Resultado rechazo:', r.status,
      '\n  id:', r.body.id, 'estado:', r.body.estado,
      '\n  motivo:', r.body.motivoRechazo,
      '\n  fechaValidacion:', r.body.fechaValidacion);
    if (r.status !== 200) console.log('  RAW:', JSON.stringify(r.body).slice(0, 400));
  }
  // 4) Agente crea venta nueva → probar rechazo también
  let ra = await doRequest('POST', '/auth/login', null, { username: 'agente1', password: 'Agente*123' });
  const agTk = ra.body.token;
  const cod = 'LLAM-NODE-' + Math.floor(Math.random() * 90000 + 10000);
  const vbody = { dniCliente:'11223344', nombreCliente:'Node Test', telefonoCliente:'987654321',
    direccionCliente:'Calle X 123', planActual:'NINGUNO', planNuevo:'FULL_300',
    codigoLlamada: cod, producto:'FIJA_HOGAR', monto: 349.50 };
  r = await doRequest('POST', '/ventas', agTk, vbody);
  console.log('\nNUEVA VENTA (agente1): status', r.status, 'id=', r.body.id, 'estado=', r.body.estado, 'codigo=', r.body.codigoLlamada);
  const nuevaId = r.body.id;
  // 5) Backoffice rechaza la venta nueva
  r = await doRequest('POST', '/ventas/' + nuevaId + '/rechazar', backTk, { motivoRechazo: 'Teléfono no pertenece a cliente (validación cruzada falló).' });
  console.log('RECHAZO de nueva venta id=' + nuevaId + ': status', r.status, 'estado=', r.body.estado, 'motivo=', r.body.motivoRechazo);
  if (r.status !== 200) console.log('  RAW:', JSON.stringify(r.body));
  // 6) Validación: venta con código duplicado
  r = await doRequest('POST', '/ventas', agTk, vbody);
  console.log('\nVenta CODIGO DUPLICADO: esperamos 400 o 409. status=', r.status, 'error=', r.body.error || '', 'msg=', (r.body.message || '').slice(0, 120));
  // 7) Validación: DNI inválido (pocos dígitos)
  r = await doRequest('POST', '/ventas', agTk, { ...vbody, dniCliente: '123', codigoLlamada: 'LLAM-BAD-' + Date.now() });
  console.log('Venta DNI inválido: esperamos 400. status=', r.status, 'msg=', (r.body.message || JSON.stringify(r.body)).slice(0, 180));
  // 8) Validación: teléfono inválido
  r = await doRequest('POST', '/ventas', agTk, { ...vbody, telefonoCliente: '123', codigoLlamada: 'LLAM-BAD2-' + Date.now() });
  console.log('Venta TEL inválido: esperamos 400. status=', r.status, 'msg=', (r.body.message || JSON.stringify(r.body)).slice(0, 180));
  // 9) AGENTE intenta aprobar (debe fallar 403)
  r = await doRequest('POST', '/ventas/1/aprobar', agTk);
  console.log('\nAGENTE intenta APROBAR (esperamos 401/403): status=', r.status);
})().catch(e => console.error('ERROR:', e));
