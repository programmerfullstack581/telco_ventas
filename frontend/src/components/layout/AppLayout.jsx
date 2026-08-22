import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  ClipboardCheck,
  FileBarChart2,
  LayoutDashboard,
  LogOut,
  Menu,
  PhoneCall,
  ShieldCheck
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { cn } from '@/lib/utils'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'
import { Separator } from '@/components/ui/separator'

const NAV_BY_ROL = {
  AGENTE: [
    { to: '/agente', label: 'Mis Ventas', icon: ClipboardCheck }
  ],
  BACKOFFICE: [
    { to: '/backoffice', label: 'Validaciones', icon: ClipboardCheck }
  ],
  SUPERVISOR: [
    { to: '/supervisor', label: 'Reportes', icon: FileBarChart2 }
  ],
  ADMIN: [
    { to: '/supervisor', label: 'Reportes', icon: FileBarChart2 },
    { to: '/admin', label: 'Panel Admin', icon: ShieldCheck }
  ]
}

const TITLES = {
  '/agente': 'Registro de Ventas',
  '/backoffice': 'Validación de Ventas',
  '/supervisor': 'Reportes del Equipo',
  '/admin': 'Panel de Administración'
}

const ROL_COLOR = {
  AGENTE: 'info',
  BACKOFFICE: 'secondary',
  SUPERVISOR: 'warning',
  ADMIN: 'destructive'
}

export default function AppLayout() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const location = useLocation()
  const items = NAV_BY_ROL[user?.rol] || []

  const initials = (user?.username || '?').slice(0, 2).toUpperCase()
  const title = TITLES[location.pathname] || 'Ventas Telco'

  const sidebar = (
    <div className="flex h-full flex-col bg-slate-950 text-slate-200">
      <div className="flex items-center gap-3 px-5 py-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-900 overflow-hidden border border-slate-700/50">
          <img src="/icono.png" alt="Logo" className="h-6 w-6 object-contain" />
        </div>
        <div>
          <p className="text-sm font-bold text-white leading-tight">Ventas Telco</p>
          <p className="text-[11px] text-slate-400">Fija Hogar</p>
        </div>
      </div>
      <Separator className="bg-slate-800" />
      <nav className="flex-1 space-y-1 px-3 py-4">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={() => setOpen(false)}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              )
            }
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-slate-800 px-3 py-4">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="w-full justify-start gap-3 px-2 text-slate-200 hover:bg-slate-800 hover:text-white">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-blue-600 text-white text-xs">{initials}</AvatarFallback>
              </Avatar>
              <span className="flex flex-col items-start text-left">
                <span className="text-sm font-semibold">{user?.username}</span>
                <span className="text-[11px] text-slate-400">{user?.rol}</span>
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" side="top" className="w-56">
            <DropdownMenuLabel>
              <div className="flex items-center justify-between">
                <span>{user?.username}</span>
                <Badge variant={ROL_COLOR[user?.rol] || 'default'}>{user?.rol}</Badge>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={logout} className="text-destructive focus:text-destructive">
              <LogOut className="h-4 w-4" />
              Cerrar sesión
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen">
      <div className="hidden md:fixed md:inset-y-0 md:left-0 md:block md:w-64">{sidebar}</div>

      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/60" onClick={() => setOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-64 shadow-xl">{sidebar}</div>
        </div>
      )}

      <div className="md:pl-64">
        <header className="sticky top-0 z-30 flex h-14 items-center gap-4 border-b bg-white/90 px-4 backdrop-blur">
          <Button variant="ghost" size="icon" className="md:hidden" onClick={() => setOpen(true)}>
            <Menu className="h-5 w-5" />
          </Button>
          <div className="flex items-center gap-2 text-slate-400">
            <LayoutDashboard className="h-4 w-4" />
            <span className="hidden sm:inline text-sm">{title}</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <span className="hidden sm:inline text-sm text-muted-foreground">
              {new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' })}
            </span>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button className="flex items-center gap-2 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-blue-600 text-white text-xs">{initials}</AvatarFallback>
                  </Avatar>
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div className="flex items-center justify-between">
                    <span>{user?.username}</span>
                    <Badge variant={ROL_COLOR[user?.rol] || 'default'}>{user?.rol}</Badge>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={logout} className="text-destructive focus:text-destructive">
                  <LogOut className="h-4 w-4" />
                  Cerrar sesión
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        <main className="p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
