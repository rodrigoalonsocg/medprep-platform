-- ============================================================
-- FIX: "infinite recursion detected in policy for relation user_profiles" (42P17)
--   + backfill de perfiles faltantes en user_profiles
-- Seguro de correr sobre una DB ya existente (idempotente).
-- Pegar y ejecutar en Supabase -> SQL Editor.
-- ============================================================

-- 1) Función helper: comprueba admin SIN disparar RLS (corre como owner).
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_profiles WHERE id = auth.uid() AND role = 'admin'
  );
$$;

-- 2) Reemplazar las políticas recursivas por la función anti-recursión.
DROP POLICY IF EXISTS "user_profiles_admin" ON public.user_profiles;
CREATE POLICY "user_profiles_admin" ON public.user_profiles FOR SELECT
  USING (public.is_admin());

DROP POLICY IF EXISTS "questions_write" ON public.questions;
CREATE POLICY "questions_write" ON public.questions FOR ALL
  USING (public.is_admin());

-- academies / documents: solo si esas tablas ya existen.
DO $$
BEGIN
  IF to_regclass('public.academies') IS NOT NULL THEN
    DROP POLICY IF EXISTS "academies_write" ON public.academies;
    CREATE POLICY "academies_write" ON public.academies FOR ALL
      USING (public.is_admin());
  END IF;

  IF to_regclass('public.documents') IS NOT NULL THEN
    DROP POLICY IF EXISTS "documents_read" ON public.documents;
    CREATE POLICY "documents_read" ON public.documents FOR SELECT
      USING (is_public OR auth.uid() = uploaded_by OR public.is_admin());

    DROP POLICY IF EXISTS "documents_write" ON public.documents;
    CREATE POLICY "documents_write" ON public.documents FOR ALL
      USING (auth.uid() = uploaded_by OR public.is_admin());
  END IF;
END $$;

-- 3) Backfill: crear perfil para todo usuario de auth.users que no tenga fila.
INSERT INTO public.user_profiles (id, full_name, role, university)
SELECT u.id,
       COALESCE(u.raw_user_meta_data->>'full_name', u.email),
       COALESCE(u.raw_user_meta_data->>'role', 'student'),
       u.raw_user_meta_data->>'university'
FROM auth.users u
LEFT JOIN public.user_profiles p ON p.id = u.id
WHERE p.id IS NULL
ON CONFLICT (id) DO NOTHING;
