import { ShieldCheck } from 'lucide-react'

// Panel de administración — implementación completa en siguiente iteración
export default function AdminPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Administración</h1>
        <p className="text-muted-foreground">Gestión de contenido y usuarios</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {[
          { title: 'Cargar preguntas', desc: 'Subir banco de preguntas al sistema' },
          { title: 'Gestionar academias', desc: 'PDFs y materiales de las 4 academias' },
          { title: 'Usuarios', desc: 'Ver y gestionar cuentas de estudiantes' },
        ].map(({ title, desc }) => (
          <div key={title} className="flex flex-col gap-2 rounded-xl border bg-card p-6 shadow-sm">
            <ShieldCheck className="h-5 w-5 text-primary" />
            <h3 className="font-semibold">{title}</h3>
            <p className="text-sm text-muted-foreground">{desc}</p>
            <button className="mt-2 rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium hover:bg-accent">
              Próximamente
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
