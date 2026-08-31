import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Login from './pages/Login'
import AgenteDashboard from './pages/AgenteDashboard'
import BackofficeDashboard from './pages/BackofficeDashboard'
import SupervisorDashboard from './pages/SupervisorDashboard'
import AdminDashboard from './pages/AdminDashboard'
import AppLayout from './components/layout/AppLayout'

function PrivateRoute({ children, roles }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="grid min-h-screen place-items-center text-muted-foreground">Cargando...</div>
  if (!user) return <Navigate to="/login" replace />
  const userRol = (user.rol || '').toUpperCase().replace('ROLE_', '')
  if (roles && !roles.map(r => r.toUpperCase()).includes(userRol)) {
    return <Navigate to="/" replace />
  }
  return children
}

function RoleRedirect() {
  const { user, loading } = useAuth()
  if (loading) return <div className="grid min-h-screen place-items-center text-muted-foreground">Cargando...</div>
  if (!user) return <Navigate to="/login" replace />
  const r = (user.rol || '').toUpperCase().replace('ROLE_', '')
  if (r === 'AGENTE') return <Navigate to="/agente" replace />
  if (r === 'BACKOFFICE') return <Navigate to="/backoffice" replace />
  if (r === 'SUPERVISOR') return <Navigate to="/supervisor" replace />
  if (r === 'ADMIN' || r.includes('ADMIN')) return <Navigate to="/admin" replace />
  return <Navigate to="/admin" replace />
}

export default function App() {
  const { user } = useAuth()
  return (
    <div className="min-h-screen">
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<RoleRedirect />} />

        <Route element={<AppLayout />}>
          <Route path="/agente" element={<PrivateRoute roles={['AGENTE']}><AgenteDashboard /></PrivateRoute>} />
          <Route path="/backoffice" element={<PrivateRoute roles={['BACKOFFICE']}><BackofficeDashboard /></PrivateRoute>} />
          <Route path="/supervisor" element={<PrivateRoute roles={['SUPERVISOR', 'ADMIN']}><SupervisorDashboard /></PrivateRoute>} />
          <Route path="/admin" element={<PrivateRoute roles={['ADMIN']}><AdminDashboard /></PrivateRoute>} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
