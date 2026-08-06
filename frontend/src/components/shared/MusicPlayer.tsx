import { useRef, useState } from 'react'
import { Music2, Pause } from 'lucide-react'

/** Reproductor flotante de música de piano (persiste montado en el layout). */
export default function MusicPlayer() {
  const audioRef = useRef<HTMLAudioElement>(null)
  const [playing, setPlaying] = useState(false)

  const toggle = () => {
    const a = audioRef.current
    if (!a) return
    if (playing) {
      a.pause()
      setPlaying(false)
    } else {
      a.volume = 0.4
      a.play().then(() => setPlaying(true)).catch(() => setPlaying(false))
    }
  }

  return (
    <>
      <audio ref={audioRef} src="/piano.mp3" loop preload="auto" />
      <button
        onClick={toggle}
        title={playing ? 'Pausar música' : 'Música de piano'}
        className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg transition hover:scale-105"
      >
        {playing && <span className="absolute inset-0 animate-ping rounded-full bg-primary/40" />}
        {playing ? <Pause className="h-6 w-6" /> : <Music2 className="h-6 w-6" />}
      </button>
    </>
  )
}
