import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  return (
    <nav>
      <h1 className="flex items-center gap-2">
        <img src="/icono.png" alt="Logo" className="h-6 w-6 inline-block object-contain" />
        Ventas Telco Fija Hogar
      </h1>
      <div className="nav-right">
        <span>{user.username}</span>
        <span className={`badge badge-${user.rol}`}>{user.rol}</span>
        <button className="ghost" onClick={logout}>Cerrar sesión</button>
      </div>
    </nav>
  )
}
