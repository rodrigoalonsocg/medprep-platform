# MedPrep Platform — CLAUDE.md

> Documento vivo de arquitectura, decisiones técnicas y reglas del proyecto.
> Actualizar en cada cambio relevante de arquitectura o decisiones de diseño.

---

## Visión del Producto

Plataforma de estudio para estudiantes de medicina en Perú que se preparan para el examen de internado (EsSalud/ENAM). Permite gestionar material de múltiples academias, entrenar con bancos de preguntas inteligentes, monitorear progreso por especialidad y generar flashcards para Anki.

**Público objetivo**: Estudiantes de medicina (multi-usuario), con rol admin para gestión de contenido.

---

## Stack Tecnológico

| Capa | Tecnología | Versión | Hosting |
|------|-----------|---------|---------|
| Backend | Spring Boot | 3.3.x | Railway |
| Lenguaje backend | Java | 21 LTS | — |
| Build tool | Maven | 3.9.x | — |
| Frontend | React + TypeScript | 18 / 5.x | Vercel |
| Build frontend | Vite | 5.x | — |
| UI Components | shadcn/ui + Tailwind CSS | latest | — |
| Base de datos | Supabase (PostgreSQL 15) | — | Supabase Cloud |
| Auth | Supabase Auth (JWT) | — | Supabase Cloud |
| File Storage | Supabase Storage | — | Supabase Cloud |
| AI Layer | Adaptador intercambiable | — | — |
| State management | Zustand + TanStack Query | — | — |
| HTTP Client | Axios | — | — |
| Routing | React Router v6 | — | — |
| Icons | Lucide React | — | — |
| Testing backend | JUnit 5 + Mockito | — | — |
| Testing frontend | Vitest + Testing Library | — | — |
| API Docs | SpringDoc OpenAPI (Swagger) | — | — |

---

## Estructura de Directorios

```
medprep-platform/
├── CLAUDE.md                          ← Este archivo
├── .gitignore
├── .github/
│   └── workflows/
│       ├── backend-ci.yml
│       └── frontend-ci.yml
├── backend/                           ← Spring Boot API
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/medprep/
│       │   │   ├── MedPrepApplication.java
│       │   │   ├── config/            ← Spring config (Security, CORS, AI)
│       │   │   ├── controller/        ← REST Controllers (capa C del MVC)
│       │   │   ├── service/           ← Lógica de negocio (capa M del MVC)
│       │   │   ├── repository/        ← Data Access (JPA Repositories)
│       │   │   ├── model/             ← Entidades JPA
│       │   │   ├── dto/
│       │   │   │   ├── request/       ← DTOs de entrada (validados con @Valid)
│       │   │   │   └── response/      ← DTOs de salida
│       │   │   ├── mapper/            ← MapStruct mappers (Entity ↔ DTO)
│       │   │   ├── exception/         ← Excepciones custom + GlobalExceptionHandler
│       │   │   ├── ai/
│       │   │   │   ├── adapter/       ← AiAdapter interface
│       │   │   │   └── provider/      ← Implementaciones (OpenAI, Gemini, DeepSeek)
│       │   │   ├── security/          ← JWT filter, Supabase auth validation
│       │   │   └── util/              ← Helpers y constantes
│       │   └── resources/
│       │       ├── application.yml
│       │       └── application-prod.yml
│       └── test/java/com/medprep/
└── frontend/                          ← React App
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.ts
    ├── tsconfig.json
    └── src/
        ├── components/
        │   ├── ui/                    ← shadcn/ui components
        │   ├── layout/                ← Navbar, Sidebar, PageWrapper
        │   └── shared/               ← Componentes reutilizables
        ├── pages/
        │   ├── auth/                  ← Login, Register
        │   ├── dashboard/             ← Home / Dashboard principal
        │   ├── qbank/                 ← Banco de preguntas
        │   ├── flashcards/            ← Generador y exportador
        │   ├── progress/              ← Semáforo clínico / Analytics
        │   ├── workspace/             ← Pomodoro + Calendario
        │   └── admin/                 ← Panel de administración
        ├── hooks/                     ← Custom React hooks
        ├── services/                  ← API calls (axios)
        ├── store/                     ← Zustand stores
        ├── types/                     ← TypeScript interfaces
        └── lib/                       ← Utils, constantes, helpers
```

---

## Arquitectura MVC por Capas

### Backend (Spring Boot)

```
HTTP Request
    ↓
[Controller Layer]     — Valida entrada, delega al servicio, retorna DTO de respuesta
    ↓
[Service Layer]        — Lógica de negocio, orquestación, transacciones
    ↓
[Repository Layer]     — Acceso a datos vía Spring Data JPA + Supabase (PostgreSQL)
    ↓
[Model Layer]          — Entidades JPA mapeadas a tablas Supabase
    ↑
[DTO Layer]            — DTOs de request (validados) y response (serializados)
[Mapper Layer]         — MapStruct: Entity ↔ DTO (sin lógica manual)
[Exception Layer]      — Custom exceptions + @ControllerAdvice global
[AI Adapter Layer]     — Interfaz + implementaciones intercambiables de IA
[Security Layer]       — Validación JWT de Supabase Auth
```

### Frontend (React)

```
[Pages]           — Componentes de ruta, orquestan la vista
    ↓
[Hooks]           — useQuery/useMutation (TanStack), lógica de estado local
    ↓
[Services]        — Llamadas HTTP con Axios (un servicio por módulo)
    ↓
[Store (Zustand)] — Estado global (user session, UI state)
    ↓
[Components]      — UI atómica y reutilizable (shadcn/ui base)
```

---

## Módulos de la Aplicación

### Módulo 1: Autenticación y Usuarios
- Supabase Auth (email/password, OAuth futuro)
- Roles: `admin` | `student`
- JWT validado en cada request por filtro Spring Security

### Módulo 2: Gestor de Conocimiento (4 Academias)
- Admin sube PDFs por academia → Supabase Storage
- Usuarios suben sus propios documentos extras
- AI indexa y permite consultas cruzadas entre academias
- Alerta de contradicción entre academias sobre un mismo tema

### Módulo 3: Q-Bank (Banco de Preguntas)
- CRUD de preguntas con 4 tags obligatorios:
  `especialidad` | `subespecialidad` | `dificultad` | `estado`
- Filtro de Erre: cuestionarios solo con preguntas incorrectas/dudosas
- Simulacros aleatorios por filtro de especialidad/dificultad
- Registro de intentos por usuario

### Módulo 4: Semáforo Clínico (Analytics)
- Dashboard por especialidad con % de aciertos
- Lógica semáforo: Verde >75% | Amarillo 60-75% | Rojo <60%
- Sprint de Recuperación automático cuando una especialidad cae a rojo
- Programación de repasos por rendimiento

### Módulo 5: Extractor de Patrones y Keywords
- Modo Patrón: IA analiza enunciado clínico → extrae keywords pivote y distractores
- Perlas médicas asociadas a cada patrón
- Biblioteca de patrones por especialidad

### Módulo 6: Fábrica de Flashcards (Exportación Anki)
- Botón "Generar Flashcard" en cada pregunta o resumen
- Formato de exportación: `Pregunta;Respuesta` (compatible con importación Anki)
- Descarga semanal de `.txt` para importar a Anki
- Biblioteca de flashcards del usuario

### Módulo 7: Workspace de Productividad
- Temporizador Pomodoro (50 min estudio / 10 min descanso)
- Calendario de bloques de estudio por especialidad
- Registro de sesiones de estudio

---

## Esquema de Base de Datos (Supabase/PostgreSQL)

```sql
-- Gestionada por Supabase Auth
auth.users (id uuid PK, email, created_at)

-- Perfiles extendidos
user_profiles (
  id uuid PK FK auth.users,
  full_name text,
  role text CHECK IN ('admin', 'student'),
  university text,
  created_at timestamptz
)

-- Academias
academies (
  id uuid PK,
  name text NOT NULL,
  description text,
  created_by uuid FK user_profiles,
  created_at timestamptz
)

-- Documentos / PDFs
documents (
  id uuid PK,
  academy_id uuid FK academies,
  uploaded_by uuid FK user_profiles,
  file_name text,
  storage_path text,           -- ruta en Supabase Storage
  file_size_bytes bigint,
  is_public boolean DEFAULT false,
  created_at timestamptz
)

-- Especialidades
specialties (
  id uuid PK,
  name text NOT NULL,          -- Ej: 'Medicina Interna'
  code text UNIQUE             -- Ej: 'MED_INT'
)

-- Subespecialidades
subspecialties (
  id uuid PK,
  specialty_id uuid FK specialties,
  name text NOT NULL,          -- Ej: 'Gastroenterología'
  code text UNIQUE
)

-- Banco de preguntas
questions (
  id uuid PK,
  specialty_id uuid FK specialties,
  subspecialty_id uuid FK subspecialties,
  stem text NOT NULL,          -- Enunciado del caso clínico
  option_a text,
  option_b text,
  option_c text,
  option_d text,
  option_e text,
  correct_option char(1),      -- 'A'|'B'|'C'|'D'|'E'
  explanation text,
  difficulty text CHECK IN ('BAJA', 'MEDIA', 'ALTA'),
  source text,                 -- Ej: 'ENAM 2023', 'EsSalud 2022'
  year int,
  keywords text[],             -- Array de keywords extraídas por IA
  created_by uuid FK user_profiles,
  created_at timestamptz
)

-- Intentos de preguntas por usuario
question_attempts (
  id uuid PK,
  user_id uuid FK user_profiles,
  question_id uuid FK questions,
  selected_option char(1),
  is_correct boolean,
  time_spent_seconds int,
  status text CHECK IN ('CORRECTA', 'INCORRECTA', 'DUDOSA'),
  attempted_at timestamptz
)

-- Progreso por especialidad (calculado / cache)
user_specialty_progress (
  id uuid PK,
  user_id uuid FK user_profiles,
  specialty_id uuid FK specialties,
  total_attempts int DEFAULT 0,
  correct_attempts int DEFAULT 0,
  accuracy_percentage numeric(5,2),
  traffic_light text CHECK IN ('VERDE', 'AMARILLO', 'ROJO'),
  last_updated timestamptz
)

-- Flashcards
flashcards (
  id uuid PK,
  user_id uuid FK user_profiles,
  question_id uuid FK questions NULL,  -- puede ser de pregunta o manual
  front text NOT NULL,
  back text NOT NULL,
  specialty_id uuid FK specialties,
  is_exported boolean DEFAULT false,
  created_at timestamptz
)

-- Sesiones de estudio (Pomodoro)
study_sessions (
  id uuid PK,
  user_id uuid FK user_profiles,
  specialty_id uuid FK specialties NULL,
  duration_minutes int,
  session_type text CHECK IN ('POMODORO', 'LIBRE'),
  started_at timestamptz,
  ended_at timestamptz
)

-- Patrones clínicos extraídos por IA
clinical_patterns (
  id uuid PK,
  question_id uuid FK questions,
  keywords text[] NOT NULL,
  diagnosis text,
  pearl text,                  -- Perla médica
  distractors text[],
  created_at timestamptz
)
```

---

## Convenciones de Código

### Backend (Java)
- Clases en `PascalCase`, métodos en `camelCase`
- DTOs con sufijo `Request` / `Response` (ej: `CreateQuestionRequest`, `QuestionResponse`)
- Servicios con sufijo `Service`, implementaciones con `ServiceImpl`
- Repositorios con sufijo `Repository`
- Controllers en `/api/v1/{resource}` (versionado desde el inicio)
- Async con `@Async` + `CompletableFuture` para llamadas a IA
- Validación de DTOs con `@Valid` + Bean Validation (jakarta.validation)
- Manejo de errores: lanzar excepciones custom, capturar en `GlobalExceptionHandler`
- Respuesta estándar: `ApiResponse<T>` wrapper para todas las respuestas

### Frontend (TypeScript/React)
- Componentes en `PascalCase.tsx`
- Hooks custom con prefijo `use` (ej: `useQuestions`, `useProgress`)
- Servicios en `camelCase.service.ts`
- Tipos/interfaces en `types/` con sufijo `Type` o `DTO` según sea la respuesta de API
- No usar `any` — siempre tipar explícitamente
- TanStack Query para todas las llamadas al servidor (no useEffect para fetch)
- Zustand solo para estado verdaderamente global (sesión, UI preferences)

---

## Variables de Entorno

### Backend (`application.yml`)
```yaml
SUPABASE_URL: https://xxx.supabase.co
SUPABASE_ANON_KEY: xxx
SUPABASE_SERVICE_KEY: xxx   # Solo backend
SUPABASE_JWT_SECRET: xxx    # Para validar tokens
AI_PROVIDER: openai          # openai | gemini | deepseek
OPENAI_API_KEY: xxx
GEMINI_API_KEY: xxx
DEEPSEEK_API_KEY: xxx
```

### Frontend (`.env`)
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=xxx
```

---

## Flujo de Autenticación

1. Usuario se registra/loguea vía Supabase Auth (frontend SDK)
2. Supabase retorna JWT firmado
3. Frontend incluye `Authorization: Bearer <token>` en cada request
4. Backend valida JWT con `SUPABASE_JWT_SECRET` en filtro Spring Security
5. Extrae `user_id` y `role` del JWT → disponible en contexto de seguridad

---

## CI/CD

| Entorno | Trigger | Pipeline |
|---------|---------|---------|
| Backend (Railway) | Push a `main` | Maven build → Docker → Railway deploy |
| Frontend (Vercel) | Push a `main` | Vite build → Vercel deploy automático |

---

## Decisiones de Arquitectura (ADRs)

| # | Decisión | Razón |
|---|----------|-------|
| ADR-01 | Supabase Auth en lugar de Spring Security custom | Reduce complejidad de auth; JWT nativo compatible con frontend |
| ADR-02 | AI Adapter Pattern | Permite cambiar proveedor de IA sin modificar lógica de negocio |
| ADR-03 | DTOs separados de entidades JPA | Desacopla la API pública del esquema interno de DB |
| ADR-04 | TanStack Query (no Redux) para server state | Manejo de cache, loading y error states sin boilerplate |
| ADR-05 | Supabase Storage para PDFs | Integración nativa con Supabase Auth para RLS por usuario |
| ADR-06 | MapStruct para mapeo Entity↔DTO | Compile-time safe, sin reflection, mayor performance |
| ADR-07 | Versionado de API desde v1 (`/api/v1/`) | Permite evolucionar API sin romper clientes existentes |

---

## Estado del Proyecto

### Fase 1 — Scaffold y Base (En progreso)
- [x] Estructura de directorios
- [x] CLAUDE.md
- [ ] Backend: pom.xml + Spring Boot base
- [ ] Backend: Entidades JPA + Repositorios
- [ ] Backend: Auth (JWT filter + Supabase)
- [ ] Backend: Módulo Q-Bank (CRUD)
- [ ] Backend: Módulo Progress (Semáforo)
- [ ] Backend: AI Adapter Layer
- [ ] Frontend: Setup Vite + Tailwind + shadcn
- [ ] Frontend: Auth pages (Login/Register)
- [ ] Frontend: Layout + Router
- [ ] Frontend: Dashboard
- [ ] Frontend: Q-Bank UI
- [ ] Frontend: Semáforo / Analytics
- [ ] Frontend: Flashcard Generator

### Fase 2 — Features IA
- [ ] Extractor de Keywords (Modo Patrón)
- [ ] Generador de Flashcards con IA
- [ ] Análisis cruzado de academias

### Fase 3 — Deploy y Polish
- [ ] CI/CD Railway + Vercel
- [ ] Supabase RLS policies
- [ ] Tests unitarios + integración
