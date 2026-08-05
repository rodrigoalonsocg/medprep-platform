import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ShieldCheck, Plus, Upload } from 'lucide-react'
import { specialtyService } from '@/services/specialty.service'
import { questionService, type CreateQuestionPayload } from '@/services/question.service'
import { academyService } from '@/services/academy.service'
import type { Difficulty } from '@/types'
import { cn } from '@/lib/utils'

type Tab = 'question' | 'academies'

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>('question')

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Administración</h1>
        <p className="text-muted-foreground">Gestión de contenido</p>
      </div>

      <div className="flex gap-2 border-b">
        <button
          onClick={() => setTab('question')}
          className={cn('border-b-2 px-3 py-2 text-sm font-medium',
            tab === 'question' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground')}
        >Cargar pregunta</button>
        <button
          onClick={() => setTab('academies')}
          className={cn('border-b-2 px-3 py-2 text-sm font-medium',
            tab === 'academies' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground')}
        >Academias y documentos</button>
      </div>

      {tab === 'question' ? <CreateQuestionForm /> : <AcademiesManager />}
    </div>
  )
}

const emptyQuestion: CreateQuestionPayload = {
  specialtyId: '', stem: '', optionA: '', optionB: '', optionC: '', optionD: '', optionE: '',
  correctOption: 'A', explanation: '', difficulty: 'MEDIA', source: '', year: undefined,
}

function CreateQuestionForm() {
  const [form, setForm] = useState<CreateQuestionPayload>(emptyQuestion)
  const [msg, setMsg] = useState<string | null>(null)
  const { data: specialties } = useQuery({ queryKey: ['specialties'], queryFn: specialtyService.list })

  const mutation = useMutation({
    mutationFn: questionService.create,
    onSuccess: () => {
      setMsg('✓ Pregunta creada')
      setForm(emptyQuestion)
    },
    onError: () => setMsg('Error al crear la pregunta (¿eres admin?)'),
  })

  const set = (k: keyof CreateQuestionPayload, v: string | number | undefined) =>
    setForm((f) => ({ ...f, [k]: v }))

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setMsg(null)
    mutation.mutate({ ...form, year: form.year ? Number(form.year) : undefined })
  }

  const input = 'w-full rounded-lg border px-3 py-2 text-sm'

  return (
    <form onSubmit={submit} className="max-w-2xl space-y-4 rounded-xl border bg-card p-6 shadow-sm">
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="mb-1 block text-sm font-medium">Especialidad</label>
          <select required value={form.specialtyId} onChange={(e) => set('specialtyId', e.target.value)} className={input}>
            <option value="">Selecciona...</option>
            {specialties?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Dificultad</label>
          <select value={form.difficulty} onChange={(e) => set('difficulty', e.target.value as Difficulty)} className={input}>
            <option value="BAJA">Baja</option>
            <option value="MEDIA">Media</option>
            <option value="ALTA">Alta</option>
          </select>
        </div>
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium">Enunciado (caso clínico)</label>
        <textarea required value={form.stem} onChange={(e) => set('stem', e.target.value)} rows={4} className={input} />
      </div>

      {(['A', 'B', 'C', 'D', 'E'] as const).map((opt) => {
        const key = `option${opt}` as keyof CreateQuestionPayload
        return (
          <div key={opt}>
            <label className="mb-1 block text-sm font-medium">Opción {opt}{opt === 'A' || opt === 'B' ? ' *' : ''}</label>
            <input
              required={opt === 'A' || opt === 'B'}
              value={(form[key] as string) ?? ''}
              onChange={(e) => set(key, e.target.value)}
              className={input}
            />
          </div>
        )
      })}

      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="mb-1 block text-sm font-medium">Opción correcta</label>
          <select value={form.correctOption} onChange={(e) => set('correctOption', e.target.value)} className={input}>
            {['A', 'B', 'C', 'D', 'E'].map((o) => <option key={o} value={o}>{o}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Año</label>
          <input type="number" value={form.year ?? ''} onChange={(e) => set('year', e.target.value ? Number(e.target.value) : undefined)} className={input} />
        </div>
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium">Explicación</label>
        <textarea value={form.explanation} onChange={(e) => set('explanation', e.target.value)} rows={3} className={input} />
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium">Fuente (ej: ENAM 2023)</label>
        <input value={form.source} onChange={(e) => set('source', e.target.value)} className={input} />
      </div>

      {msg && <p className="text-sm">{msg}</p>}

      <button
        type="submit"
        disabled={mutation.isPending}
        className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
      >
        <Plus className="h-4 w-4" />
        {mutation.isPending ? 'Guardando...' : 'Crear pregunta'}
      </button>
    </form>
  )
}

function AcademiesManager() {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [uploadTarget, setUploadTarget] = useState<string>('')
  const [file, setFile] = useState<File | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const { data: academies } = useQuery({ queryKey: ['academies'], queryFn: academyService.list })

  const createMutation = useMutation({
    mutationFn: () => academyService.create({ name, description: description || undefined }),
    onSuccess: () => {
      setName(''); setDescription(''); setMsg('✓ Academia creada')
      queryClient.invalidateQueries({ queryKey: ['academies'] })
    },
    onError: () => setMsg('Error al crear la academia'),
  })

  const uploadMutation = useMutation({
    mutationFn: () => academyService.uploadDocument(uploadTarget, file!),
    onSuccess: () => {
      setFile(null); setMsg('✓ Documento subido')
      queryClient.invalidateQueries({ queryKey: ['documents', uploadTarget] })
    },
    onError: () => setMsg('Error al subir el documento'),
  })

  const input = 'w-full rounded-lg border px-3 py-2 text-sm'

  return (
    <div className="grid max-w-3xl gap-6 md:grid-cols-2">
      <div className="space-y-3 rounded-xl border bg-card p-6 shadow-sm">
        <h3 className="flex items-center gap-2 font-semibold"><ShieldCheck className="h-5 w-5 text-primary" /> Nueva academia</h3>
        <input placeholder="Nombre" value={name} onChange={(e) => setName(e.target.value)} className={input} />
        <textarea placeholder="Descripción (opcional)" value={description} onChange={(e) => setDescription(e.target.value)} rows={2} className={input} />
        <button
          onClick={() => { setMsg(null); createMutation.mutate() }}
          disabled={!name || createMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
        >
          <Plus className="h-4 w-4" /> Crear
        </button>
      </div>

      <div className="space-y-3 rounded-xl border bg-card p-6 shadow-sm">
        <h3 className="flex items-center gap-2 font-semibold"><Upload className="h-5 w-5 text-primary" /> Subir documento</h3>
        <select value={uploadTarget} onChange={(e) => setUploadTarget(e.target.value)} className={input}>
          <option value="">Selecciona academia...</option>
          {academies?.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
        </select>
        <input type="file" accept=".pdf,.doc,.docx,.txt" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="text-sm" />
        <button
          onClick={() => { setMsg(null); uploadMutation.mutate() }}
          disabled={!uploadTarget || !file || uploadMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
        >
          <Upload className="h-4 w-4" /> {uploadMutation.isPending ? 'Subiendo...' : 'Subir'}
        </button>
      </div>

      {msg && <p className="text-sm md:col-span-2">{msg}</p>}
    </div>
  )
}
