-- Academia y subsección por pregunta (correr en Supabase -> SQL Editor).
ALTER TABLE questions ADD COLUMN IF NOT EXISTS academy    text;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS subsection text;
