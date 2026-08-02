import { useState } from 'react'
import { Download, Layers } from 'lucide-react'

// Página placeholder — se implementa en siguiente iteración con AI integration
export default function FlashcardsPage() {
  const [flashcards] = useState<{ front: string; back: string }[]>([])

  const exportToAnki = () => {
    const content = flashcards.map((f) => `${f.front};${f.back}`).join('\n')
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `flashcards-medprep-${new Date().toISOString().slice(0, 10)}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Flashcards</h1>
          <p className="text-muted-foreground">Generadas automáticamente por IA — exporta a Anki con un clic</p>
        </div>
        <button
          onClick={exportToAnki}
          disabled={flashcards.length === 0}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
        >
          <Download className="h-4 w-4" />
          Exportar a Anki (.txt)
        </button>
      </div>

      <div className="flex flex-col items-center gap-4 rounded-xl border border-dashed p-16 text-center">
        <Layers className="h-12 w-12 text-muted-foreground" />
        <p className="font-medium">Tus flashcards aparecerán aquí</p>
        <p className="max-w-sm text-sm text-muted-foreground">
          Cuando respondas una pregunta en el Banco de Preguntas, pulsa "Generar Flashcard"
          y se añadirá automáticamente a esta biblioteca.
        </p>
      </div>
    </div>
  )
}
