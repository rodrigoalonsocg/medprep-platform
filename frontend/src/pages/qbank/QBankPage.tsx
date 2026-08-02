import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { questionService } from '@/services/question.service'
import type { Question, AttemptResponse } from '@/types'
import { CheckCircle2, XCircle, BookOpen, Shuffle } from 'lucide-react'
import { cn } from '@/lib/utils'

export default function QBankPage() {
  const [mode, setMode] = useState<'browse' | 'simulacro'>('browse')
  const [simulacroQuestions, setSimulacroQuestions] = useState<Question[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [lastAttempt, setLastAttempt] = useState<AttemptResponse | null>(null)

  const { data: questions, isLoading } = useQuery({
    queryKey: ['questions'],
    queryFn: () => questionService.list({ size: 20 }),
    enabled: mode === 'browse',
  })

  const startSimulacro = useQuery({
    queryKey: ['simulacro'],
    queryFn: () => questionService.generateSimulacro(undefined, 10),
    enabled: false,
  })

  const attemptMutation = useMutation({
    mutationFn: ({ questionId, option }: { questionId: string; option: string }) =>
      questionService.submitAttempt(questionId, option),
    onSuccess: (data) => setLastAttempt(data),
  })

  const handleStartSimulacro = async () => {
    const result = await startSimulacro.refetch()
    if (result.data) {
      setSimulacroQuestions(result.data)
      setCurrentIndex(0)
      setLastAttempt(null)
      setMode('simulacro')
    }
  }

  const handleAnswer = (option: string) => {
    if (lastAttempt) return
    const question = simulacroQuestions[currentIndex]
    attemptMutation.mutate({ questionId: question.id, option })
  }

  const handleNext = () => {
    if (currentIndex < simulacroQuestions.length - 1) {
      setCurrentIndex((i) => i + 1)
      setLastAttempt(null)
    } else {
      setMode('browse')
      setSimulacroQuestions([])
    }
  }

  if (mode === 'simulacro' && simulacroQuestions.length > 0) {
    const q = simulacroQuestions[currentIndex]
    const options = [
      { key: 'A', text: q.optionA },
      { key: 'B', text: q.optionB },
      { key: 'C', text: q.optionC },
      { key: 'D', text: q.optionD },
      { key: 'E', text: q.optionE },
    ].filter((o) => o.text)

    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">Simulacro</h1>
          <span className="text-sm text-muted-foreground">
            {currentIndex + 1} / {simulacroQuestions.length}
          </span>
        </div>

        <div className="rounded-lg border bg-card p-6 shadow-sm">
          <p className="mb-2 text-xs font-medium uppercase text-muted-foreground">
            {q.specialtyName} {q.subspecialtyName ? `• ${q.subspecialtyName}` : ''}
          </p>
          <p className="text-sm leading-relaxed">{q.stem}</p>
        </div>

        <div className="space-y-3">
          {options.map(({ key, text }) => {
            const isSelected = lastAttempt?.selectedOption === key
            const isCorrect = lastAttempt?.correctOption === key
            return (
              <button
                key={key}
                onClick={() => handleAnswer(key)}
                disabled={!!lastAttempt}
                className={cn(
                  'flex w-full items-start gap-3 rounded-lg border p-4 text-left text-sm transition-colors',
                  !lastAttempt && 'hover:border-primary hover:bg-accent',
                  lastAttempt && isCorrect && 'border-green-500 bg-green-50 text-green-800',
                  lastAttempt && isSelected && !isCorrect && 'border-red-500 bg-red-50 text-red-800',
                  lastAttempt && !isSelected && !isCorrect && 'opacity-60',
                )}
              >
                <span className="font-bold">{key}.</span>
                <span>{text}</span>
              </button>
            )
          })}
        </div>

        {lastAttempt && (
          <div className={cn(
            'rounded-lg p-4',
            lastAttempt.correct ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800',
          )}>
            <div className="mb-2 flex items-center gap-2 font-semibold">
              {lastAttempt.correct
                ? <><CheckCircle2 className="h-4 w-4" /> ¡Correcto!</>
                : <><XCircle className="h-4 w-4" /> Incorrecto — Respuesta: {lastAttempt.correctOption}</>
              }
            </div>
            {lastAttempt.explanation && (
              <p className="text-sm">{lastAttempt.explanation}</p>
            )}
            <button
              onClick={handleNext}
              className="mt-3 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            >
              {currentIndex < simulacroQuestions.length - 1 ? 'Siguiente →' : 'Finalizar simulacro'}
            </button>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Banco de Preguntas</h1>
          <p className="text-muted-foreground">Practica por especialidad o genera un simulacro</p>
        </div>
        <button
          onClick={handleStartSimulacro}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
        >
          <Shuffle className="h-4 w-4" />
          Generar simulacro
        </button>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-20 animate-pulse rounded-lg bg-muted" />
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {questions?.content.map((q) => (
            <div key={q.id} className="rounded-lg border bg-card p-4">
              <div className="mb-2 flex items-center gap-2">
                <span className="rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">{q.specialtyName}</span>
                <span className={cn(
                  'rounded-full px-2 py-0.5 text-xs font-medium',
                  q.difficulty === 'BAJA' ? 'bg-green-100 text-green-800' :
                  q.difficulty === 'MEDIA' ? 'bg-yellow-100 text-yellow-800' :
                  'bg-red-100 text-red-800',
                )}>{q.difficulty}</span>
              </div>
              <p className="text-sm">{q.stem.slice(0, 200)}...</p>
            </div>
          ))}
          {questions?.content.length === 0 && (
            <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed p-10 text-center">
              <BookOpen className="h-10 w-10 text-muted-foreground" />
              <p className="font-medium">No hay preguntas disponibles</p>
              <p className="text-sm text-muted-foreground">El administrador debe cargar el banco de preguntas.</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
