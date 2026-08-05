import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { GraduationCap, FileText, Download } from 'lucide-react'
import { academyService } from '@/services/academy.service'

export default function AcademiesPage() {
  const [selected, setSelected] = useState<string | null>(null)

  const { data: academies, isLoading } = useQuery({
    queryKey: ['academies'],
    queryFn: academyService.list,
  })

  const { data: documents } = useQuery({
    queryKey: ['documents', selected],
    queryFn: () => academyService.listDocuments(selected!),
    enabled: !!selected,
  })

  const download = async (documentId: string) => {
    const url = await academyService.getDownloadUrl(documentId)
    window.open(url, '_blank')
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Academias</h1>
        <p className="text-muted-foreground">Material de estudio de las academias</p>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[...Array(3)].map((_, i) => <div key={i} className="h-32 animate-pulse rounded-xl bg-muted" />)}
        </div>
      ) : (academies?.length ?? 0) === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed p-12 text-center">
          <GraduationCap className="h-10 w-10 text-muted-foreground" />
          <p className="font-medium">No hay academias registradas</p>
          <p className="text-sm text-muted-foreground">El administrador puede crearlas desde el panel de administración.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {academies!.map((a) => (
            <button
              key={a.id}
              onClick={() => setSelected(a.id)}
              className={`flex flex-col items-start gap-2 rounded-xl border p-6 text-left shadow-sm transition-colors ${
                selected === a.id ? 'border-primary bg-accent' : 'bg-card hover:border-primary'
              }`}
            >
              <GraduationCap className="h-6 w-6 text-primary" />
              <h3 className="font-semibold">{a.name}</h3>
              {a.description && <p className="text-sm text-muted-foreground">{a.description}</p>}
            </button>
          ))}
        </div>
      )}

      {selected && (
        <div className="rounded-xl border bg-card p-6">
          <h2 className="mb-4 font-semibold">Documentos</h2>
          {(documents?.length ?? 0) === 0 ? (
            <p className="text-sm text-muted-foreground">Esta academia aún no tiene documentos.</p>
          ) : (
            <ul className="divide-y">
              {documents!.map((d) => (
                <li key={d.id} className="flex items-center justify-between py-3">
                  <span className="flex items-center gap-2 text-sm">
                    <FileText className="h-4 w-4 text-muted-foreground" />
                    {d.fileName}
                  </span>
                  <button
                    onClick={() => download(d.id)}
                    className="flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-medium hover:bg-accent"
                  >
                    <Download className="h-3.5 w-3.5" /> Descargar
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
