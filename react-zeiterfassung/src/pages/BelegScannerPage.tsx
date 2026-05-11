import { useEffect, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    ArrowLeft, ScanLine, Upload, Loader2, CheckCircle2, AlertCircle,
    Receipt, RefreshCw, X
} from 'lucide-react'
import ScannerModal from '../components/ScannerModal'

/**
 * Mobile-Beleg-Scanner für die Buchhaltung.
 *
 * Workflow: Foto -> sofort asynchron hochladen -> Scanner ist sofort wieder
 * frei für den nächsten Beleg. Keine Validierung am Handy — alle Korrekturen
 * passieren am PC unter "Belege & Kasse".
 *
 * Die Upload-Queue wird im Hintergrund abgearbeitet und visuell unten als
 * "läuft / fertig / fehlgeschlagen"-Karten dargestellt. Fehlgeschlagene
 * Uploads können retried werden.
 */
type ItemStatus = 'pending' | 'uploading' | 'done' | 'failed'

interface QueueItem {
    localId: string
    file: File
    status: ItemStatus
    serverId?: number
    error?: string
    addedAt: number
}

interface Permissions {
    darfScannen: boolean
    darfSehen: boolean
}

export default function BelegScannerPage() {
    const navigate = useNavigate()
    const [permission, setPermission] = useState<Permissions | null>(null)
    const [permissionLoading, setPermissionLoading] = useState(true)
    const [showScanner, setShowScanner] = useState(false)
    const [queue, setQueue] = useState<QueueItem[]>([])
    const fileInputRef = useRef<HTMLInputElement>(null)

    const token = typeof window !== 'undefined' ? localStorage.getItem('zeiterfassung_token') : null

    useEffect(() => {
        if (!token) {
            setPermission({ darfScannen: false, darfSehen: false })
            setPermissionLoading(false)
            return
        }
        fetch(`/api/buchhaltung/mobile/me/permissions?token=${token}`)
            .then(res => res.ok ? res.json() : { darfScannen: false, darfSehen: false })
            .then(setPermission)
            .catch(() => setPermission({ darfScannen: false, darfSehen: false }))
            .finally(() => setPermissionLoading(false))
    }, [token])

    const enqueue = (file: File) => {
        const item: QueueItem = {
            localId: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
            file,
            status: 'pending',
            addedAt: Date.now(),
        }
        setQueue(q => [item, ...q])
        // Fire-and-forget: kein await — Scanner ist sofort wieder bereit.
        void uploadItem(item)
    }

    const uploadItem = async (item: QueueItem) => {
        setQueue(q => q.map(x => x.localId === item.localId ? { ...x, status: 'uploading' } : x))
        try {
            const fd = new FormData()
            fd.append('datei', item.file)
            const res = await fetch(`/api/buchhaltung/mobile/belege?token=${token}`, {
                method: 'POST',
                body: fd,
            })
            if (!res.ok) {
                const txt = await res.text().catch(() => '')
                throw new Error(`HTTP ${res.status}: ${txt}`)
            }
            const data = await res.json()
            setQueue(q => q.map(x => x.localId === item.localId
                ? { ...x, status: 'done', serverId: data.id }
                : x))
        } catch (err) {
            const msg = err instanceof Error ? err.message : String(err)
            setQueue(q => q.map(x => x.localId === item.localId
                ? { ...x, status: 'failed', error: msg }
                : x))
        }
    }

    const retry = (item: QueueItem) => {
        if (item.status === 'uploading') return
        void uploadItem(item)
    }

    const removeItem = (localId: string) => {
        setQueue(q => q.filter(x => x.localId !== localId))
    }

    const handleScanComplete = async (file: File) => {
        setShowScanner(false)
        enqueue(file)
    }

    const handleFileChoose = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files
        if (!files) return
        for (let i = 0; i < files.length; i++) {
            enqueue(files[i])
        }
        if (fileInputRef.current) fileInputRef.current.value = ''
    }

    // --- Gating ---

    if (permissionLoading) {
        return (
            <div className="h-full flex items-center justify-center bg-slate-50">
                <Loader2 className="w-8 h-8 animate-spin text-rose-600" />
            </div>
        )
    }

    if (!permission?.darfScannen) {
        return (
            <div className="h-full flex flex-col bg-slate-50 safe-area-top">
                <header className="bg-white border-b border-slate-200 px-4 py-4 flex items-center gap-3">
                    <button onClick={() => navigate('/')} className="p-2 hover:bg-slate-100 rounded-lg">
                        <ArrowLeft className="w-5 h-5 text-slate-600" />
                    </button>
                    <h1 className="font-bold text-slate-900">Beleg-Scanner</h1>
                </header>
                <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-slate-500">
                    <AlertCircle className="w-12 h-12 mb-3 text-slate-400" />
                    <p className="font-medium text-slate-700">Keine Berechtigung</p>
                    <p className="text-sm mt-2 max-w-xs">
                        Der Beleg-Scanner ist nur für Mitarbeiter der Buchhaltung verfügbar.
                        Berechtigungen werden unter Administration → Lieferanten-Dokumentenrechte vergeben.
                    </p>
                </div>
            </div>
        )
    }

    const counts = {
        offen: queue.filter(q => q.status === 'pending' || q.status === 'uploading').length,
        fertig: queue.filter(q => q.status === 'done').length,
        fehler: queue.filter(q => q.status === 'failed').length,
    }

    return (
        <div className="h-full flex flex-col bg-slate-50">
            <header className="bg-white border-b border-slate-200 px-4 py-4 safe-area-top flex items-center gap-3">
                <button onClick={() => navigate('/')} className="p-2 hover:bg-slate-100 rounded-lg active:scale-95">
                    <ArrowLeft className="w-5 h-5 text-slate-600" />
                </button>
                <div className="flex-1">
                    <h1 className="font-bold text-slate-900">Beleg-Scanner</h1>
                    <p className="text-xs text-slate-500">Schnellscan – Validierung erfolgt am PC</p>
                </div>
            </header>

            {/* Sticky Aktions-Bereich */}
            <div className="p-4 space-y-3 bg-white border-b border-slate-100 shadow-sm">
                <button
                    onClick={() => setShowScanner(true)}
                    className="w-full bg-rose-600 hover:bg-rose-700 active:scale-[0.98] text-white font-bold rounded-2xl py-5 flex items-center justify-center gap-3 shadow-lg transition-all"
                >
                    <ScanLine className="w-7 h-7" />
                    <span className="text-lg">Beleg scannen</span>
                </button>

                <button
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full bg-white border-2 border-slate-200 hover:border-rose-300 active:scale-[0.98] text-slate-700 font-semibold rounded-2xl py-4 flex items-center justify-center gap-2 transition-all"
                >
                    <Upload className="w-5 h-5" />
                    Aus Galerie wählen
                </button>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*,application/pdf"
                    multiple
                    onChange={handleFileChoose}
                    className="hidden"
                />
            </div>

            {/* Queue-Status */}
            <div className="grid grid-cols-3 gap-2 px-4 py-3 bg-slate-100/50">
                <StatBox label="In Queue" value={counts.offen} color="text-sky-600" />
                <StatBox label="Hochgeladen" value={counts.fertig} color="text-emerald-600" />
                <StatBox label="Fehler" value={counts.fehler} color="text-red-600" />
            </div>

            {/* Liste */}
            <div className="flex-1 overflow-auto p-4 space-y-2 safe-area-bottom">
                {queue.length === 0 ? (
                    <div className="text-center text-slate-400 py-12">
                        <Receipt className="w-12 h-12 mx-auto mb-2 opacity-40" />
                        <p className="text-sm">Noch keine Belege gescannt.</p>
                        <p className="text-xs mt-1">Tippe oben auf <strong>Beleg scannen</strong>.</p>
                    </div>
                ) : queue.map(item => (
                    <QueueRow key={item.localId} item={item} onRetry={retry} onRemove={removeItem} />
                ))}
            </div>

            {showScanner && (
                <ScannerModal
                    onClose={() => setShowScanner(false)}
                    onSave={handleScanComplete}
                />
            )}
        </div>
    )
}

function StatBox({ label, value, color }: { label: string; value: number; color: string }) {
    return (
        <div className="bg-white rounded-xl p-3 text-center border border-slate-200">
            <div className={`text-2xl font-bold tabular-nums ${color}`}>{value}</div>
            <div className="text-xs text-slate-500 mt-0.5">{label}</div>
        </div>
    )
}

function QueueRow({ item, onRetry, onRemove }: {
    item: QueueItem
    onRetry: (item: QueueItem) => void
    onRemove: (localId: string) => void
}) {
    const Icon = item.status === 'done' ? CheckCircle2
        : item.status === 'failed' ? AlertCircle
        : item.status === 'uploading' ? Loader2
        : Receipt
    const iconCls = item.status === 'done' ? 'text-emerald-500'
        : item.status === 'failed' ? 'text-red-500'
        : item.status === 'uploading' ? 'text-sky-500 animate-spin'
        : 'text-slate-400'
    const bg = item.status === 'done' ? 'bg-emerald-50 border-emerald-100'
        : item.status === 'failed' ? 'bg-red-50 border-red-100'
        : 'bg-white border-slate-200'

    const time = new Date(item.addedAt).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })

    return (
        <div className={`flex items-center gap-3 p-3 rounded-xl border ${bg}`}>
            <Icon className={`w-6 h-6 flex-shrink-0 ${iconCls}`} />
            <div className="flex-1 min-w-0">
                <p className="font-medium text-sm text-slate-900 truncate">{item.file.name}</p>
                <p className="text-xs text-slate-500">
                    {item.status === 'pending' && 'Wartet…'}
                    {item.status === 'uploading' && 'Wird hochgeladen…'}
                    {item.status === 'done' && `Hochgeladen (#${item.serverId}) · ${time}`}
                    {item.status === 'failed' && (item.error || 'Fehler')}
                </p>
            </div>
            {item.status === 'failed' && (
                <button onClick={() => onRetry(item)} className="p-2 hover:bg-white rounded-lg" aria-label="Erneut versuchen">
                    <RefreshCw className="w-4 h-4 text-slate-600" />
                </button>
            )}
            {(item.status === 'done' || item.status === 'failed') && (
                <button onClick={() => onRemove(item.localId)} className="p-2 hover:bg-white rounded-lg" aria-label="Entfernen">
                    <X className="w-4 h-4 text-slate-500" />
                </button>
            )}
        </div>
    )
}
