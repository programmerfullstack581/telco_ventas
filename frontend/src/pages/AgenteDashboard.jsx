import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Search } from 'lucide-react'
import api from '@/services/api'
import { confirmar, exito, error as swalError } from '@/services/swal'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DatePicker } from '@/components/ui/date-picker'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select'

const empty = {
  dniCliente: '', nombreCliente: '', telefonoCliente: '', direccionCliente: '',
  planActual: '', codigoLlamada: '', monto: '',
  clienteId: null, planId: null, distritoId: null
}

const ESTADO_BADGE = {
  PENDIENTE: 'warning',
  APROBADA: 'success',
  RECHAZADA: 'destructive'
}

export default function AgenteDashboard() {
  const [form, setForm] = useState(empty)
  const [planes, setPlanes] = useState([])
  const [distritos, setDistritos] = useState([])
  const [misVentas, setMisVentas] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadingDni, setLoadingDni] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [filtros, setFiltros] = useState({ estado: '', desde: '', hasta: '' })

  const load = useCallback(async () => {
    const qs = new URLSearchParams()
    if (filtros.estado) qs.set('estado', filtros.estado)
    if (filtros.desde) qs.set('desde', filtros.desde)
    if (filtros.hasta) qs.set('hasta', filtros.hasta)
    qs.set('page', 0); qs.set('size', 50)
    try {
      const { data } = await api.get(`/ventas/mis-ventas?${qs}`)
      setMisVentas(data.content || [])
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }, [filtros])

  useEffect(() => {
    load()
    api.get('/planes').then(({ data }) => setPlanes(data)).catch(() => {})
    api.get('/distritos').then(({ data }) => setDistritos(data)).catch(() => {})
  }, [load])

  const buscarDni = async () => {
    const dni = form.dniCliente.trim()
    if (!/^(\d{8}|\d{11})$/.test(dni)) { swalError('DNI inválido', 'Ingresa 8 dígitos (DNI) o 11 (RUC)'); return }
    setLoadingDni(true)
    try {
      const { data } = await api.get(`/clientes/dni/${dni}`)
      setForm(f => ({
        ...f,
        nombreCliente: data.nombreCliente,
        telefonoCliente: data.telefono,
        direccionCliente: data.direccion,
        distritoId: data.distritoId || null,
        clienteId: data.id
      }))
      toast.success(`Cliente encontrado: ${data.nombreCliente}`)
    } catch (e) {
      if (e.response?.status === 404) {
        toast('Cliente nuevo: completa los datos para registrarlo con la venta', { description: 'DNI ' + dni })
        setForm(f => ({ ...f, clienteId: null }))
      } else {
        swalError('Error', e.response?.data?.message || 'No se pudo consultar el cliente')
      }
    } finally {
      setLoadingDni(false)
    }
  }

  const setPlan = (planId) => {
    const plan = planes.find(p => p.id === Number(planId))
    if (!plan) return
    setForm(f => ({
      ...f,
      planId: plan.id,
      planNuevo: plan.nombre,
      monto: plan.precioBase,
      producto: plan.tipo === 'FIJA' ? 'FIJA' : 'FIJA_HOGAR'
    }))
  }

  const submit = async (e) => {
    if (e && e.preventDefault) e.preventDefault()
    setEnviando(true)
    try {
      await api.post('/ventas', { ...form, monto: Number(form.monto) })
      await exito('Venta registrada', 'La venta quedó en estado PENDIENTE para validación.')
      setForm(empty)
      load()
    } catch (e) {
      swalError('Error al registrar', e.response?.data?.message || 'Ocurrió un error')
    } finally {
      setEnviando(false)
    }
  }

  const fmt = n => n ? Number(n).toFixed(2) : '0.00'

  const conteo = { pendientes: 0, aprobadas: 0, rechazadas: 0 }
  misVentas.forEach(v => { if (conteo[v.estado] !== undefined) conteo[v.estado]++ })

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="pb-4">
          <CardTitle>Registrar nueva venta</CardTitle>
          <CardDescription>Busca el cliente por DNI para autocompletar, o regístralo junto con la venta.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={submit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div className="space-y-1.5">
                <Label>DNI / RUC Cliente *</Label>
                <div className="flex gap-2">
                  <Input
                    value={form.dniCliente}
                    onChange={e => setForm({ ...form, dniCliente: e.target.value })}
                    placeholder="8 o 11 dígitos"
                    maxLength="11"
                    required
                  />
                  <Button type="button" variant="outline" onClick={buscarDni} disabled={loadingDni}>
                    <Search className="h-4 w-4" />
                    {loadingDni ? '...' : 'Buscar'}
                  </Button>
                </div>
              </div>
              <div className="space-y-1.5">
                <Label>Nombre Cliente *</Label>
                <Input value={form.nombreCliente} onChange={e => setForm({ ...form, nombreCliente: e.target.value })} required />
              </div>
              <div className="space-y-1.5">
                <Label>Teléfono *</Label>
                <Input
                  value={form.telefonoCliente}
                  onChange={e => setForm({ ...form, telefonoCliente: e.target.value })}
                  placeholder="9 dígitos"
                  maxLength="9"
                  required
                />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label>Dirección *</Label>
                <Input value={form.direccionCliente} onChange={e => setForm({ ...form, direccionCliente: e.target.value })} required />
              </div>
              <div className="space-y-1.5">
                <Label>Distrito</Label>
                <Select value={form.distritoId?.toString() || ''} onValueChange={v => setForm({ ...form, distritoId: v ? Number(v) : null })}>
                  <SelectTrigger>
                    <SelectValue placeholder="Selecciona distrito" />
                  </SelectTrigger>
                  <SelectContent>
                    {distritos.map(d => (
                      <SelectItem key={d.id} value={String(d.id)}>{d.nombre} · {d.departamento}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Plan *</Label>
                <Select value={form.planId?.toString() || ''} onValueChange={setPlan}>
                  <SelectTrigger>
                    <SelectValue placeholder="Selecciona un plan" />
                  </SelectTrigger>
                  <SelectContent>
                    {planes.map(p => (
                      <SelectItem key={p.id} value={String(p.id)}>
                        {p.codigo} · S/ {fmt(p.precioBase)} ({p.tipo})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Plan Actual</Label>
                <Input value={form.planActual} onChange={e => setForm({ ...form, planActual: e.target.value })} placeholder="Opcional" />
              </div>
              <div className="space-y-1.5">
                <Label>Monto S/ *</Label>
                <Input
                  type="number" step="0.01" min="0.01"
                  value={form.monto}
                  onChange={e => setForm({ ...form, monto: e.target.value })}
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label>Código Llamada * (único)</Label>
                <Input
                  value={form.codigoLlamada}
                  onChange={e => setForm({ ...form, codigoLlamada: e.target.value })}
                  placeholder="ej: CALL-123"
                  required
                />
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="ghost"
                onClick={async () => {
                  const r = await confirmar({
                    titulo: '¿Registrar esta venta?',
                    texto: 'Revisa los datos antes de confirmar. La venta quedará PENDIENTE.',
                    confirmarTexto: 'Sí, registrar'
                  })
                  if (r.isConfirmed) submit(null)
                }}
              >
                Vista previa y confirmar
              </Button>
              <Button type="submit" disabled={enviando || !form.planId}>
                {enviando ? 'Registrando...' : 'Registrar Venta'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card className="border-l-4 border-l-amber-400">
          <CardContent className="p-4"><p className="text-2xl font-bold">{conteo.pendientes}</p><p className="text-sm text-muted-foreground">Pendientes</p></CardContent>
        </Card>
        <Card className="border-l-4 border-l-emerald-500">
          <CardContent className="p-4"><p className="text-2xl font-bold">{conteo.aprobadas}</p><p className="text-sm text-muted-foreground">Aprobadas</p></CardContent>
        </Card>
        <Card className="border-l-4 border-l-red-500">
          <CardContent className="p-4"><p className="text-2xl font-bold">{conteo.rechazadas}</p><p className="text-sm text-muted-foreground">Rechazadas</p></CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <CardTitle>Mis Ventas</CardTitle>
              <CardDescription>Filtra por estado o rango de fechas</CardDescription>
            </div>
            <div className="flex flex-wrap gap-2">
              <Select value={filtros.estado} onValueChange={v => setFiltros({ ...filtros, estado: v })}>
                <SelectTrigger className="w-36"><SelectValue placeholder="Estado" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="">Todos</SelectItem>
                  <SelectItem value="PENDIENTE">PENDIENTE</SelectItem>
                  <SelectItem value="APROBADA">APROBADA</SelectItem>
                  <SelectItem value="RECHAZADA">RECHAZADA</SelectItem>
                </SelectContent>
              </Select>
<DatePicker className="w-40" value={filtros.desde} onChange={d => setFiltros({ ...filtros, desde: d })} />
                <DatePicker className="w-40" value={filtros.hasta} onChange={d => setFiltros({ ...filtros, hasta: d })} />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Cód. Llamada</TableHead>
                  <TableHead>Cliente</TableHead>
                  <TableHead>Plan</TableHead>
                  <TableHead>Monto</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead>Registro</TableHead>
                  <TableHead>Motivo</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {misVentas.length === 0 && (
                  <TableRow><TableCell colSpan={7} className="h-24 text-center text-muted-foreground">Sin resultados</TableCell></TableRow>
                )}
                {misVentas.map(v => (
                  <TableRow key={v.id}>
                    <TableCell className="font-medium">{v.codigoLlamada}</TableCell>
                    <TableCell>{v.nombreCliente} <span className="text-muted-foreground text-xs">({v.dniCliente})</span></TableCell>
                    <TableCell>{v.planNuevo}</TableCell>
                    <TableCell>S/ {fmt(v.monto)}</TableCell>
                    <TableCell><Badge variant={ESTADO_BADGE[v.estado]}>{v.estado}</Badge></TableCell>
                    <TableCell className="text-muted-foreground text-xs">{v.fechaRegistro?.slice(0, 16).replace('T', ' ')}</TableCell>
                    <TableCell className="text-muted-foreground text-xs">{v.motivoRechazo || '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
