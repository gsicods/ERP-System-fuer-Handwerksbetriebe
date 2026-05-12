import { useEffect, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    ArrowLeft, ScanLine, Loader2, CheckCircle2, AlertCircle,
    Receipt, RefreshCw, X, Plus, ChevronRight, SplitSquareHorizontal, CheckSquare, ListChecks, FileText,
} from 'lucide-react'
import ScannerModal from '../components/ScannerModal'
import { SupplierSelectionModal } from '../components/SupplierSelectionModal'
import { releaseCameraStream } from '../services/cameraStreamService'

interface LieferantOption {
    id: number
    firmenname: string
}

/**
 * Mobile-Beleg-Scanner mit Wizard-Flow.
 *
 * Schritte pro Beleg:
 *  1) Quelle: Scannen vs. Galerie (PDFs)
 *  2) Lieferant optional zuweisen (oder überspringen)
 *  3) Aufteilung: Ganz für Firma vs. Nur Teile
 *  4) Aufnahme: Kamera-Scan oder PDF-Auswahl
 *
 * Anschliessend laeuft der Upload im Hintergrund, der Scanner ist sofort
 * wieder frei. Bei "Nur Teile" extrahiert der Server die Positionen und
 * der Nutzer hakt sie auf einer Folgeseite an. Bei "Ganz" ist der Mobile-
 * Vorgang abgeschlossen — die Validierung findet am PC statt.
 */
type ItemStatus = 'pending' | 'uploading' | 'done' | 'failed'

type AufteilungsModus = 'VOLLSTAENDIG' | 'TEILWEISE'

type WizardStep = 'idle' | 'source' | 'lieferant' | 'aufteilung'
type CaptureSource = 'scan' | 'gallery'

interface QueueItem {
    localId: string
    file: File
    status: ItemStatus
    serverId?: number
    error?: string
    addedAt: number
    lieferantId?: number
    lieferantName?: string
    aufteilungsModus: AufteilungsModus
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

    // Wizard-State: ein Beleg wird in 3 Schritten konfiguriert, dann aufgenommen.
    const [wizardStep, setWizardStep] = useState<WizardStep>('idle')
    const [draftSource, setDraftSource] = useState<CaptureSource | null>(null)
    const [draftLieferant, setDraftLieferant] = useState<LieferantOption | null>(null)

    // Halte den aktiven Capture-Kontext zwischen Wizard-Ende und Datei-Eingang
    // ausserhalb des Render-Zyklus. Vermeidet State-Race wenn das File-Event
    // noch waehrend des Rerenders nach Sheet-Close hereinkommt.
    const captureContextRef = useRef<{ lieferant: LieferantOption | null; modus: AufteilungsModus } | null>(null)

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

    // Beim Verlassen der Scanner-Page Kamera-Tracks freigeben. Innerhalb der
    // Page bleibt der MediaStream im cameraStreamService gecacht — so wird
    // bei iOS PWA pro Page-Besuch nur EIN Permission-Prompt ausgeloest.
    useEffect(() => {
        return () => { releaseCameraStream() }
    }, [])

    // Vollbild-Overlay fuer den TEILWEISE-Flow: nach Capture wartet die UI
    // hier, bis der Upload durch ist und die Beleg-ID zurueckkommt. Danach
    // navigieren wir direkt zur Positionen-Auswahl-Page, die selbst weiter
    // pollt bis die KI-Extraktion fertig ist. So muss der Buchhalter nicht
    // nochmal in einer Queue auf "Auswaehlen" klicken.
    const [direktUploadDateiname, setDirektUploadDateiname] = useState<string | null>(null)
    // Schutz gegen Doppel-Auslsung (z.B. Doppel-Tap im ScannerModal) und
    // gegen State-Updates nach Unmount (User drueckt Back waehrend Upload).
    const direktUploadInFlightRef = useRef(false)
    const mountedRef = useRef(true)
    useEffect(() => {
        mountedRef.current = true
        return () => { mountedRef.current = false }
    }, [])

    const enqueue = (file: File, lieferant: LieferantOption | null, modus: AufteilungsModus) => {
        // TEILWEISE laeuft NICHT ueber die Hintergrund-Queue — der Nutzer wartet
        // bis die Beleg-ID da ist und landet sofort auf der Positionen-Auswahl.
        if (modus === 'TEILWEISE') {
            void uploadUndNavigiereZuPositionen(file, lieferant)
            return
        }
        const item: QueueItem = {
            localId: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
            file,
            status: 'pending',
            addedAt: Date.now(),
            lieferantId: lieferant?.id,
            lieferantName: lieferant?.firmenname,
            aufteilungsModus: modus,
        }
        setQueue(q => [item, ...q])
        // Fire-and-forget: kein await — Scanner ist sofort wieder bereit.
        void uploadItem(item)
    }

    const uploadUndNavigiereZuPositionen = async (file: File, lieferant: LieferantOption | null) => {
        // Doppel-Guard: Wenn schon ein TEILWEISE-Upload laeuft, ignoriere zweite
        // Auslsung (sonst gehen Belege verloren, weil Overlay ueberschrieben wird).
        if (direktUploadInFlightRef.current) return
        direktUploadInFlightRef.current = true
        setDirektUploadDateiname(file.name)
        try {
            const fd = new FormData()
            fd.append('datei', file)
            const url = new URL(`/api/buchhaltung/mobile/belege`, window.location.origin)
            if (token) url.searchParams.set('token', token)
            if (lieferant?.id != null) url.searchParams.set('lieferantId', String(lieferant.id))
            url.searchParams.set('aufteilungsModus', 'TEILWEISE')
            const res = await fetch(url.pathname + url.search, {
                method: 'POST',
                body: fd,
            })
            if (!res.ok) {
                const txt = await res.text().catch(() => '')
                throw new Error(`HTTP ${res.status}: ${txt}`)
            }
            const data = await res.json()
            // Wenn der User waehrend des Uploads die Page verlassen hat, NICHT
            // mehr navigieren — sonst springt er aus einer ganz anderen Page
            // ploetzlich zur Positionen-Auswahl.
            if (!mountedRef.current) return
            // Direkt rueber — die Page pollt selbst, bis kiAnalyseStatus DONE ist.
            navigate(`/belege/${data.id}/positionen`)
        } catch (err) {
            if (!mountedRef.current) return
            // Bei Fehler nicht spurlos verschwinden: in die Queue als failed,
            // damit der Buchhalter den Beleg sieht und retryen kann.
            const msg = err instanceof Error ? err.message : String(err)
            setQueue(q => [{
                localId: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
                file,
                status: 'failed',
                addedAt: Date.now(),
                error: msg,
                lieferantId: lieferant?.id,
                lieferantName: lieferant?.firmenname,
                aufteilungsModus: 'TEILWEISE',
            }, ...q])
        } finally {
            // Overlay garantiert zumachen, Lock loesen — auch bei Unmount.
            if (mountedRef.current) setDirektUploadDateiname(null)
            direktUploadInFlightRef.current = false
        }
    }

    // Sichtbare Fehler-Karte fuer Dateien, die wir gar nicht erst hochladen
    // (z.B. JPG aus der Galerie auf Android-Browsern, die das accept-Attribut
    // ignorieren). Ohne das wuerde der Tipp einfach stumm verschwinden.
    const pushRejected = (file: File, reason: string, lieferant: LieferantOption | null, modus: AufteilungsModus) => {
        setQueue(q => [{
            localId: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
            file,
            status: 'failed',
            addedAt: Date.now(),
            error: reason,
            lieferantId: lieferant?.id,
            lieferantName: lieferant?.firmenname,
            aufteilungsModus: modus,
        }, ...q])
    }

    const uploadItem = async (item: QueueItem) => {
        setQueue(q => q.map(x => x.localId === item.localId ? { ...x, status: 'uploading' } : x))
        try {
            const fd = new FormData()
            fd.append('datei', item.file)
            const url = new URL(`/api/buchhaltung/mobile/belege`, window.location.origin)
            if (token) url.searchParams.set('token', token)
            if (item.lieferantId != null) url.searchParams.set('lieferantId', String(item.lieferantId))
            url.searchParams.set('aufteilungsModus', item.aufteilungsModus)
            const res = await fetch(url.pathname + url.search, {
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

    // --- Wizard-Flow ---

    const startWizard = () => {
        setDraftSource(null)
        setDraftLieferant(null)
        setWizardStep('source')
    }

    const cancelWizard = () => {
        setWizardStep('idle')
        setDraftSource(null)
        setDraftLieferant(null)
    }

    const pickSource = (src: CaptureSource) => {
        setDraftSource(src)
        setWizardStep('lieferant')
    }

    const handleSupplierPicked = (l: LieferantOption | null) => {
        setDraftLieferant(l)
        setWizardStep('aufteilung')
    }

    const finishWizard = (modus: AufteilungsModus) => {
        const src = draftSource
        if (src === 'scan') {
            captureContextRef.current = { lieferant: draftLieferant, modus }
            setWizardStep('idle')
            setShowScanner(true)
        } else if (src === 'gallery') {
            captureContextRef.current = { lieferant: draftLieferant, modus }
            setWizardStep('idle')
            // Im naechsten Tick triggern, damit das Bottom-Sheet zuerst schliesst.
            setTimeout(() => fileInputRef.current?.click(), 0)
        }
    }

    const handleScanComplete = async (file: File) => {
        setShowScanner(false)
        const ctx = captureContextRef.current
        if (!ctx) {
            // SICHTBAR machen statt silent return — sonst verschwindet der Beleg
            // spurlos und der User sieht "0 in Queue / 0 Fehler" ohne Hinweis.
            pushRejected(file, 'Aufnahme-Kontext verloren — bitte Wizard neu starten', null, 'VOLLSTAENDIG')
            return
        }
        enqueue(file, ctx.lieferant, ctx.modus)
        // ctx bleibt erhalten (wird beim naechsten finishWizard ueberschrieben).
        // Frueher hier null gesetzt → Race mit ScannerModal-Renders moeglich.
    }

    const handleFileChoose = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files
        const ctx = captureContextRef.current
        if (!files || files.length === 0) {
            // User hat den File-Picker mit Cancel geschlossen — nichts zu tun.
            if (fileInputRef.current) fileInputRef.current.value = ''
            return
        }
        if (!ctx) {
            // Files da, aber kein Wizard-Kontext — sichtbar machen statt silent.
            for (let i = 0; i < files.length; i++) {
                pushRejected(files[i], 'Aufnahme-Kontext verloren — bitte Wizard neu starten', null, 'VOLLSTAENDIG')
            }
            if (fileInputRef.current) fileInputRef.current.value = ''
            return
        }
        for (let i = 0; i < files.length; i++) {
            const f = files[i]
            // Defensive: Filepicker akzeptiert zwar nur PDFs (accept-Attr),
            // aber Mobile-Browser ignorieren das teils — also Endung pruefen
            // und bei Mismatch sichtbare Fehler-Karte erzeugen.
            if (!f.type.includes('pdf') && !f.name.toLowerCase().endsWith('.pdf')) {
                pushRejected(f, 'Nur PDF-Dateien erlaubt', ctx.lieferant, ctx.modus)
                continue
            }
            enqueue(f, ctx.lieferant, ctx.modus)
        }
        // ctx NICHT nullen — bleibt bis zum naechsten finishWizard-Aufruf.
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

            {/* Haupt-Aktion: ein einziger Einstieg in den Wizard */}
            <div className="p-4 bg-white border-b border-slate-100 shadow-sm">
                <button
                    onClick={startWizard}
                    className="w-full bg-rose-600 hover:bg-rose-700 active:scale-[0.98] text-white font-bold rounded-2xl py-5 flex items-center justify-center gap-3 shadow-lg transition-all"
                >
                    <Plus className="w-7 h-7" />
                    <span className="text-lg">Beleg hinzufügen</span>
                </button>
            </div>

            <input
                ref={fileInputRef}
                type="file"
                accept="application/pdf"
                multiple
                onChange={handleFileChoose}
                className="hidden"
            />

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
                        <p className="text-xs mt-1">Tippe oben auf <strong>Beleg hinzufügen</strong>.</p>
                    </div>
                ) : queue.map(item => (
                    <QueueRow
                        key={item.localId}
                        item={item}
                        onRetry={retry}
                        onRemove={removeItem}
                        onOpenPositionen={(beleg) =>
                            navigate(`/belege/${beleg}/positionen`)
                        }
                    />
                ))}
            </div>

            {/* Wizard Schritt 1: Quelle waehlen */}
            {wizardStep === 'source' && (
                <SourceSheet
                    onPick={pickSource}
                    onCancel={cancelWizard}
                />
            )}

            {/* Wizard Schritt 2: Lieferant zuweisen oder ueberspringen */}
            <SupplierSelectionModal
                isOpen={wizardStep === 'lieferant'}
                onClose={cancelWizard}
                onSelect={handleSupplierPicked}
                onBack={() => setWizardStep('source')}
            />

            {/* Wizard Schritt 3: Aufteilung waehlen */}
            {wizardStep === 'aufteilung' && (
                <AufteilungSheet
                    onPick={finishWizard}
                    onBack={() => setWizardStep('lieferant')}
                    onCancel={cancelWizard}
                    lieferantName={draftLieferant?.firmenname ?? null}
                />
            )}

            {showScanner && (
                <ScannerModal
                    onClose={() => setShowScanner(false)}
                    onSave={handleScanComplete}
                />
            )}

            {direktUploadDateiname && (
                <DirektUploadOverlay dateiname={direktUploadDateiname} />
            )}
        </div>
    )
}

function DirektUploadOverlay({ dateiname }: { dateiname: string }) {
    return (
        <div className="fixed inset-0 z-[70] bg-slate-900/80 backdrop-blur-sm flex flex-col items-center justify-center p-6 safe-area-top safe-area-bottom">
            <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-sm w-full flex flex-col items-center gap-4 text-center">
                <Loader2 className="w-12 h-12 text-rose-600 animate-spin" />
                <div>
                    <p className="text-lg font-bold text-slate-900">Beleg wird hochgeladen…</p>
                    <p className="text-sm text-slate-500 mt-1 truncate max-w-full">{dateiname}</p>
                </div>
                <p className="text-xs text-slate-500 leading-relaxed">
                    Gleich werden die einzelnen Positionen erkannt — du kannst sie dann direkt
                    anhaken. Bitte einen Moment nicht zurückgehen.
                </p>
            </div>
        </div>
    )
}

// --- Wizard-Bottom-Sheets ---

function SourceSheet({ onPick, onCancel }: {
    onPick: (s: CaptureSource) => void
    onCancel: () => void
}) {
    return (
        <SheetShell title="Wie möchtest du den Beleg erfassen?" onCancel={onCancel}>
            <button
                onClick={() => onPick('scan')}
                className="w-full bg-white border-2 border-slate-200 hover:border-rose-300 active:bg-rose-50 rounded-2xl p-5 flex items-center gap-4 text-left transition-all"
            >
                <div className="w-12 h-12 bg-rose-50 rounded-xl flex items-center justify-center flex-shrink-0">
                    <ScanLine className="w-6 h-6 text-rose-600" />
                </div>
                <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900">Beleg scannen</p>
                    <p className="text-sm text-slate-500 mt-0.5">Mit der Kamera abfotografieren</p>
                </div>
                <ChevronRight className="w-5 h-5 text-slate-400 flex-shrink-0" />
            </button>

            <button
                onClick={() => onPick('gallery')}
                className="w-full bg-white border-2 border-slate-200 hover:border-rose-300 active:bg-rose-50 rounded-2xl p-5 flex items-center gap-4 text-left transition-all"
            >
                <div className="w-12 h-12 bg-rose-50 rounded-xl flex items-center justify-center flex-shrink-0">
                    <FileText className="w-6 h-6 text-rose-600" />
                </div>
                <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900">Aus Galerie auswählen</p>
                    <p className="text-sm text-slate-500 mt-0.5">Nur PDF-Dateien</p>
                </div>
                <ChevronRight className="w-5 h-5 text-slate-400 flex-shrink-0" />
            </button>
        </SheetShell>
    )
}

function AufteilungSheet({ onPick, onBack, onCancel, lieferantName }: {
    onPick: (m: AufteilungsModus) => void
    onBack: () => void
    onCancel: () => void
    lieferantName: string | null
}) {
    return (
        <SheetShell
            title="Was ist auf dem Beleg?"
            subtitle={lieferantName ? `Lieferant: ${lieferantName}` : 'Ohne Lieferant'}
            onCancel={onCancel}
            onBack={onBack}
        >
            <button
                onClick={() => onPick('VOLLSTAENDIG')}
                className="w-full bg-white border-2 border-slate-200 hover:border-rose-300 active:bg-rose-50 rounded-2xl p-5 flex items-center gap-4 text-left transition-all"
            >
                <div className="w-12 h-12 bg-emerald-50 rounded-xl flex items-center justify-center flex-shrink-0">
                    <CheckSquare className="w-6 h-6 text-emerald-600" />
                </div>
                <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900">Ganz für die Firma</p>
                    <p className="text-sm text-slate-500 mt-0.5">Kompletter Beleg wird gebucht</p>
                </div>
                <ChevronRight className="w-5 h-5 text-slate-400 flex-shrink-0" />
            </button>

            <button
                onClick={() => onPick('TEILWEISE')}
                className="w-full bg-white border-2 border-slate-200 hover:border-rose-300 active:bg-rose-50 rounded-2xl p-5 flex items-center gap-4 text-left transition-all"
            >
                <div className="w-12 h-12 bg-amber-50 rounded-xl flex items-center justify-center flex-shrink-0">
                    <SplitSquareHorizontal className="w-6 h-6 text-amber-600" />
                </div>
                <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900">Nur Teile</p>
                    <p className="text-sm text-slate-500 mt-0.5">Positionen nach dem Scan auswählen</p>
                </div>
                <ChevronRight className="w-5 h-5 text-slate-400 flex-shrink-0" />
            </button>
        </SheetShell>
    )
}

function SheetShell({ title, subtitle, onCancel, onBack, children }: {
    title: string
    subtitle?: string
    onCancel: () => void
    onBack?: () => void
    children: React.ReactNode
}) {
    return (
        <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center bg-slate-900/50">
            <div className="w-full sm:max-w-md bg-slate-50 rounded-t-3xl sm:rounded-3xl shadow-2xl safe-area-bottom">
                <div className="px-5 pt-5 pb-3 flex items-start gap-3">
                    {onBack && (
                        <button
                            onClick={onBack}
                            className="p-2 -ml-2 hover:bg-slate-200 rounded-lg"
                            aria-label="Zurück"
                        >
                            <ArrowLeft className="w-5 h-5 text-slate-600" />
                        </button>
                    )}
                    <div className="flex-1 min-w-0">
                        <h2 className="text-lg font-bold text-slate-900">{title}</h2>
                        {subtitle && <p className="text-sm text-slate-500 mt-0.5">{subtitle}</p>}
                    </div>
                    <button
                        onClick={onCancel}
                        className="p-2 -mr-2 hover:bg-slate-200 rounded-lg"
                        aria-label="Abbrechen"
                    >
                        <X className="w-5 h-5 text-slate-600" />
                    </button>
                </div>
                <div className="px-5 pb-6 space-y-3">
                    {children}
                </div>
            </div>
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

function QueueRow({ item, onRetry, onRemove, onOpenPositionen }: {
    item: QueueItem
    onRetry: (item: QueueItem) => void
    onRemove: (localId: string) => void
    onOpenPositionen: (belegId: number) => void
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
                {item.lieferantName && (
                    <p className="text-xs text-slate-500 truncate">→ {item.lieferantName}</p>
                )}
            </div>
            {item.status === 'failed' && (
                <button onClick={() => onRetry(item)} className="p-2 hover:bg-white rounded-lg" aria-label="Erneut versuchen">
                    <RefreshCw className="w-4 h-4 text-slate-600" />
                </button>
            )}
            {item.status === 'done' && item.aufteilungsModus === 'TEILWEISE' && item.serverId != null && (
                <button
                    onClick={() => onOpenPositionen(item.serverId!)}
                    className="px-2 py-1.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold rounded-lg flex items-center gap-1"
                    aria-label="Positionen auswählen"
                >
                    <ListChecks className="w-4 h-4" />
                    Auswählen
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
