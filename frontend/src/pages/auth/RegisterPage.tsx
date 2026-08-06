import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { supabase } from '@/lib/supabase'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', university: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    const { error } = await supabase.auth.signUp({
      email: form.email,
      password: form.password,
      options: {
        data: { full_name: form.fullName, university: form.university, role: 'student' },
      },
    })

    if (error) {
      setError(error.message)
    } else {
      navigate('/dashboard')
    }

    setLoading(false)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <div className="w-full max-w-md space-y-6 rounded-xl border bg-card p-6 shadow-sm sm:p-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-primary">MedPrep</h1>
          <p className="mt-1 text-sm text-muted-foreground">Crea tu cuenta de estudiante</p>
        </div>

        <form onSubmit={handleRegister} className="space-y-4">
          {[
            { name: 'fullName', label: 'Nombre completo', type: 'text', placeholder: 'Juan Pérez' },
            { name: 'email', label: 'Email', type: 'email', placeholder: 'tu@email.com' },
            { name: 'password', label: 'Contraseña', type: 'password', placeholder: '••••••••' },
            { name: 'university', label: 'Universidad', type: 'text', placeholder: 'UNMSM, UPCH...' },
          ].map(({ name, label, type, placeholder }) => (
            <div key={name} className="space-y-1">
              <label className="text-sm font-medium" htmlFor={name}>{label}</label>
              <input
                id={name}
                name={name}
                type={type}
                required={name !== 'university'}
                value={form[name as keyof typeof form]}
                onChange={handleChange}
                className="w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
                placeholder={placeholder}
              />
            </div>
          ))}

          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600 dark:bg-red-950/40 dark:text-red-300">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-primary py-2 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            {loading ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <p className="text-center text-sm text-muted-foreground">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Inicia sesión
          </Link>
        </p>
      </div>
    </div>
  )
}
