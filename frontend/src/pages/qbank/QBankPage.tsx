import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { questionService } from '@/services/question.service'
import { specialtyService } from '@/services/specialty.service'
import { aiService } from '@/services/ai.service'
import type { Question, AttemptResponse, PatternDTO } from '@/types'
import { CheckCircle2, XCircle, BookOpen, Shuffle, Sparkles, Brain, Filter, Trash2 } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { cn } from '@/lib/utils'

type Mode = 'browse' | 'errores' | 'simulacro'

export default function QBankPage() {
  const [mode, setMode] = useState<Mode>('browse')
  const [simulacroQuestions, setSimulacroQuestions] = useState<Question[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [lastAttempt, setLastAttempt] = useState<AttemptResponse | null>(null)
  const [flashMsg, setFlashMsg] = useState<string | null>(null)
  const queryClient = useQueryClient()
  const [filters, setFilters] = useState<{ specialtyId?: string; academy?: string; difficulty?: string }>({})
  const [page, setPage] = useState(0)

  const { data: questions, isLoading } = useQuery({
    queryKey: ['questions', filters, page],
    queryFn: () => questionService.list({ size: 5, page, ...filters }),
    enabled: mode === 'browse',
  })

  const { data: specialties } = useQuery({ queryKey: ['specialties'], queryFn: specialtyService.list })
  const { data: academies } = useQuery({ queryKey: ['academies-list'], queryFn: questionService.getAcademies })

  const setFilter = (k: 'specialtyId' | 'academy' | 'difficulty', v: string) => {
    setFilters((f) => ({ ...f, [k]: v || undefined }))
    setPage(0)
  }

  const { data: errorQuestions, isLoading: loadingErrors } = useQuery({
    queryKey: ['questions', 'errors'],
    queryFn: () => questionService.getErrorQuestions(),
    enabled: mode === 'errores',
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

  const flashcardMutation = useMutation({
    mutationFn: (questionId: string) => aiService.generateFlashcards(questionId),
    onSuccess: (msg) => {
      setFlashMsg(`✓ ${msg}`)
      // Refresca la biblioteca cuando la generación en segundo plano ya terminó.
      setTimeout(() => queryClient.invalidateQueries({ queryKey: ['flashcards'] }), 9000)
    },
    onError: () => setFlashMsg('No se pudo iniciar la generación (revisa la API de IA)'),
  })

  const handleStartSimulacro = async () => {
    const result = await startSimulacro.refetch()
    if (result.data) {
      setSimulacroQuestions(result.data)
      setCurrentIndex(0)
      setLastAttempt(null)
      setFlashMsg(null)
      setMode('simulacro')
    }
  }

  const handleAnswer = (option: string) => {
    if (lastAttempt) return
    const question = simulacroQuestions[currentIndex]
    attemptMutation.mutate({ questionId: question.id, option })
  }

  const handleNext = () => {
    setFlashMsg(null)
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
                  lastAttempt && isCorrect && 'border-green-500 bg-green-50 text-green-800 dark:bg-green-950/40 dark:text-green-300',
                  lastAttempt && isSelected && !isCorrect && 'border-red-500 bg-red-50 text-red-800 dark:bg-red-950/40 dark:text-red-300',
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
            lastAttempt.correct ? 'bg-green-50 text-green-800 dark:bg-green-950/40 dark:text-green-300' : 'bg-red-50 text-red-800 dark:bg-red-950/40 dark:text-red-300',
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
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                onClick={() => flashcardMutation.mutate(q.id)}
                disabled={flashcardMutation.isPending}
                className="flex items-center gap-2 rounded-lg border border-primary bg-card px-4 py-2 text-sm font-medium text-primary disabled:opacity-50"
              >
                <Sparkles className="h-4 w-4" />
                {flashcardMutation.isPending ? 'Generando...' : 'Generar Flashcard (IA)'}
              </button>
              <button
                onClick={handleNext}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
              >
                {currentIndex < simulacroQuestions.length - 1 ? 'Siguiente →' : 'Finalizar simulacro'}
              </button>
            </div>
            {flashMsg && <p className="mt-2 text-xs">{flashMsg}</p>}
          </div>
        )}
      </div>
    )
  }

  const list = mode === 'errores' ? errorQuestions : questions
  const listLoading = mode === 'errores' ? loadingErrors : isLoading

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Banco de Preguntas</h1>
          <p className="text-muted-foreground">Practica, repasa tus errores o genera un simulacro</p>
        </div>
        <button
          onClick={handleStartSimulacro}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
        >
          <Shuffle className="h-4 w-4" />
          Generar simulacro
        </button>
      </div>

      <div className="flex gap-2 border-b">
        <TabButton active={mode === 'browse'} onClick={() => setMode('browse')} icon={BookOpen} label="Explorar" />
        <TabButton active={mode === 'errores'} onClick={() => setMode('errores')} icon={Filter} label="Filtro de Erre" />
      </div>

      {mode === 'browse' && (
        <div className="flex flex-wrap gap-2">
          <select value={filters.specialtyId ?? ''} onChange={(e) => setFilter('specialtyId', e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            <option value="">Todas las especialidades</option>
            {specialties?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <select value={filters.academy ?? ''} onChange={(e) => setFilter('academy', e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            <option value="">Todas las academias</option>
            {academies?.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
          <select value={filters.difficulty ?? ''} onChange={(e) => setFilter('difficulty', e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
            <option value="">Toda complejidad</option>
            <option value="BAJA">Baja</option>
            <option value="MEDIA">Media</option>
            <option value="ALTA">Alta</option>
          </select>
        </div>
      )}

      {mode === 'errores' && (
        <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
          Preguntas que fallaste o marcaste como dudosas en las últimas 3 semanas.
        </p>
      )}

      {listLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-20 animate-pulse rounded-lg bg-muted" />
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {list?.content.map((q) => (
            <QuestionCard key={q.id} question={q} />
          ))}
          {list?.content.length === 0 && (
            <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed p-10 text-center">
              <BookOpen className="h-10 w-10 text-muted-foreground" />
              <p className="font-medium">
                {mode === 'errores' ? 'Sin errores recientes' : 'No hay preguntas disponibles'}
              </p>
              <p className="text-sm text-muted-foreground">
                {mode === 'errores'
                  ? '¡Bien! No tienes preguntas falladas en las últimas 3 semanas.'
                  : 'El administrador debe cargar el banco de preguntas.'}
              </p>
            </div>
          )}
        </div>
      )}

      {mode === 'browse' && questions && questions.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="rounded-lg border px-3 py-1.5 text-sm disabled:opacity-40">Anterior</button>
          <span className="text-sm text-muted-foreground">Página {questions.number + 1} de {questions.totalPages}</span>
          <button disabled={page >= questions.totalPages - 1} onClick={() => setPage((p) => p + 1)} className="rounded-lg border px-3 py-1.5 text-sm disabled:opacity-40">Siguiente</button>
        </div>
      )}
    </div>
  )
}

function TabButton({ active, onClick, icon: Icon, label }: {
  active: boolean; onClick: () => void; icon: typeof BookOpen; label: string
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'flex items-center gap-2 border-b-2 px-3 py-2 text-sm font-medium transition-colors',
        active ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground',
      )}
    >
      <Icon className="h-4 w-4" />
      {label}
    </button>
  )
}

function QuestionCard({ question: q }: { question: Question }) {
  const [pattern, setPattern] = useState<PatternDTO | null>(null)
  const [confirming, setConfirming] = useState(false)
  const { isAdmin } = useAuthStore()
  const queryClient = useQueryClient()
  const analyzeMutation = useMutation({
    mutationFn: () => aiService.analyzePattern(q.id),
    onSuccess: (data) => setPattern(data),
  })
  const deleteMutation = useMutation({
    mutationFn: () => questionService.remove(q.id),
    onSuccess: () => { setConfirming(false); queryClient.invalidateQueries({ queryKey: ['questions'] }) },
  })

  return (
    <div className="rounded-lg border bg-card p-4">
      <div className="mb-2 flex items-start justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">{q.specialtyName}</span>
          {q.academy && <span className="rounded-full border px-2 py-0.5 text-xs font-medium">{q.academy}</span>}
          <span className={cn(
            'rounded-full px-2 py-0.5 text-xs font-medium',
            q.difficulty === 'BAJA' ? 'bg-green-100 text-green-800 dark:bg-green-950/50 dark:text-green-300' :
            q.difficulty === 'MEDIA' ? 'bg-yellow-100 text-yellow-800' :
            'bg-red-100 text-red-800 dark:bg-red-950/50 dark:text-red-300',
          )}>{q.difficulty}</span>
        </div>
        {isAdmin() && (
          <button
            onClick={() => setConfirming(true)}
            className="flex shrink-0 items-center gap-2 rounded-lg border px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Eliminar
          </button>
        )}
      </div>
      <p className="whitespace-pre-line text-sm">{q.stem}</p>

      {confirming && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setConfirming(false)}>
          <div className="w-full max-w-sm rounded-xl border bg-card p-5 shadow-lg" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-semibold">¿Eliminar esta pregunta?</h3>
            <p className="mt-1 text-sm text-muted-foreground">Esta acción no se puede deshacer.</p>
            <div className="mt-4 flex justify-end gap-2">
              <button onClick={() => setConfirming(false)} className="rounded-lg border px-3 py-1.5 text-sm">Cancelar</button>
              <button
                onClick={() => deleteMutation.mutate()}
                disabled={deleteMutation.isPending}
                className="rounded-lg bg-destructive px-3 py-1.5 text-sm font-medium text-destructive-foreground disabled:opacity-50"
              >
                {deleteMutation.isPending ? 'Eliminando…' : 'Eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}

      <button
        onClick={() => analyzeMutation.mutate()}
        disabled={analyzeMutation.isPending || !!pattern}
        className="mt-3 flex items-center gap-2 rounded-lg border px-3 py-1.5 text-xs font-medium text-primary hover:bg-accent disabled:opacity-50"
      >
        <Brain className="h-3.5 w-3.5" />
        {analyzeMutation.isPending ? 'Analizando...' : pattern ? 'Patrón analizado' : 'Analizar patrón (IA)'}
      </button>

      {analyzeMutation.isError && (
        <p className="mt-2 text-xs text-red-600">No se pudo analizar (revisa la API de IA).</p>
      )}

      {pattern && (
        <div className="mt-3 space-y-2 rounded-lg bg-muted/50 p-3 text-xs">
          <div className="flex flex-wrap gap-1">
            {pattern.keywords.map((k) => (
              <span key={k} className="rounded-full bg-primary/10 px-2 py-0.5 text-primary">{k}</span>
            ))}
          </div>
          {pattern.diagnosis && <p><span className="font-semibold">Diagnóstico:</span> {pattern.diagnosis}</p>}
          {pattern.pearl && <p><span className="font-semibold">Perla:</span> {pattern.pearl}</p>}
          {pattern.distractors?.length > 0 && (
            <p><span className="font-semibold">Distractores:</span> {pattern.distractors.join(', ')}</p>
          )}
        </div>
      )}
    </div>
  )
}
