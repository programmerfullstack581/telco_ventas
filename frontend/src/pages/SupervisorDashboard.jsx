import { useEffect, useState } from 'react'
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, Cell
} from 'recharts'
import { CalendarDays, DollarSign, Download, FileCheck2, ThumbsDown, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import api from '@/services/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { DatePicker } from '@/components/ui/date-picker'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const PALETA = ['#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316']

const fmt = n => n ? Number(n).toFixed(2) : '0.00'

const TIPOS_REPORTE = [
  { value: 'resumen', label: 'Resumen general' },
  { value: 'por-plan', label: 'Ventas por plan' },
  { value: 'por-agente', label: 'Ventas por agente' },
  { value: 'comisiones', label: 'Comisiones' }
]

const FORMATOS = [
  { value: 'xlsx', label: 'Excel (.xlsx)' },
  { value: 'pdf', label: 'PDF' },
  { value: 'csv', label: 'CSV' },
  { value: 'html', label: 'HTML' }
]

function StatCard({ icon: Icon, label, value, sub, className }) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 p-4">
        <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-white ${className}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="min-w-0">
          <p className="truncate text-xs text-muted-foreground">{label}</p>
          <p className="text-xl font-bold leading-tight">{value}</p>
          {sub && <p className="truncate text-xs text-muted-foreground">{sub}</p>}
        </div>
      </CardContent>
    </Card>
  )
}

export default function SupervisorDashboard() {
  const [rango, setRango] = useState({ desde: '', hasta: '' })
  const [resumen, setResumen] = useState(null)
  const [porPlan, setPorPlan] = useState([])
  const [porAgente, setPorAgente] = useState([])
  const [comisiones, setComisiones] = useState(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('plan')
  const [tipoExport, setTipoExport] = useState('por-plan')
  const [formatoExport, setFormatoExport] = useState('xlsx')
  const [exportando, setExportando] = useState(false)

  const cargar = async () => {
    setLoading(true)
    const qs = new URLSearchParams()
    if (rango.desde) qs.set('desde', rango.desde)
    if (rango.hasta) qs.set('hasta', rango.hasta)
    const suffix = qs.toString() ? `?${qs}` : ''
    try {
      const [r, p, a, c] = await Promise.all([
        api.get(`/reportes/resumen${suffix}`),
        api.get(`/reportes/por-plan${suffix}`),
        api.get(`/reportes/por-agente${suffix}`),
        api.get('/reportes/comisiones')
      ])
      setResumen(r.data)
      setPorPlan(p.data)
      setPorAgente(a.data)
      setComisiones(c.data)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { cargar() }, [rango])

  useEffect(() => {
    const mapa = { plan: 'por-plan', agente: 'por-agente', comisiones: 'comisiones' }
    if (mapa[tab]) setTipoExport(mapa[tab])
  }, [tab])

  const exportar = async () => {
    setExportando(true)
    const params = { tipo: tipoExport, formato: formatoExport }
    if (rango.desde) params.desde = rango.desde
    if (rango.hasta) params.hasta = rango.hasta
    try {
      const res = await api.get('/reportes/exportar', { params, responseType: 'blob' })
      const cd = res.headers['content-disposition'] || ''
      let nombre = `reporte.${formatoExport === 'xlsx' ? 'xlsx' : formatoExport}`
      const starMatch = cd.match(/filename\*=UTF-8''([^;]+)/i)
      if (starMatch) {
        nombre = decodeURIComponent(starMatch[1].trim())
      } else {
        const plainMatch = cd.match(/filename="?([^";]+)"?/i)
        if (plainMatch && !plainMatch[1].includes('=?UTF-8')) nombre = plainMatch[1]
      }
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = nombre
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
      toast.success('Reporte descargado')
    } catch (e) {
      toast.error('Error al generar el reporte')
    } finally {
      setExportando(false)
    }
  }

  const dias = resumen?.ventasPorDia?.map(d => ({
    ...d,
    fecha: String(d.fecha).slice(0, 10).split('-').reverse().join('/')
  })) || []

  return (
    <div className="space-y-6">
      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 p-4">
          <div className="space-y-1.5">
            <Label>Desde</Label>
            <DatePicker className="w-44" value={rango.desde} onChange={d => setRango({ ...rango, desde: d })} />
          </div>
          <div className="space-y-1.5">
            <Label>Hasta</Label>
            <DatePicker className="w-44" value={rango.hasta} onChange={d => setRango({ ...rango, hasta: d })} />
          </div>
          <Button variant="outline" onClick={() => setRango({ desde: '', hasta: '' })}>Limpiar</Button>
          <div className="ml-auto flex flex-wrap items-end gap-3">
            <div className="space-y-1.5">
              <Label>Reporte</Label>
              <Select value={tipoExport} onValueChange={setTipoExport}>
                <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {TIPOS_REPORTE.map(t => <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Formato</Label>
              <Select value={formatoExport} onValueChange={setFormatoExport}>
                <SelectTrigger className="w-36"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {FORMATOS.map(f => <SelectItem key={f.value} value={f.value}>{f.label}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <Button onClick={exportar} disabled={exportando}>
              <Download className="h-4 w-4" /> {exportando ? 'Generando...' : 'Exportar'}
            </Button>
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {[0, 1, 2, 3, 4].map(i => <Skeleton key={i} className="h-20 rounded-xl" />)}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <StatCard icon={CalendarDays} label="Total ventas" value={resumen?.totalVentas ?? 0} className="bg-blue-600" />
          <StatCard icon={XCircle} label="Pendientes" value={resumen?.totalPendientes ?? 0} className="bg-amber-500" />
          <StatCard icon={FileCheck2} label="Aprobadas" value={resumen?.totalAprobadas ?? 0} className="bg-emerald-600" />
          <StatCard icon={ThumbsDown} label="Rechazadas" value={resumen?.totalRechazadas ?? 0} className="bg-red-500" />
          <StatCard icon={DollarSign} label="Monto aprobado" value={`S/ ${fmt(resumen?.montoTotalAprobadas)}`} className="bg-violet-600" />
        </div>
      )}

      <Card>
        <CardHeader className="pb-2">
          <CardTitle>Ventas por día</CardTitle>
          <CardDescription>Serie diaria en el período seleccionado</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <Skeleton className="h-56 w-full rounded-lg" />
          ) : dias.length === 0 ? (
            <div className="grid h-56 place-items-center text-muted-foreground">Sin datos en el período</div>
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={dias} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="fecha" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(v, n) => [n === 'monto' ? `S/ ${fmt(v)}` : v, n === 'monto' ? 'Monto' : 'Cantidad']} />
                  <Bar dataKey="cantidad" fill="#2563eb" radius={[4, 4, 0, 0]} name="Cantidad" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </CardContent>
      </Card>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="plan">Por Plan</TabsTrigger>
          <TabsTrigger value="agente">Por Agente</TabsTrigger>
          <TabsTrigger value="comisiones">Comisiones</TabsTrigger>
        </TabsList>

        <TabsContent value="plan">
          <Card>
            <CardContent className="pt-6">
              <div className="mb-4 h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={porPlan} layout="vertical" margin={{ top: 0, right: 8, left: 8, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" horizontal={false} />
                    <XAxis type="number" tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="planCodigo" width={90} tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(v) => [v, 'Cantidad']} />
                    <Bar dataKey="cantidad" radius={[0, 4, 4, 0]}>
                      {porPlan.map((_, i) => <Cell key={i} fill={PALETA[i % PALETA.length]} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Plan</TableHead>
                    <TableHead>Nombre</TableHead>
                    <TableHead>Cantidad</TableHead>
                    <TableHead>Aprobadas</TableHead>
                    <TableHead>Monto aprobado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {porPlan.length === 0 && <TableRow><TableCell colSpan={5} className="h-16 text-center text-muted-foreground">Sin datos</TableCell></TableRow>}
                  {porPlan.map(p => (
                    <TableRow key={p.planId || p.planCodigo}>
                      <TableCell><Badge variant="info">{p.planCodigo}</Badge></TableCell>
                      <TableCell>{p.planNombre}</TableCell>
                      <TableCell className="font-medium">{p.cantidad}</TableCell>
                      <TableCell>{p.aprobadas}</TableCell>
                      <TableCell>S/ {fmt(p.montoAprobado)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="agente">
          <Card>
            <CardContent className="pt-6">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Agente</TableHead>
                    <TableHead>Total</TableHead>
                    <TableHead>Pendientes</TableHead>
                    <TableHead>Aprobadas</TableHead>
                    <TableHead>Rechazadas</TableHead>
                    <TableHead>Monto aprobado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {porAgente.length === 0 && <TableRow><TableCell colSpan={6} className="h-16 text-center text-muted-foreground">Sin datos</TableCell></TableRow>}
                  {porAgente.map(a => (
                    <TableRow key={a.agenteId}>
                      <TableCell className="font-medium">{a.agenteUsername}</TableCell>
                      <TableCell>{a.total}</TableCell>
                      <TableCell><Badge variant="warning">{a.pendientes}</Badge></TableCell>
                      <TableCell><Badge variant="success">{a.aprobadas}</Badge></TableCell>
                      <TableCell><Badge variant="destructive">{a.rechazadas}</Badge></TableCell>
                      <TableCell>S/ {fmt(a.montoAprobado)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="comisiones">
          <div className="grid gap-4 sm:grid-cols-2">
            <Card className="border-emerald-200">
              <CardContent className="p-4">
                <p className="text-xs text-muted-foreground">Total pendiente de pago</p>
                <p className="text-2xl font-bold text-emerald-600">S/ {fmt(comisiones?.totalPendiente)}</p>
              </CardContent>
            </Card>
            <Card className="border-blue-200">
              <CardContent className="p-4">
                <p className="text-xs text-muted-foreground">Total pagado</p>
                <p className="text-2xl font-bold text-blue-600">S/ {fmt(comisiones?.totalPagado)}</p>
              </CardContent>
            </Card>
          </div>
          <Card className="mt-4">
            <CardContent className="pt-6">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Agente</TableHead>
                    <TableHead>Total</TableHead>
                    <TableHead>Pendientes</TableHead>
                    <TableHead>Pagadas</TableHead>
                    <TableHead>Monto pendiente</TableHead>
                    <TableHead>Monto pagado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(comisiones?.porAgente || []).length === 0 && <TableRow><TableCell colSpan={6} className="h-16 text-center text-muted-foreground">Sin comisiones</TableCell></TableRow>}
                  {(comisiones?.porAgente || []).map(c => (
                    <TableRow key={c.agenteId}>
                      <TableCell className="font-medium">{c.agenteUsername}</TableCell>
                      <TableCell>{c.total}</TableCell>
                      <TableCell><Badge variant="warning">{c.pendientes}</Badge></TableCell>
                      <TableCell><Badge variant="success">{c.pagadas}</Badge></TableCell>
                      <TableCell>S/ {fmt(c.montoPendiente)}</TableCell>
                      <TableCell>S/ {fmt(c.montoPagado)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
