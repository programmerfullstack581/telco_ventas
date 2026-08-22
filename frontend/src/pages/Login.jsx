import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Loader2, Lock, PhoneCall, User } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export default function Login() {
  const [form, setForm] = useState({ username: '', password: '' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const nav = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setErr(''); setLoading(true)
    try {
      const r = await login(form.username, form.password)
      if (r.rol === 'AGENTE') nav('/agente')
      else if (r.rol === 'BACKOFFICE') nav('/backoffice')
      else if (r.rol === 'SUPERVISOR') nav('/supervisor')
      else nav('/admin')
    } catch (e) {
      setErr(e.response?.data?.message || 'Credenciales inválidas')
    } finally { setLoading(false) }
  }

  const demo = (u, p) => { setForm({ username: u, password: p }) }

  return (
    <div className="grid min-h-screen place-items-center bg-gradient-to-br from-slate-950 via-blue-900 to-cyan-800 p-4">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-center gap-3 text-white">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 backdrop-blur overflow-hidden border border-white/10">
            <img src="/icono.png" alt="Logo" className="h-8 w-8 object-contain" />
          </div>
          <div>
            <h1 className="text-xl font-bold leading-tight">Ventas Telco</h1>
            <p className="text-sm text-blue-200">Sistema de Ventas · Fija Hogar</p>
          </div>
        </div>

        <Card className="border-0 shadow-2xl">
          <CardHeader>
            <CardTitle className="text-xl">Iniciar sesión</CardTitle>
            <CardDescription>Ingresa tus credenciales para continuar</CardDescription>
          </CardHeader>
          <CardContent>
            {err && (
              <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {err}
              </div>
            )}
            <form onSubmit={submit} className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="username">Usuario</Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="username"
                    className="pl-9"
                    value={form.username}
                    onChange={e => setForm({ ...form, username: e.target.value })}
                    placeholder="admin / agente1 / back1 / supervisor1"
                    autoComplete="username"
                    required
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="password">Contraseña</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="password"
                    type="password"
                    className="pl-9"
                    value={form.password}
                    onChange={e => setForm({ ...form, password: e.target.value })}
                    placeholder="••••••••••"
                    autoComplete="current-password"
                    required
                  />
                </div>
              </div>
              <Button type="submit" className="w-full" size="lg" disabled={loading}>
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                {loading ? 'Entrando...' : 'Ingresar'}
              </Button>
            </form>

            <div className="mt-5 rounded-lg border border-dashed p-3 text-xs text-muted-foreground">
              <p className="mb-2 font-semibold text-foreground">Cuentas demo (clic para autocompletar)</p>
              <div className="grid grid-cols-2 gap-1.5">
                <button type="button" className="text-left hover:text-blue-600" onClick={() => demo('admin', 'Admin*123')}>admin → ADMIN</button>
                <button type="button" className="text-left hover:text-blue-600" onClick={() => demo('supervisor1', 'Sup*123')}>supervisor1 → SUPERVISOR</button>
                <button type="button" className="text-left hover:text-blue-600" onClick={() => demo('back1', 'Back*123')}>back1 → BACKOFFICE</button>
                <button type="button" className="text-left hover:text-blue-600" onClick={() => demo('agente1', 'Agente*123')}>agente1 → AGENTE</button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
