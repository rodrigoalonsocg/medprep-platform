import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Download, Layers, Eye, EyeOff } from 'lucide-react'
import { aiService } from '@/services/ai.service'
import type { FlashcardDTO } from '@/types'

export default function FlashcardsPage() {
  const { data: flashcards, isLoading } = useQuery({
    queryKey: ['flashcards'],
    queryFn: aiService.getFlashcards,
  })

  const exportToAnki = async () => {
    const blob = await aiService.exportToAnki()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `flashcards-medprep-${new Date().toISOString().slice(0, 10)}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  const hasCards = (flashcards?.length ?? 0) > 0

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Flashcards</h1>
          <p className="text-muted-foreground">Generadas por IA — exporta a Anki con un clic</p>
        </div>
        <button
          onClick={exportToAnki}
          disabled={!hasCards}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
        >
          <Download className="h-4 w-4" />
          Exportar a Anki (.txt)
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-lg bg-muted" />
          ))}
        </div>
      ) : hasCards ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {flashcards!.map((f) => <FlashcardCard key={f.id} f={f} />)}
        </div>
      ) : (
        <div className="flex flex-col items-center gap-4 rounded-xl border border-dashed p-16 text-center">
          <Layers className="h-12 w-12 text-muted-foreground" />
          <p className="font-medium">Tus flashcards aparecerán aquí</p>
          <p className="max-w-sm text-sm text-muted-foreground">
            En el Banco de Preguntas, tras responder pulsa "Generar Flashcard (IA)"
            y se añadirá automáticamente a esta biblioteca.
          </p>
        </div>
      )}
    </div>
  )
}

function FlashcardCard({ f }: { f: FlashcardDTO }) {
  const [show, setShow] = useState(false)
  return (
    <div className="rounded-lg border bg-card p-4 shadow-sm">
      <div className="mb-2 flex items-center justify-between gap-2">
        {f.specialtyName
          ? <span className="rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">{f.specialtyName}</span>
          : <span />}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShow((v) => !v)}
            className="flex items-center gap-1 rounded-lg border px-2 py-0.5 text-xs font-medium text-primary hover:bg-accent"
          >
            {show ? <><EyeOff className="h-3.5 w-3.5" /> Ocultar</> : <><Eye className="h-3.5 w-3.5" /> Ver respuesta</>}
          </button>
          {f.exported && <span className="text-xs text-muted-foreground">Exportada</span>}
        </div>
      </div>
      <p className="text-sm font-semibold">{f.front}</p>
      {show
        ? <p className="mt-1 text-sm text-muted-foreground">{f.back}</p>
        : <p className="mt-1 text-sm italic text-muted-foreground/60">Respuesta oculta — pulsa "Ver respuesta"</p>}
    </div>
  )
}
