import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Upload, Sparkles, Trash2, Loader2, FileText } from 'lucide-react'
import { questionService } from '@/services/question.service'
import type { Question } from '@/types'
import { cn } from '@/lib/utils'

export default function AdminPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Administración</h1>
        <p className="text-muted-foreground">Importa exámenes con IA para llenar el banco de preguntas</p>
      </div>
      <ExamImporter />
    </div>
  )
}

const difficultyBadge: Record<string, string> = {
  BAJA: 'bg-green-100 text-green-700',
  MEDIA: 'bg-amber-100 text-amber-700',
  ALTA: 'bg-red-100 text-red-700',
}

function ExamImporter() {
  const queryClient = useQueryClient()
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<{ imported: number; questions: Question[] } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const importMutation = useMutation({
    mutationFn: () => questionService.importExam(file!),
    onSuccess: (data) => {
      setResult(data)
      setError(null)
      setFile(null)
      queryClient.invalidateQueries({ queryKey: ['questions'] })
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Error al importar el examen. Revisa que sea un PDF con texto y respuestas.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => questionService.remove(id),
    onSuccess: (_d, id) => {
      setResult((r) => r ? { ...r, imported: r.imported - 1, questions: r.questions.filter((q) => q.id !== id) } : r)
    },
  })

  return (
    <div className="max-w-3xl space-y-6">
      <div className="space-y-4 rounded-xl border bg-card p-6 shadow-sm">
        <div className="flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h3 className="font-semibold">Importar examen con IA</h3>
            <p className="text-sm text-muted-foreground">
              Sube un PDF de examen (con las respuestas). La IA detecta cada pregunta, la clasifica por
              especialidad y la agrega al banco y a los simulacros.
            </p>
          </div>
        </div>

        <label className="flex cursor-pointer items-center gap-3 rounded-lg border border-dashed p-4 hover:bg-accent">
          <FileText className="h-5 w-5 text-muted-foreground" />
          <span className="text-sm">{file ? file.name : 'Selecciona un PDF de examen…'}</span>
          <input
            type="file"
            accept="application/pdf,.pdf"
            className="hidden"
            onChange={(e) => { setFile(e.target.files?.[0] ?? null); setError(null) }}
          />
        </label>

        <button
          onClick={() => { setError(null); importMutation.mutate() }}
          disabled={!file || importMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
        >
          {importMutation.isPending
            ? <><Loader2 className="h-4 w-4 animate-spin" /> Analizando con IA… (puede tardar ~1 min)</>
            : <><Upload className="h-4 w-4" /> Analizar e importar</>}
        </button>

        {error && <p className="text-sm text-destructive">{error}</p>}
      </div>

      {result && (
        <div className="space-y-3 rounded-xl border bg-card p-6 shadow-sm">
          <h3 className="font-semibold text-primary">
            ✓ {result.imported} pregunta{result.imported === 1 ? '' : 's'} importada{result.imported === 1 ? '' : 's'}
          </h3>
          <p className="text-sm text-muted-foreground">Revisa la clasificación. Si alguna quedó mal, elimínala con la papelera.</p>
          <ul className="divide-y">
            {result.questions.map((q) => (
              <li key={q.id} className="flex items-start justify-between gap-3 py-3">
                <div className="min-w-0">
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">{q.specialtyName}</span>
                    <span className={cn('rounded-full px-2 py-0.5 text-xs font-medium', difficultyBadge[q.difficulty] ?? 'bg-muted')}>{q.difficulty}</span>
                  </div>
                  <p className="truncate text-sm">{q.stem}</p>
                </div>
                <button
                  onClick={() => deleteMutation.mutate(q.id)}
                  disabled={deleteMutation.isPending}
                  className="shrink-0 rounded-lg p-2 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                  title="Eliminar pregunta"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
