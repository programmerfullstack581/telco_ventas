import Swal from 'sweetalert2'

export const confirmar = ({ titulo, texto, icon = 'warning', confirmarTexto = 'Sí, continuar', cancelarTexto = 'Cancelar', danger = false }) =>
  Swal.fire({
    title: titulo,
    text: texto,
    icon,
    showCancelButton: true,
    confirmButtonText: confirmarTexto,
    cancelButtonText: cancelarTexto,
    confirmButtonColor: danger ? '#dc2626' : '#2563eb',
    cancelButtonColor: '#6b7280',
    reverseButtons: true
  })

export const exito = (titulo, texto) =>
  Swal.fire({ icon: 'success', title: titulo, text: texto, timer: 2200, showConfirmButton: false, timerProgressBar: true })

export const error = (titulo, texto) =>
  Swal.fire({ icon: 'error', title: titulo, text: texto })

export const pedirTexto = ({ titulo, texto, placeholder = '', valorInicial = '', validar = () => true }) =>
  Swal.fire({
    title: titulo,
    text: texto,
    input: 'text',
    inputPlaceholder: placeholder,
    inputValue: valorInicial,
    showCancelButton: true,
    confirmButtonText: 'Confirmar',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#2563eb',
    cancelButtonColor: '#6b7280',
    inputValidator: (v) => {
      if (!v || !validar(v)) return 'Valor inválido'
    }
  })
