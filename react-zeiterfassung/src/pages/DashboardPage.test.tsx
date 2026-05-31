import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from './DashboardPage'
import { OfflineService } from '../services/OfflineService'

// actual-Spread ist notwendig, weil DashboardPage createOperationId und
// buildBookingRequestPayload direkt aus dem Modul importiert – die müssen
// ihre echten Implementierungen behalten.
vi.mock('../services/OfflineService', async () => {
    const actual = await vi.importActual<typeof import('../services/OfflineService')>(
        '../services/OfflineService',
    )
    return {
        ...actual,
        OfflineService: {
            getFailedEntries: vi.fn(),
            removeFailedEntry: vi.fn(),
            getHeuteGearbeitet: vi.fn(),
            getPendingCount: vi.fn(),
            getUnsyncedStopMinutes: vi.fn(),
            getProjekte: vi.fn(),
            getKategorien: vi.fn(),
            getArbeitsgaenge: vi.fn(),
            addPendingEntryWithOperationId: vi.fn(),
        },
    }
})

const mockedOfflineService = vi.mocked(OfflineService)

function createMemoryStorage(): Storage {
    const store = new Map<string, string>()
    return {
        get length() {
            return store.size
        },
        clear() {
            store.clear()
        },
        getItem(key: string) {
            return store.has(key) ? store.get(key)! : null
        },
        key(index: number) {
            return Array.from(store.keys())[index] ?? null
        },
        removeItem(key: string) {
            store.delete(key)
        },
        setItem(key: string, value: string) {
            store.set(key, value)
        },
    }
}

const renderDashboard = () =>
    render(
        <MemoryRouter>
            <DashboardPage mitarbeiter={{ id: 1, vorname: 'Max', nachname: 'Mustermann' }} onLogout={() => undefined} />
        </MemoryRouter>,
    )

const buildActiveWorkSession = () => ({
    projektId: 77,
    projektName: 'Bauvorhaben Mustermann',
    kundenName: 'Max Mustermann',
    auftragsnummer: 'A-1',
    arbeitsgangId: 11,
    arbeitsgangName: 'Montage',
    produktkategorieId: null,
    produktkategorieName: null,
    startTime: new Date(Date.now() - 30 * 60_000).toISOString(),
    typ: null as const,
})

describe('DashboardPage – Pause-Button', () => {
    beforeEach(() => {
        vi.stubGlobal('localStorage', createMemoryStorage())
        localStorage.setItem('zeiterfassung_token', 'tok-test')
        localStorage.setItem(
            'zeiterfassung_active_session',
            JSON.stringify(buildActiveWorkSession()),
        )

        mockedOfflineService.getFailedEntries.mockResolvedValue([])
        mockedOfflineService.getHeuteGearbeitet.mockResolvedValue({
            stunden: 0,
            minuten: 0,
            fromCache: false,
        })
        // pendingCount=1 ist bewusst gewählt: loadActiveSession überspringt
        // den GET /aktiv-Server-Sync bei pendingCount>0 und behält die lokale
        // Session. Ohne dieses Skip würde die {}-Antwort des Fallback-Mocks
        // (s. einzelne Tests) die Session direkt wieder leeren – siehe
        // DashboardPage.tsx Zeile 212-216.
        mockedOfflineService.getPendingCount.mockResolvedValue(1)
        mockedOfflineService.getUnsyncedStopMinutes.mockResolvedValue(0)
        mockedOfflineService.addPendingEntryWithOperationId.mockResolvedValue(undefined)
    })

    afterEach(() => {
        vi.clearAllMocks()
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('Pause-Tap zeigt sofort die Pause-Session, auch wenn der Server mit 4xx ablehnt', async () => {
        // REGRESSION: zuvor wurde ein Server-4xx in handlePause stumm
        // in die Konsole geloggt – Handwerker sah nichts und konnte die
        // Pause nicht anstechen. Jetzt: optimistisches UI + Offline-Queue.
        const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
            const url = typeof input === 'string' ? input : input.toString()
            if (url.includes('/api/zeiterfassung/pause')) {
                return new Response(
                    JSON.stringify({ error: 'Bereits laufende Pausenbuchung' }),
                    { status: 400, headers: { 'Content-Type': 'application/json' } },
                )
            }
            // Andere Fetches (Permissions, Urlaubsverfall, aktiv) freundlich ablehnen
            return new Response('{}', { status: 200 })
        })
        vi.stubGlobal('fetch', fetchMock)

        const user = userEvent.setup()
        renderDashboard()

        const pauseButton = await screen.findByRole('button', { name: /pause/i })
        await user.click(pauseButton)

        // 1) Pause wurde lokal eingereiht (Offline-Queue / Reparatur-Pfad)
        await waitFor(() => {
            expect(mockedOfflineService.addPendingEntryWithOperationId).toHaveBeenCalledWith(
                'pause',
                expect.objectContaining({ token: 'tok-test' }),
                expect.any(String),
                expect.any(String),
                expect.any(Number), // workDurationMinutes
            )
        })

        // 2) UI zeigt jetzt die Pause-Session – das ist das Symptom, das vorher fehlte.
        //    Präziser Match auf den ☕-Badge, damit der Test nicht zufällig
        //    durch das Wort "Pause" in anderen Strings grün wird.
        expect(await screen.findByText('☕ Pause')).toBeInTheDocument()

        // 3) localStorage hält die Pause-Session (Reload-fest)
        const stored = JSON.parse(localStorage.getItem('zeiterfassung_active_session') || '{}')
        expect(stored.typ).toBe('PAUSE')

        // 4) Cooldown-Flag gesetzt, damit ein nachfolgendes loadActiveSession()
        //    die optimistische Pause nicht durch eine vom Server weiterhin
        //    aktive Arbeitsbuchung überschreibt (Reviewer-Finding).
        expect(localStorage.getItem('zeiterfassung_start_synced_at')).not.toBeNull()
    })

    it('Pause-Tap zeigt die Pause-Session bei erfolgreichem Server-Response (Happy Path)', async () => {
        const serverStartZeit = new Date().toISOString()
        const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
            const url = typeof input === 'string' ? input : input.toString()
            if (url.includes('/api/zeiterfassung/pause')) {
                return new Response(
                    JSON.stringify({ id: 42, startZeit: serverStartZeit, typ: 'PAUSE', status: 'gestartet' }),
                    { status: 200, headers: { 'Content-Type': 'application/json' } },
                )
            }
            return new Response('{}', { status: 200 })
        })
        vi.stubGlobal('fetch', fetchMock)

        const user = userEvent.setup()
        renderDashboard()

        const pauseButton = await screen.findByRole('button', { name: /pause/i })
        await user.click(pauseButton)

        await waitFor(() => {
            const stored = JSON.parse(localStorage.getItem('zeiterfassung_active_session') || '{}')
            expect(stored.typ).toBe('PAUSE')
            expect(stored.startTime).toBe(serverStartZeit)
        })

        // UI rendert die Pause-Session
        expect(await screen.findByText('☕ Pause')).toBeInTheDocument()

        // Im Happy-Path NICHT offline einreihen
        expect(mockedOfflineService.addPendingEntryWithOperationId).not.toHaveBeenCalled()
    })

    it('Pause-Doppeltap erzeugt nur eine Pause-Operation', async () => {
        const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
            const url = typeof input === 'string' ? input : input.toString()
            if (url.includes('/api/zeiterfassung/pause')) {
                await new Promise(resolve => setTimeout(resolve, 25))
                return new Response(
                    JSON.stringify({ id: 42, startZeit: new Date().toISOString(), typ: 'PAUSE', status: 'gestartet' }),
                    { status: 200, headers: { 'Content-Type': 'application/json' } },
                )
            }
            return new Response('{}', { status: 200 })
        })
        vi.stubGlobal('fetch', fetchMock)

        renderDashboard()

        const pauseButton = await screen.findByRole('button', { name: /pause/i })
        pauseButton.click()
        pauseButton.click()

        await screen.findByText('☕ Pause')

        const pauseCalls = fetchMock.mock.calls.filter(([input]) => {
            const url = typeof input === 'string' ? input : input.toString()
            return url.includes('/api/zeiterfassung/pause')
        })
        expect(pauseCalls).toHaveLength(1)
        expect(mockedOfflineService.addPendingEntryWithOperationId).not.toHaveBeenCalled()
    })
})

describe('DashboardPage – loadActiveSession Frisch-Guard', () => {
    // Stellt ein fetch-Mock bereit, bei dem GET /aktiv "keine aktive Buchung"
    // (leeres Objekt, kein .id) liefert. Genau diese verzögerte Server-Antwort
    // hat die frisch angestochene Buchung früher gelöscht.
    const stubFetchAktivLeer = () => {
        const fetchMock = vi.fn(async () => new Response('{}', { status: 200 }))
        vi.stubGlobal('fetch', fetchMock)
        return fetchMock
    }

    beforeEach(() => {
        vi.stubGlobal('localStorage', createMemoryStorage())
        localStorage.setItem('zeiterfassung_token', 'tok-test')

        mockedOfflineService.getFailedEntries.mockResolvedValue([])
        mockedOfflineService.getHeuteGearbeitet.mockResolvedValue({
            stunden: 0,
            minuten: 0,
            fromCache: false,
        })
        // pendingCount=0: voll synchronisiert -> loadActiveSession führt den
        // Server-Abgleich (GET /aktiv) wirklich aus. Das ist der Online-Start-Fall.
        mockedOfflineService.getPendingCount.mockResolvedValue(0)
        mockedOfflineService.getUnsyncedStopMinutes.mockResolvedValue(0)
        mockedOfflineService.addPendingEntryWithOperationId.mockResolvedValue(undefined)
    })

    afterEach(() => {
        vi.clearAllMocks()
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('behält eine frisch angestochene Buchung, wenn GET /aktiv (verzögert) noch aktiv:false liefert', async () => {
        // REGRESSION: Nach dem Online-Anstechen verschwand die "Aktive Buchung"-
        // Karte, weil der Server die frisch committete Buchung noch nicht sah und
        // GET /aktiv aktiv:false lieferte -> lokale Session wurde gelöscht.
        const frischeSession = { ...buildActiveWorkSession(), startTime: new Date().toISOString() }
        localStorage.setItem('zeiterfassung_active_session', JSON.stringify(frischeSession))

        const fetchMock = stubFetchAktivLeer()
        renderDashboard()

        // Warten bis der Server-Abgleich tatsächlich gelaufen ist...
        await waitFor(() => {
            const aktivCall = fetchMock.mock.calls.some(([input]) => {
                const url = typeof input === 'string' ? input : input?.toString() ?? ''
                return url.includes('/api/zeiterfassung/aktiv/')
            })
            expect(aktivCall).toBe(true)
        })

        // ...die Karte bleibt sichtbar und die Session bleibt in localStorage.
        expect(await screen.findByText('Bauvorhaben Mustermann')).toBeInTheDocument()
        expect(localStorage.getItem('zeiterfassung_active_session')).not.toBeNull()
    })

    it('räumt eine alte (nicht frische) Session weiterhin auf, wenn der Server aktiv:false liefert', async () => {
        // NO-REGRESSION: Wurde die Buchung woanders (PC) beendet, muss eine alte
        // lokale Session beim Server-Abgleich verschwinden. Der Frisch-Guard darf
        // das nicht verhindern.
        const alteSession = {
            ...buildActiveWorkSession(),
            startTime: new Date(Date.now() - 60 * 60_000).toISOString(), // 60 Min alt
        }
        localStorage.setItem('zeiterfassung_active_session', JSON.stringify(alteSession))

        stubFetchAktivLeer()
        renderDashboard()

        await waitFor(() => {
            expect(localStorage.getItem('zeiterfassung_active_session')).toBeNull()
        })
        expect(screen.queryByText('Bauvorhaben Mustermann')).not.toBeInTheDocument()
    })
})
