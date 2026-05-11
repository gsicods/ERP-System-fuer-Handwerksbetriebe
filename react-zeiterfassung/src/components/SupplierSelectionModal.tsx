import { useEffect, useRef, useState } from 'react'
import { X, Search, Building2, ChevronRight, Loader2 } from 'lucide-react'

interface Lieferant {
    id: number
    firmenname: string
}

interface LieferantSucheItem {
    id: number
    lieferantenname: string
}
// Backend liefert `{ lieferanten: [...], gesamt, seite, seitenGroesse }`.
interface LieferantSucheResponse {
    lieferanten?: LieferantSucheItem[]
}

interface SupplierSelectionModalProps {
    isOpen: boolean
    onClose: () => void
    onSelect: (lieferant: Lieferant | null) => void
}

const MIN_CHARS = 3
const DEBOUNCE_MS = 300

export function SupplierSelectionModal({ isOpen, onClose, onSelect }: SupplierSelectionModalProps) {
    const [searchTerm, setSearchTerm] = useState('')
    const [results, setResults] = useState<Lieferant[]>([])
    const [loading, setLoading] = useState(false)
    const abortRef = useRef<AbortController | null>(null)

    // In-Flight-Request beim echten Unmount der Komponente abbrechen,
    // damit keine setState-Calls auf einer demontierten Komponente passieren.
    useEffect(() => () => abortRef.current?.abort(), [])

    // Debounced Server-Search ab MIN_CHARS Zeichen. Setzt State nur in
    // async Callbacks (setTimeout/.then/.finally) — niemals synchron im
    // Effect-Body, damit kein react-hooks/set-state-in-effect Lint-Fehler.
    useEffect(() => {
        if (!isOpen) return
        const q = searchTerm.trim()
        if (q.length < MIN_CHARS) return

        const handle = setTimeout(() => {
            abortRef.current?.abort()
            const ctrl = new AbortController()
            abortRef.current = ctrl
            setLoading(true)
            fetch(`/api/lieferanten?q=${encodeURIComponent(q)}&size=50`, { signal: ctrl.signal })
                .then(res => res.ok ? res.json() as Promise<LieferantSucheResponse> : { lieferanten: [] })
                .then(data => {
                    if (ctrl.signal.aborted) return
                    const items = (data.lieferanten ?? [])
                        .filter(l => l.lieferantenname)
                        .map(l => ({ id: l.id, firmenname: l.lieferantenname }))
                    items.sort((a, b) => a.firmenname.localeCompare(b.firmenname, 'de'))
                    setResults(items)
                })
                .catch(err => {
                    if (err?.name !== 'AbortError') setResults([])
                })
                .finally(() => {
                    if (!ctrl.signal.aborted) setLoading(false)
                })
        }, DEBOUNCE_MS)

        return () => clearTimeout(handle)
    }, [searchTerm, isOpen])

    // Bei Close/Select Suchzustand zurücksetzen, ohne setState im Effect-Body.
    const resetAndClose = () => {
        abortRef.current?.abort()
        setSearchTerm('')
        setResults([])
        setLoading(false)
        onClose()
    }
    const pick = (l: Lieferant | null) => {
        abortRef.current?.abort()
        setSearchTerm('')
        setResults([])
        setLoading(false)
        onSelect(l)
    }

    if (!isOpen) return null

    const q = searchTerm.trim()
    const tooShort = q.length > 0 && q.length < MIN_CHARS
    const idle = q.length === 0
    const enoughChars = q.length >= MIN_CHARS
    const showEmpty = enoughChars && !loading && results.length === 0

    return (
        <div className="fixed inset-0 bg-slate-50 z-[60] flex flex-col safe-area-top safe-area-bottom animate-in slide-in-from-bottom duration-200">
            {/* Header */}
            <div className="bg-white border-b border-slate-200 px-4 py-4 flex items-center gap-3 shadow-sm z-10">
                <button
                    onClick={resetAndClose}
                    className="p-2 hover:bg-slate-100 rounded-full transition-colors"
                >
                    <X className="w-6 h-6 text-slate-600" />
                </button>
                <div className="flex-1">
                    <h2 className="text-lg font-bold text-slate-900">Lieferant auswählen</h2>
                    <p className="text-sm text-slate-500">Für Dokument-Zuordnung</p>
                </div>
            </div>

            {/* Content */}
            <div className="flex-1 flex flex-col overflow-hidden">
                {/* Search */}
                <div className="p-4 bg-white border-b border-slate-100">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            placeholder="Lieferant suchen (mind. 3 Zeichen)…"
                            autoFocus
                            className="w-full pl-10 pr-10 py-3 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all"
                        />
                        {loading && (
                            <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-rose-500 animate-spin" />
                        )}
                    </div>
                </div>

                {/* List */}
                <div className="flex-1 overflow-y-auto p-4 space-y-2">
                    {/* "No Supplier" Option */}
                    <button
                        onClick={() => pick(null)}
                        className="w-full bg-slate-100 border border-slate-200 rounded-xl p-4 flex items-center justify-between hover:bg-slate-200 transition-all text-left mb-4"
                    >
                        <span className="font-medium text-slate-600">Ohne Lieferant fortfahren</span>
                        <ChevronRight className="w-5 h-5 text-slate-400" />
                    </button>

                    {idle && (
                        <div className="text-center py-12 text-slate-400">
                            Tippe mindestens {MIN_CHARS} Zeichen ein, um Lieferanten zu suchen.
                        </div>
                    )}

                    {tooShort && (
                        <div className="text-center py-12 text-slate-400">
                            Noch {MIN_CHARS - q.length} Zeichen…
                        </div>
                    )}

                    {showEmpty && (
                        <div className="text-center py-12 text-slate-400">
                            Keine Lieferanten gefunden
                        </div>
                    )}

                    {enoughChars && results.map(lieferant => (
                        <button
                            key={lieferant.id}
                            onClick={() => pick(lieferant)}
                            className="w-full bg-white border border-slate-200 rounded-xl p-4 flex items-center justify-between hover:border-rose-200 hover:shadow-sm active:bg-rose-50 transition-all text-left group"
                        >
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-rose-50 rounded-lg flex items-center justify-center group-hover:bg-rose-100 transition-colors">
                                    <Building2 className="w-5 h-5 text-rose-600" />
                                </div>
                                <span className="font-medium text-slate-900">{lieferant.firmenname}</span>
                            </div>
                            <ChevronRight className="w-5 h-5 text-slate-400 group-hover:text-rose-400" />
                        </button>
                    ))}
                </div>
            </div>
        </div>
    )
}
