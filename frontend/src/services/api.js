import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('telco_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('telco_token')
      localStorage.removeItem('telco_user')
      if (!window.location.href.includes('/login')) window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api
