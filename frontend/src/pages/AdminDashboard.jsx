import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Plus, Power, Search, ShieldCheck, Trash2 } from 'lucide-react'
import api from '@/services/api'
import { confirmar, exito } from '@/services/swal'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { DatePicker } from '@/components/ui/date-picker'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const TIPOS = ['FIJA', 'HOGAR', 'EMPRESARIAL']
const fmt = n => n ? Number(n).toFixed(2) : '0.00'

const emptyUsuario = { username: '', password: '', rol: 'AGENTE', supervisorId: '', activo: true }
const emptyPlan = { codigo: '', nombre: '', tipo: 'HOGAR', velocidadMbps: '', precioBase: '', descripcion: '', activo: true }
const emptyCliente = { dni: '', nombreCliente: '', telefono: '', direccion: '', distritoId: '', email: '' }

export default function AdminDashboard() {
  const [tab, setTab] = useState('usuarios')

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Panel de Administración</h2>
        <p className="text-sm text-muted-foreground">Gestión de usuarios, planes, clientes y auditoría del sistema</p>
      </div>
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="usuarios">Usuarios</TabsTrigger>
          <TabsTrigger value="roles"><ShieldCheck className="mr-1.5 h-4 w-4" />Roles y permisos</TabsTrigger>
          <TabsTrigger value="planes">Planes</TabsTrigger>
          <TabsTrigger value="clientes">Clientes</TabsTrigger>
          <TabsTrigger value="auditoria">Auditoría</TabsTrigger>
        </TabsList>
        <TabsContent value="usuarios"><UsuariosTab /></TabsContent>
        <TabsContent value="roles"><RolesTab /></TabsContent>
        <TabsContent value="planes"><PlanesTab /></TabsContent>
        <TabsContent value="clientes"><ClientesTab /></TabsContent>
        <TabsContent value="auditoria"><AuditoriaTab /></TabsContent>
      </Tabs>
    </div>
  )
}

function UsuariosTab() {
  const { user } = useAuth()
  const [usuarios, setUsuarios] = useState([])
  const [opciones, setOpciones] = useState([])
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [dialogo, setDialogo] = useState(null)
  const [form, setForm] = useState(emptyUsuario)

  const load = useCallback(async () => {
    try {
      const [u, o, r] = await Promise.all([api.get('/usuarios'), api.get('/usuarios/opciones'), api.get('/roles')])
      setUsuarios(u.data)
      setOpciones(o.data)
      setRoles(r.data)
    } catch (e) { console.error(e) } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const guardar = async () => {
    try {
      const body = {
        ...form,
        supervisorId: form.supervisorId ? Number(form.supervisorId) : null
      }
      if (dialogo?.id) {
        await api.put(`/usuarios/${dialogo.id}`, { ...body, username: undefined, password: form.password || undefined })
        toast.success('Usuario actualizado')
      } else {
        await api.post('/usuarios', body)
        toast.success('Usuario creado')
      }
      setDialogo(null)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al guardar')
    }
  }

  const toggleEstado = async (u) => {
    const r = await confirmar({
      titulo: u.activo ? '¿Inhabilitar usuario?' : '¿Habilitar usuario?',
      texto: u.username + ' · ' + u.rol,
      confirmarTexto: u.activo ? 'Sí, inhabilitar' : 'Sí, habilitar',
      danger: u.activo
    })
    if (!r.isConfirmed) return
    try {
      await api.patch(`/usuarios/${u.id}/estado?activo=${!u.activo}`)
      toast.success(`Usuario ${u.activo ? 'inhabilitado' : 'habilitado'}`)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al cambiar estado')
    }
  }

  const eliminar = async (u) => {
    const r = await confirmar({
      titulo: '¿Eliminar usuario?',
      texto: `Se borrará definitivamente ${u.username} de la base de datos, junto con sus ventas, comisiones, historial y auditoría. Esta acción no se puede deshacer.`,
      confirmarTexto: 'Sí, eliminar',
      danger: true
    })
    if (!r.isConfirmed) return
    try {
      await api.delete(`/usuarios/${u.id}`)
      toast.success('Usuario eliminado de la base de datos')
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al eliminar usuario')
    }
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between pb-4">
        <div>
          <CardTitle>Usuarios del sistema</CardTitle>
          <CardDescription>Gestión de cuentas y roles</CardDescription>
        </div>
        <Button onClick={() => { setForm(emptyUsuario); setDialogo({}) }}>
          <Plus className="h-4 w-4" /> Nuevo usuario
        </Button>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="space-y-2"><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /></div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Usuario</TableHead>
                <TableHead>Rol</TableHead>
                <TableHead>Supervisor</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {usuarios.map(u => (
                <TableRow key={u.id}>
                  <TableCell className="font-medium">{u.username}</TableCell>
                  <TableCell><Badge variant={u.rol === 'ADMIN' ? 'destructive' : u.rol === 'SUPERVISOR' ? 'warning' : 'info'}>{u.rol}</Badge></TableCell>
                  <TableCell className="text-muted-foreground">{u.supervisorUsername || '—'}</TableCell>
                  <TableCell>{u.activo ? <Badge variant="success">Activo</Badge> : <Badge variant="secondary">Inactivo</Badge>}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Button variant="outline" size="sm" onClick={() => { setForm({ ...emptyUsuario, ...u, supervisorId: u.supervisorId || '', password: '' }); setDialogo({ id: u.id }) }}>
                        Editar
                      </Button>
                      <Button variant="ghost" size="sm" title={u.activo ? 'Inhabilitar' : 'Habilitar'} disabled={user?.userId === u.id} onClick={() => toggleEstado(u)}>
                        <Power className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" title="Eliminar de la base de datos" disabled={user?.userId === u.id} onClick={() => eliminar(u)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>

      <Dialog open={!!dialogo} onOpenChange={o => !o && setDialogo(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{dialogo?.id ? 'Editar usuario' : 'Nuevo usuario'}</DialogTitle>
            <DialogDescription>Define credenciales, rol y supervisor asignado.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="space-y-1.5">
              <Label>Username</Label>
              <Input value={form.username} disabled={!!dialogo?.id} onChange={e => setForm({ ...form, username: e.target.value })} required />
            </div>
            <div className="space-y-1.5">
              <Label>{dialogo?.id ? 'Nueva contraseña (opcional)' : 'Contraseña'}</Label>
              <Input type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} placeholder={dialogo?.id ? 'Dejar vacío para no cambiar' : 'Mínimo 6 caracteres'} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Rol</Label>
                <Select value={form.rol} onValueChange={v => setForm({ ...form, rol: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {roles.filter(r => r.activo).map(r => <SelectItem key={r.id} value={r.nombre}>{r.nombre}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Estado</Label>
                <Select value={String(form.activo)} onValueChange={v => setForm({ ...form, activo: v === 'true' })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">Activo</SelectItem>
                    <SelectItem value="false">Inactivo</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            {form.rol === 'AGENTE' && (
              <div className="space-y-1.5">
                <Label>Supervisor</Label>
                <Select value={form.supervisorId?.toString() || ''} onValueChange={v => setForm({ ...form, supervisorId: v })}>
                  <SelectTrigger><SelectValue placeholder="Sin supervisor" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">Sin supervisor</SelectItem>
                    {opciones.filter(o => o.rol === 'SUPERVISOR').map(o => (
                      <SelectItem key={o.id} value={String(o.id)}>{o.username}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogo(null)}>Cancelar</Button>
            <Button onClick={guardar} disabled={!form.username || (!dialogo?.id && !form.password)}>Guardar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function PlanesTab() {
  const [planes, setPlanes] = useState([])
  const [loading, setLoading] = useState(true)
  const [dialogo, setDialogo] = useState(null)
  const [form, setForm] = useState(emptyPlan)

  const load = useCallback(async () => {
    try {
      const { data } = await api.get('/planes/todos')
      setPlanes(data)
    } catch (e) { console.error(e) } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const guardar = async () => {
    try {
      const body = { ...form, velocidadMbps: form.velocidadMbps ? Number(form.velocidadMbps) : null, precioBase: Number(form.precioBase) }
      if (dialogo?.id) {
        await api.put(`/planes/${dialogo.id}`, body)
        toast.success('Plan actualizado')
      } else {
        await api.post('/planes', body)
        toast.success('Plan creado')
      }
      setDialogo(null)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al guardar')
    }
  }

  const toggle = async (p) => {
    const r = await confirmar({
      titulo: p.activo ? '¿Desactivar plan?' : '¿Activar plan?',
      texto: p.codigo + ' · ' + p.nombre,
      confirmarTexto: p.activo ? 'Sí, desactivar' : 'Sí, activar',
      danger: p.activo
    })
    if (!r.isConfirmed) return
    try {
      await api.patch(`/planes/${p.id}/estado?activo=${!p.activo}`)
      toast.success(`Plan ${p.activo ? 'desactivado' : 'activado'}`)
      load()
    } catch (e) { toast.error(e.response?.data?.message || 'Error') }
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between pb-4">
        <div>
          <CardTitle>Catálogo de planes</CardTitle>
          <CardDescription>Planes disponibles para las ventas de agentes</CardDescription>
        </div>
        <Button onClick={() => { setForm(emptyPlan); setDialogo({}) }}>
          <Plus className="h-4 w-4" /> Nuevo plan
        </Button>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="space-y-2"><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /></div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Código</TableHead>
                <TableHead>Nombre</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead>Velocidad</TableHead>
                <TableHead>Precio base</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {planes.map(p => (
                <TableRow key={p.id}>
                  <TableCell><Badge variant="info">{p.codigo}</Badge></TableCell>
                  <TableCell className="font-medium">{p.nombre}</TableCell>
                  <TableCell className="text-muted-foreground">{p.tipo}</TableCell>
                  <TableCell>{p.velocidadMbps ? p.velocidadMbps + ' Mbps' : '—'}</TableCell>
                  <TableCell>S/ {fmt(p.precioBase)}</TableCell>
                  <TableCell>{p.activo ? <Badge variant="success">Activo</Badge> : <Badge variant="secondary">Inactivo</Badge>}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => { setForm({ ...emptyPlan, ...p, velocidadMbps: p.velocidadMbps || '' }); setDialogo({ id: p.id }) }}>Editar</Button>
                      <Button variant="ghost" size="sm" onClick={() => toggle(p)}><Power className="h-4 w-4" /></Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>

      <Dialog open={!!dialogo} onOpenChange={o => !o && setDialogo(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{dialogo?.id ? 'Editar plan' : 'Nuevo plan'}</DialogTitle>
            <DialogDescription>Define el catálogo que verán los agentes.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Código *</Label>
                <Input value={form.codigo} onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })} placeholder="HOGAR_100" required />
              </div>
              <div className="space-y-1.5">
                <Label>Tipo *</Label>
                <Select value={form.tipo} onValueChange={v => setForm({ ...form, tipo: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{TIPOS.map(t => <SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Nombre *</Label>
              <Input value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Velocidad (Mbps)</Label>
                <Input type="number" min="1" value={form.velocidadMbps} onChange={e => setForm({ ...form, velocidadMbps: e.target.value })} />
              </div>
              <div className="space-y-1.5">
                <Label>Precio base S/ *</Label>
                <Input type="number" step="0.01" min="0.01" value={form.precioBase} onChange={e => setForm({ ...form, precioBase: e.target.value })} required />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Descripción</Label>
              <Input value={form.descripcion} onChange={e => setForm({ ...form, descripcion: e.target.value })} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogo(null)}>Cancelar</Button>
            <Button onClick={guardar} disabled={!form.codigo || !form.nombre || !form.precioBase}>Guardar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function ClientesTab() {
  const [clientes, setClientes] = useState([])
  const [distritos, setDistritos] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [dialogo, setDialogo] = useState(null)
  const [form, setForm] = useState(emptyCliente)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const qs = new URLSearchParams()
      if (search) qs.set('search', search)
      qs.set('page', 0); qs.set('size', 20)
      const [c, d] = await Promise.all([api.get(`/clientes?${qs}`), api.get('/distritos')])
      setClientes(c.data.content || [])
      setDistritos(d.data)
    } catch (e) { console.error(e) } finally { setLoading(false) }
  }, [search])

  useEffect(() => { load() }, [load])

  const guardar = async () => {
    try {
      const body = { ...form, distritoId: form.distritoId ? Number(form.distritoId) : null }
      if (dialogo?.id) {
        await api.put(`/clientes/${dialogo.id}`, body)
        toast.success('Cliente actualizado')
      } else {
        await api.post('/clientes', body)
        toast.success('Cliente creado')
      }
      setDialogo(null)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al guardar')
    }
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between pb-4">
        <div>
          <CardTitle>Clientes registrados</CardTitle>
          <CardDescription>Búsqueda por DNI o nombre</CardDescription>
        </div>
        <div className="flex gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input className="w-64 pl-9" placeholder="Buscar..." value={search} onChange={e => setSearch(e.target.value)} />
          </div>
          <Button onClick={() => { setForm(emptyCliente); setDialogo({}) }}>
            <Plus className="h-4 w-4" /> Nuevo
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="space-y-2"><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /></div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>DNI</TableHead>
                <TableHead>Nombre</TableHead>
                <TableHead>Teléfono</TableHead>
                <TableHead>Dirección</TableHead>
                <TableHead>Distrito</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {clientes.length === 0 && <TableRow><TableCell colSpan={6} className="h-16 text-center text-muted-foreground">Sin clientes</TableCell></TableRow>}
              {clientes.map(c => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium">{c.dni}</TableCell>
                  <TableCell>{c.nombreCliente}</TableCell>
                  <TableCell>{c.telefono}</TableCell>
                  <TableCell className="text-muted-foreground max-w-[240px] truncate">{c.direccion}</TableCell>
                  <TableCell>{c.distritoNombre || '—'}</TableCell>
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" onClick={() => { setForm({ ...emptyCliente, ...c, distritoId: c.distritoId || '' }); setDialogo({ id: c.id }) }}>Editar</Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>

      <Dialog open={!!dialogo} onOpenChange={o => !o && setDialogo(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{dialogo?.id ? 'Editar cliente' : 'Nuevo cliente'}</DialogTitle>
            <DialogDescription>Datos del cliente registrado en el sistema.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>DNI / RUC *</Label>
                <Input value={form.dni} onChange={e => setForm({ ...form, dni: e.target.value })} maxLength="11" required />
              </div>
              <div className="space-y-1.5">
                <Label>Teléfono *</Label>
                <Input value={form.telefono} onChange={e => setForm({ ...form, telefono: e.target.value })} maxLength="9" required />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Nombre completo *</Label>
              <Input value={form.nombreCliente} onChange={e => setForm({ ...form, nombreCliente: e.target.value })} required />
            </div>
            <div className="space-y-1.5">
              <Label>Dirección *</Label>
              <Input value={form.direccion} onChange={e => setForm({ ...form, direccion: e.target.value })} required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Distrito</Label>
                <Select value={form.distritoId?.toString() || ''} onValueChange={v => setForm({ ...form, distritoId: v })}>
                  <SelectTrigger><SelectValue placeholder="Selecciona" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">Sin distrito</SelectItem>
                    {distritos.map(d => <SelectItem key={d.id} value={String(d.id)}>{d.nombre}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Email</Label>
                <Input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogo(null)}>Cancelar</Button>
            <Button onClick={guardar} disabled={!form.dni || !form.nombreCliente || !form.telefono || !form.direccion}>Guardar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function AuditoriaTab() {
  const [page, setPage] = useState({ number: 0, totalPages: 0, totalElements: 0 })
  const [items, setItems] = useState([])
  const [filtros, setFiltros] = useState({ accion: '', usuario: '', desde: '', hasta: '' })
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const qs = new URLSearchParams()
      if (filtros.accion) qs.set('accion', filtros.accion)
      if (filtros.usuario) qs.set('usuario', filtros.usuario)
      if (filtros.desde) qs.set('desde', filtros.desde)
      if (filtros.hasta) qs.set('hasta', filtros.hasta)
      qs.set('page', page.number); qs.set('size', 15)
      const { data } = await api.get(`/auditoria?${qs}`)
      setItems(data.content || [])
      setPage({ number: data.number, totalPages: data.totalPages, totalElements: data.totalElements })
    } catch (e) { console.error(e) } finally { setLoading(false) }
  }, [filtros, page.number])

  useEffect(() => { load() }, [load])

  const ir = n => { setPage(p => ({ ...p, number: n })) }

  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle>Bitácora de auditoría</CardTitle>
        <CardDescription>{page.totalElements} registro(s) · acciones del sistema</CardDescription>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <Input placeholder="Acción (ej: LOGIN, APROBAR_VENTA)" value={filtros.accion} onChange={e => setFiltros({ ...filtros, accion: e.target.value })} />
          <Input placeholder="Usuario" value={filtros.usuario} onChange={e => setFiltros({ ...filtros, usuario: e.target.value })} />
          <DatePicker value={filtros.desde} onChange={d => setFiltros({ ...filtros, desde: d })} />
          <DatePicker value={filtros.hasta} onChange={d => setFiltros({ ...filtros, hasta: d })} />
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="space-y-2"><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /></div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Fecha</TableHead>
                <TableHead>Usuario</TableHead>
                <TableHead>Acción</TableHead>
                <TableHead>Entidad</TableHead>
                <TableHead>Detalle</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.length === 0 && <TableRow><TableCell colSpan={5} className="h-16 text-center text-muted-foreground">Sin registros</TableCell></TableRow>}
              {items.map(a => (
                <TableRow key={a.id}>
                  <TableCell className="text-muted-foreground text-xs">{a.fecha?.slice(0, 16).replace('T', ' ')}</TableCell>
                  <TableCell className="font-medium">{a.usuarioUsername || '—'}</TableCell>
                  <TableCell><Badge variant={a.accion?.includes('ERROR') ? 'destructive' : 'info'}>{a.accion}</Badge></TableCell>
                  <TableCell className="text-muted-foreground text-xs">{a.entidad}{a.entidadId ? ' #' + a.entidadId : ''}</TableCell>
                  <TableCell className="text-muted-foreground max-w-[320px] truncate">{a.detalle || '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {page.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-end gap-2">
            <Button variant="outline" size="sm" disabled={page.number === 0} onClick={() => ir(page.number - 1)}>Anterior</Button>
            <span className="text-sm text-muted-foreground">Pág. {page.number + 1} de {page.totalPages}</span>
            <Button variant="outline" size="sm" disabled={page.number + 1 >= page.totalPages} onClick={() => ir(page.number + 1)}>Siguiente</Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

const MODULO_LABEL = {
  DASHBOARD: 'Panel principal',
  USUARIOS: 'Usuarios',
  ROLES: 'Roles y permisos',
  PLANES: 'Planes',
  VENTAS: 'Ventas',
  CLIENTES: 'Clientes',
  COMISIONES: 'Comisiones',
  REPORTES: 'Reportes',
  AUDITORIA: 'Auditoría'
}

const emptyRol = { nombre: '', descripcion: '', activo: true, permisos: new Set() }

function RolesTab() {
  const { user } = useAuth()
  const [roles, setRoles] = useState([])
  const [permisos, setPermisos] = useState([])
  const [loading, setLoading] = useState(true)
  const [dialogo, setDialogo] = useState(null)
  const [form, setForm] = useState(emptyRol)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [r, p] = await Promise.all([api.get('/roles'), api.get('/permisos')])
      setRoles(r.data)
      setPermisos(p.data)
    } catch (e) { console.error(e) } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const modulos = Object.keys(MODULO_LABEL)
    .map(modulo => ({ modulo, label: MODULO_LABEL[modulo], items: permisos.filter(p => p.modulo === modulo) }))
    .filter(g => g.items.length > 0)

  const guardar = async () => {
    try {
      const body = {
        nombre: form.nombre,
        descripcion: form.descripcion,
        activo: form.activo,
        permisos: Array.from(form.permisos)
      }
      if (dialogo?.id) {
        await api.put(`/roles/${dialogo.id}`, body)
        toast.success('Rol actualizado')
      } else {
        await api.post('/roles', body)
        toast.success('Rol creado')
      }
      setDialogo(null)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al guardar')
    }
  }

  const togglePermiso = (codigo) => {
    setForm(f => {
      const permisos = new Set(f.permisos)
      if (permisos.has(codigo)) permisos.delete(codigo)
      else permisos.add(codigo)
      return { ...f, permisos }
    })
  }

  const toggleEstado = async (r) => {
    const esPropio = user?.rolId === r.id
    const confirmado = await confirmar({
      titulo: r.activo ? '¿Desactivar rol?' : '¿Activar rol?',
      texto: r.nombre + ' · ' + (esPropio ? 'Tu cuenta usa este rol' : r.usuarios + ' usuario(s) asignado(s)'),
      confirmarTexto: r.activo ? 'Sí, desactivar' : 'Sí, activar',
      danger: r.activo
    })
    if (!confirmado.isConfirmed) return
    try {
      await api.patch(`/roles/${r.id}/estado?activo=${!r.activo}`)
      toast.success(`Rol ${r.activo ? 'desactivado' : 'activado'}`)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al cambiar estado')
    }
  }

  const eliminar = async (r) => {
    const confirmado = await confirmar({
      titulo: '¿Eliminar rol?',
      texto: r.nombre + ' · solo si no tiene usuarios asignados. Esta acción no se puede deshacer.',
      confirmarTexto: 'Sí, eliminar',
      danger: true
    })
    if (!confirmado.isConfirmed) return
    try {
      await api.delete(`/roles/${r.id}`)
      toast.success('Rol eliminado')
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Error al eliminar rol')
    }
  }

  const abrirEdicion = (r) => {
    setForm({
      nombre: r.nombre,
      descripcion: r.descripcion || '',
      activo: r.activo,
      permisos: new Set(r.permisos || [])
    })
    setDialogo({ id: r.id, nombre: r.nombre })
  }

  const esAdminEdit = dialogo?.nombre === 'ADMIN'
  const esRolPropio = dialogo?.id != null && user?.rolId === dialogo.id

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between pb-4">
        <div>
          <CardTitle>Roles y permisos</CardTitle>
          <CardDescription>Define los permisos que tiene cada rol del sistema</CardDescription>
        </div>
        <Button onClick={() => { setForm(emptyRol); setDialogo({}) }}>
          <Plus className="h-4 w-4" /> Nuevo rol
        </Button>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="space-y-2"><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /></div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Rol</TableHead>
                <TableHead>Descripción</TableHead>
                <TableHead>Permisos</TableHead>
                <TableHead>Usuarios</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {roles.map(r => (
                <TableRow key={r.id}>
                  <TableCell className="font-medium">
                    <div className="flex items-center gap-2">
                      <Badge variant={r.nombre === 'ADMIN' ? 'destructive' : 'info'}>{r.nombre}</Badge>
                      {user?.rolId === r.id && <Badge variant="secondary">Actual</Badge>}
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[260px]">{r.descripcion || '—'}</TableCell>
                  <TableCell>{r.permisos?.length ?? 0} permiso(s)</TableCell>
                  <TableCell>{r.usuarios}</TableCell>
                  <TableCell>{r.activo ? <Badge variant="success">Activo</Badge> : <Badge variant="secondary">Inactivo</Badge>}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => abrirEdicion(r)}>Editar</Button>
                      <Button variant="ghost" size="sm" disabled={r.nombre === 'ADMIN'} title={r.activo ? 'Desactivar' : 'Activar'} onClick={() => toggleEstado(r)}>
                        <Power className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" disabled={r.nombre === 'ADMIN'} title="Eliminar" onClick={() => eliminar(r)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>

      <Dialog open={!!dialogo} onOpenChange={o => !o && setDialogo(null)}>
        <DialogContent className="max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{dialogo?.id ? 'Editar rol' : 'Nuevo rol'}</DialogTitle>
            <DialogDescription>Asigna permisos por módulo. El nombre se guarda en mayúsculas.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Nombre *</Label>
                <Input value={form.nombre} disabled={esAdminEdit} onChange={e => setForm({ ...form, nombre: e.target.value.toUpperCase() })} placeholder="VENTAS_EXT" required />
              </div>
              <div className="space-y-1.5">
                <Label>Estado</Label>
                <Select value={String(form.activo)} onValueChange={v => setForm({ ...form, activo: v === 'true' })} disabled={esRolPropio && !form.activo}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">Activo</SelectItem>
                    <SelectItem value="false">Inactivo</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Descripción</Label>
              <Input value={form.descripcion} onChange={e => setForm({ ...form, descripcion: e.target.value })} />
            </div>
            <div className="space-y-3">
              <Label>Permisos por módulo</Label>
              <div className="grid gap-3 sm:grid-cols-2">
                {modulos.map(g => (
                  <div key={g.modulo} className="rounded-lg border p-3">
                    <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{g.label}</p>
                    <div className="space-y-1.5">
                      {g.items.map(p => {
                        const bloqueado = (esAdminEdit && p.codigo === 'ROLES_VER') || (esRolPropio && p.codigo === 'ROLES_EDITAR')
                        return (
                          <label key={p.id} className={`flex items-start gap-2 text-sm ${bloqueado ? 'opacity-60' : 'cursor-pointer'}`}>
                            <input
                              type="checkbox"
                              className="mt-0.5 h-4 w-4 accent-primary"
                              checked={form.permisos.has(p.codigo)}
                              disabled={bloqueado}
                              onChange={() => togglePermiso(p.codigo)}
                            />
                            <span>
                              <span className="font-medium">{p.accion}</span>
                              <span className="text-muted-foreground"> · {p.codigo}</span>
                              <span className="block text-xs text-muted-foreground">{p.descripcion}</span>
                            </span>
                          </label>
                        )
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogo(null)}>Cancelar</Button>
            <Button onClick={guardar} disabled={!form.nombre}>Guardar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}
