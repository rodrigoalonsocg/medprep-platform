-- ============================================================
-- MedPrep Platform — Schema inicial
-- Ejecutar en Supabase SQL Editor
-- ============================================================

-- Perfiles extendidos (complementa auth.users de Supabase)
CREATE TABLE IF NOT EXISTS user_profiles (
  id          UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name   TEXT,
  role        TEXT NOT NULL DEFAULT 'student' CHECK (role IN ('admin', 'student')),
  university  TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger: crear perfil automáticamente al registrarse
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO user_profiles (id, full_name, role, university)
  VALUES (
    NEW.id,
    NEW.raw_user_meta_data->>'full_name',
    COALESCE(NEW.raw_user_meta_data->>'role', 'student'),
    NEW.raw_user_meta_data->>'university'
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- Especialidades
CREATE TABLE IF NOT EXISTS specialties (
  id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name  TEXT NOT NULL,
  code  TEXT NOT NULL UNIQUE
);

-- Subespecialidades
CREATE TABLE IF NOT EXISTS subspecialties (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  specialty_id UUID NOT NULL REFERENCES specialties(id) ON DELETE CASCADE,
  name         TEXT NOT NULL,
  code         TEXT NOT NULL UNIQUE
);

-- Banco de preguntas
CREATE TABLE IF NOT EXISTS questions (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  specialty_id     UUID NOT NULL REFERENCES specialties(id),
  subspecialty_id  UUID REFERENCES subspecialties(id),
  stem             TEXT NOT NULL,
  option_a         TEXT NOT NULL,
  option_b         TEXT NOT NULL,
  option_c         TEXT,
  option_d         TEXT,
  option_e         TEXT,
  correct_option   CHAR(1) NOT NULL CHECK (correct_option IN ('A','B','C','D','E')),
  explanation      TEXT,
  difficulty       TEXT NOT NULL CHECK (difficulty IN ('BAJA','MEDIA','ALTA')),
  source           TEXT,
  year             INT,
  keywords         TEXT[],
  created_by       UUID REFERENCES user_profiles(id),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Intentos por usuario
CREATE TABLE IF NOT EXISTS question_attempts (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  question_id         UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  selected_option     CHAR(1) CHECK (selected_option IN ('A','B','C','D','E')),
  is_correct          BOOLEAN NOT NULL,
  time_spent_seconds  INT,
  status              TEXT NOT NULL CHECK (status IN ('CORRECTA','INCORRECTA','DUDOSA')),
  attempted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Progreso por especialidad (cache calculado)
CREATE TABLE IF NOT EXISTS user_specialty_progress (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  specialty_id        UUID NOT NULL REFERENCES specialties(id) ON DELETE CASCADE,
  total_attempts      INT NOT NULL DEFAULT 0,
  correct_attempts    INT NOT NULL DEFAULT 0,
  accuracy_percentage NUMERIC(5,2),
  traffic_light       TEXT CHECK (traffic_light IN ('VERDE','AMARILLO','ROJO')),
  last_updated        TIMESTAMPTZ,
  UNIQUE (user_id, specialty_id)
);

-- Flashcards
CREATE TABLE IF NOT EXISTS flashcards (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  question_id  UUID REFERENCES questions(id) ON DELETE SET NULL,
  front        TEXT NOT NULL,
  back         TEXT NOT NULL,
  specialty_id UUID REFERENCES specialties(id),
  is_exported  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Patrones clínicos extraídos por IA
CREATE TABLE IF NOT EXISTS clinical_patterns (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  keywords    TEXT[] NOT NULL,
  diagnosis   TEXT,
  pearl       TEXT,
  distractors TEXT[],
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Sesiones de estudio (Pomodoro)
CREATE TABLE IF NOT EXISTS study_sessions (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  specialty_id   UUID REFERENCES specialties(id),
  duration_minutes INT,
  session_type   TEXT CHECK (session_type IN ('POMODORO','LIBRE')),
  started_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ended_at       TIMESTAMPTZ
);

-- ============================================================
-- Índices para performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_questions_specialty    ON questions(specialty_id);
CREATE INDEX IF NOT EXISTS idx_questions_difficulty   ON questions(difficulty);
CREATE INDEX IF NOT EXISTS idx_attempts_user          ON question_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_attempts_user_question ON question_attempts(user_id, question_id);
CREATE INDEX IF NOT EXISTS idx_progress_user          ON user_specialty_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_flashcards_user        ON flashcards(user_id);

-- ============================================================
-- Row Level Security (RLS)
-- ============================================================
ALTER TABLE user_profiles          ENABLE ROW LEVEL SECURITY;
ALTER TABLE questions              ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_attempts      ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_specialty_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE flashcards             ENABLE ROW LEVEL SECURITY;
ALTER TABLE study_sessions         ENABLE ROW LEVEL SECURITY;

-- user_profiles: cada usuario ve solo su perfil; admin ve todos
CREATE POLICY "user_profiles_self"   ON user_profiles FOR ALL USING (auth.uid() = id);
CREATE POLICY "user_profiles_admin"  ON user_profiles FOR SELECT
  USING ((SELECT role FROM user_profiles WHERE id = auth.uid()) = 'admin');

-- questions: todos los autenticados leen; solo admin escribe
CREATE POLICY "questions_read"   ON questions FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "questions_write"  ON questions FOR ALL
  USING ((SELECT role FROM user_profiles WHERE id = auth.uid()) = 'admin');

-- specialties y subspecialties: lectura pública
ALTER TABLE specialties    ENABLE ROW LEVEL SECURITY;
ALTER TABLE subspecialties ENABLE ROW LEVEL SECURITY;
CREATE POLICY "specialties_read"    ON specialties    FOR SELECT USING (TRUE);
CREATE POLICY "subspecialties_read" ON subspecialties FOR SELECT USING (TRUE);

-- intentos: cada usuario solo ve los suyos
CREATE POLICY "attempts_own" ON question_attempts FOR ALL USING (auth.uid() = user_id);

-- progreso: cada usuario solo ve el suyo
CREATE POLICY "progress_own" ON user_specialty_progress FOR ALL USING (auth.uid() = user_id);

-- flashcards: cada usuario solo ve las suyas
CREATE POLICY "flashcards_own" ON flashcards FOR ALL USING (auth.uid() = user_id);

-- sesiones: cada usuario solo ve las suyas
CREATE POLICY "sessions_own" ON study_sessions FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- Datos semilla — especialidades base
-- ============================================================
INSERT INTO specialties (name, code) VALUES
  ('Medicina Interna',       'MED_INT'),
  ('Cirugía General',        'CIR_GEN'),
  ('Pediatría',              'PEDIAT'),
  ('Ginecología y Obstetricia', 'GINECO')
ON CONFLICT (code) DO NOTHING;

-- Subespecialidades de Medicina Interna
INSERT INTO subspecialties (specialty_id, name, code)
SELECT s.id, sub.name, sub.code FROM specialties s,
  (VALUES
    ('Cardiología',       'CARDIO'),
    ('Gastroenterología', 'GASTRO'),
    ('Nefrología',        'NEFRO'),
    ('Neumología',        'NEUMO'),
    ('Endocrinología',    'ENDOC'),
    ('Neurología',        'NEURO'),
    ('Infectología',      'INFEC'),
    ('Reumatología',      'REUM')
  ) AS sub(name, code)
WHERE s.code = 'MED_INT'
ON CONFLICT (code) DO NOTHING;

-- Subespecialidades de Cirugía General
INSERT INTO subspecialties (specialty_id, name, code)
SELECT s.id, sub.name, sub.code FROM specialties s,
  (VALUES
    ('Traumatología',     'TRAUMA'),
    ('Anestesiología',    'ANEST'),
    ('Abdomen Agudo',     'ABD_AGU'),
    ('Cirugía Vascular',  'CIR_VASC'),
    ('Neurocirugía',      'NEUROC')
  ) AS sub(name, code)
WHERE s.code = 'CIR_GEN'
ON CONFLICT (code) DO NOTHING;

-- Subespecialidades de Pediatría
INSERT INTO subspecialties (specialty_id, name, code)
SELECT s.id, sub.name, sub.code FROM specialties s,
  (VALUES
    ('Neonatología',      'NEONAT'),
    ('Pediatría General', 'PEDIAT_G')
  ) AS sub(name, code)
WHERE s.code = 'PEDIAT'
ON CONFLICT (code) DO NOTHING;

-- Subespecialidades de Ginecología
INSERT INTO subspecialties (specialty_id, name, code)
SELECT s.id, sub.name, sub.code FROM specialties s,
  (VALUES
    ('Obstetricia',       'OBSTET'),
    ('Ginecología',       'GINEC_G'),
    ('Oncología Ginecológica', 'ONCOG')
  ) AS sub(name, code)
WHERE s.code = 'GINECO'
ON CONFLICT (code) DO NOTHING;
