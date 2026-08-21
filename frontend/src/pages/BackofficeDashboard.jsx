import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Check, X } from 'lucide-react'
import api from '@/services/api'
import { confirmar, exito, error as swalError, pedirTexto } from '@/services/swal'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const ESTADO_BADGE = {
  PENDIENTE: 'warning',
  APROBADA: 'success',
  RECHAZADA: 'destructive'
}

export default function BackofficeDashboard() {
  const [pendientes, setPendientes] = useState([])
  const [todas, setTodas] = useState([])
  const [motivos, setMotivos] = useState({})
  const [loading, setLoading] = useState(true)

  const load = async () => {
    setLoading(true)
    try {
      const p = await api.get('/ventas/pendientes?size=100')
      setPendientes(p.data.content || [])
    } catch (e) { console.error(e) }
    try {
      const { data } = await api.get('/ventas/equipo')
      setTodas(data)
    } catch (e) { console.error(e) }
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  const aprobar = async v => {
    const r = await confirmar({
      titulo: '¿Aprobar esta venta?',
      texto: `${v.nombreCliente} · S/ ${fmt(v.monto)} · ${v.planNuevo}. Se generará la comisión del agente.`,
      confirmarTexto: 'Sí, aprobar',
      danger: false
    })
    if (!r.isConfirmed) return
    try {
      await api.post(`/ventas/${v.id}/aprobar`)
      toast.success(`Venta #${v.id} aprobada`)
      load()
    } catch (e) { swalError('Error', e.response?.data?.message || 'No se pudo aprobar') }
  }

  const rechazar = async v => {
    const r = await pedirTexto({
      titulo: 'Rechazar venta',
      texto: `Motivo del rechazo para ${v.nombreCliente}:`,
      placeholder: 'Motivo (mínimo 5 caracteres)',
      validar: m => m.trim().length >= 5
    })
    if (!r.isConfirmed || !r.value) return
    try {
      await api.post(`/ventas/${v.id}/rechazar`, { motivoRechazo: r.value.trim() })
      toast.error(`Venta #${v.id} rechazada`)
      setMotivos(m => ({ ...m, [v.id]: '' }))
      load()
    } catch (e) { swalError('Error', e.response?.data?.message || 'No se pudo rechazar') }
  }

  const fmt = n => n ? Number(n).toFixed(2) : '0.00'
  const filtTodas = todas.filter(v => v.estado !== 'PENDIENTE').slice(0, 15)

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="pb-4">
          <CardTitle>Ventas Pendientes de Validación</CardTitle>
          <CardDescription>{pendientes.length} venta(s) esperando tu revisión</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              <Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" />
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Cliente</TableHead>
                  <TableHead>Agente</TableHead>
                  <TableHead>Plan</TableHead>
                  <TableHead>Monto</TableHead>
                  <TableHead>Motivo (si rechaza)</TableHead>
                  <TableHead className="text-right">Acciones</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pendientes.length === 0 && (
                  <TableRow><TableCell colSpan={6} className="h-24 text-center text-muted-foreground">No hay ventas pendientes</TableCell></TableRow>
                )}
                {pendientes.map(v => (
                  <TableRow key={v.id}>
                    <TableCell>
                      <div className="font-medium">{v.nombreCliente}</div>
                      <div className="text-xs text-muted-foreground">{v.dniCliente} · {v.telefonoCliente}</div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{v.agenteUsername || '#' + v.agenteId}</TableCell>
                    <TableCell>{v.planNuevo}</TableCell>
                    <TableCell className="font-medium">S/ {fmt(v.monto)}</TableCell>
                    <TableCell className="min-w-[220px]">
                      <Input
                        placeholder="Motivo (solo si rechaza)"
                        value={motivos[v.id] || ''}
                        onChange={e => setMotivos({ ...motivos, [v.id]: e.target.value })}
                      />
                    </TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-2">
                        <Button variant="success" size="sm" onClick={() => aprobar(v)}>
                          <Check className="h-4 w-4" /> Aprobar
                        </Button>
                        <Button variant="destructive" size="sm" onClick={() => rechazar(v)}>
                          <X className="h-4 w-4" /> Rechazar
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle>Últimas validadas</CardTitle>
          <CardDescription>Historial reciente de ventas aprobadas y rechazadas</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Cód.</TableHead>
                <TableHead>Cliente</TableHead>
                <TableHead>Monto</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Motivo</TableHead>
                <TableHead>Validación</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtTodas.length === 0 && (
                <TableRow><TableCell colSpan={6} className="h-24 text-center text-muted-foreground">Sin datos</TableCell></TableRow>
              )}
              {filtTodas.map(v => (
                <TableRow key={v.id}>
                  <TableCell className="font-medium">{v.codigoLlamada}</TableCell>
                  <TableCell>{v.nombreCliente}</TableCell>
                  <TableCell>S/ {fmt(v.monto)}</TableCell>
                  <TableCell><Badge variant={ESTADO_BADGE[v.estado]}>{v.estado}</Badge></TableCell>
                  <TableCell className="text-muted-foreground">{v.motivoRechazo || '—'}</TableCell>
                  <TableCell className="text-muted-foreground text-xs">{v.fechaValidacion?.slice(0, 16).replace('T', ' ')}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
