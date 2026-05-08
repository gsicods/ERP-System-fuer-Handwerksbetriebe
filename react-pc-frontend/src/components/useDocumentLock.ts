import { useEffect, useRef, useState } from 'react';

/**
 * Verwaltet das Soft-Lock fuer ein einzelnes Dokument im Editor.
 *
 * Verhalten:
 *   - Beim Mount mit gueltiger dokumentId wird das Lock erworben (POST /acquire).
 *   - Solange das Lock gehalten wird, pingt ein Heartbeat alle HEARTBEAT_INTERVAL_MS
 *     Sekunden und verhindert, dass der Server-Eintrag verfaellt.
 *   - Vor dem Unmount und beim "pagehide"-Event wird das Lock per DELETE freigegeben.
 *   - Faellt das Schliessen aus (Tab-Crash, Stromverlust), uebernimmt der Server
 *     das Lock automatisch nach 90s an den naechsten User.
 *
 * Status:
 *   idle           - Keine dokumentId, also kein Lock noetig (z.B. neues, noch
 *                    nicht gespeichertes Dokument).
 *   loading        - Acquire-Request laeuft.
 *   acquired       - Caller haelt das Lock und darf bearbeiten.
 *   locked-by-other- Anderer Benutzer haelt das Lock; Editor sollte nicht oeffnen.
 *   error          - Netzwerk-/Serverfehler. UI sollte Retry anbieten.
 */

export type DokumentLockTyp = 'AUSGANG' | 'EINGANG';

export type DocumentLockStatus =
    | 'idle'
    | 'loading'
    | 'acquired'
    | 'locked-by-other'
    | 'error';

export interface DocumentLockHolder {
    userId: number;
    displayName: string;
    acquiredAt: string;
    lastHeartbeatAt: string;
}

interface UseDocumentLockResult {
    status: DocumentLockStatus;
    holder: DocumentLockHolder | null;
    /** Manuell erneut versuchen (z.B. nach Klick auf "Erneut versuchen"). */
    retry: () => void;
    /** True, solange das Lock noch nicht freigegeben wurde — fuer Kontrollen vor dem Speichern. */
    isHeld: boolean;
}

interface AcquireResponse {
    status: 'ACQUIRED' | 'LOCKED_BY_OTHER';
    holderUserId: number;
    holderDisplayName: string;
    acquiredAt: string;
    lastHeartbeatAt: string;
}

const HEARTBEAT_INTERVAL_MS = 30_000;

export function useDocumentLock(
    dokumentTyp: DokumentLockTyp,
    dokumentId: number | null | undefined
): UseDocumentLockResult {
    const [status, setStatus] = useState<DocumentLockStatus>('idle');
    const [holder, setHolder] = useState<DocumentLockHolder | null>(null);
    const [retryNonce, setRetryNonce] = useState(0);

    const heldRef = useRef(false);

    useEffect(() => {
        if (dokumentId == null) {
            heldRef.current = false;
            // status/holder werden durch den Cleanup des vorherigen Effekts
            // zurueckgesetzt — siehe return-Funktion am Ende.
            return;
        }

        const lockUrl = `/api/dokument-locks/${dokumentTyp}/${dokumentId}`;
        const controller = new AbortController();
        let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
        let cancelled = false;

        const stopHeartbeat = () => {
            if (heartbeatTimer != null) {
                clearInterval(heartbeatTimer);
                heartbeatTimer = null;
            }
        };

        const acquire = async () => {
            setStatus('loading');
            try {
                const res = await fetch(`${lockUrl}/acquire`, {
                    method: 'POST',
                    credentials: 'same-origin',
                    signal: controller.signal,
                });
                if (cancelled) return;

                if (res.status === 409) {
                    const data = (await res.json().catch(() => null)) as AcquireResponse | null;
                    setHolder(toHolder(data));
                    setStatus('locked-by-other');
                    heldRef.current = false;
                    return;
                }

                if (!res.ok) {
                    setStatus('error');
                    heldRef.current = false;
                    return;
                }

                const data = (await res.json()) as AcquireResponse;
                setHolder(toHolder(data));
                setStatus('acquired');
                heldRef.current = true;
                heartbeatTimer = setInterval(() => void heartbeat(), HEARTBEAT_INTERVAL_MS);
            } catch (err) {
                if (cancelled || (err instanceof DOMException && err.name === 'AbortError')) {
                    return;
                }
                setStatus('error');
                heldRef.current = false;
            }
        };

        const heartbeat = async () => {
            try {
                const res = await fetch(`${lockUrl}/heartbeat`, {
                    method: 'POST',
                    credentials: 'same-origin',
                });
                if (cancelled) return;

                if (res.status === 409) {
                    const data = (await res.json().catch(() => null)) as AcquireResponse | null;
                    setHolder(toHolder(data));
                    setStatus('locked-by-other');
                    heldRef.current = false;
                    stopHeartbeat();
                    return;
                }

                if (!res.ok) {
                    // Vorruebergehender Serverfehler: Editor offen lassen, naechster
                    // Tick versucht es wieder. Status bleibt 'acquired'.
                    return;
                }

                const data = (await res.json()) as AcquireResponse;
                setHolder(toHolder(data));
                heldRef.current = true;
            } catch {
                // Netzwerk-Hiccups nicht in 'error' kippen — Editor bleibt benutzbar,
                // naechster Heartbeat versucht es erneut.
            }
        };

        const releaseSync = () => {
            if (!heldRef.current) return;
            heldRef.current = false;
            // keepalive: true erlaubt dem Browser, den Request auch nach dem
            // Schliessen des Tabs noch zuzustellen.
            try {
                void fetch(lockUrl, {
                    method: 'DELETE',
                    credentials: 'same-origin',
                    keepalive: true,
                });
            } catch {
                // best effort
            }
        };

        const handlePageHide = () => releaseSync();
        window.addEventListener('pagehide', handlePageHide);

        void acquire();

        return () => {
            cancelled = true;
            controller.abort();
            stopHeartbeat();
            window.removeEventListener('pagehide', handlePageHide);
            if (heldRef.current) {
                heldRef.current = false;
                void fetch(lockUrl, {
                    method: 'DELETE',
                    credentials: 'same-origin',
                    keepalive: true,
                }).catch(() => {
                    // best effort
                });
            }
            // State zuruecksetzen, damit ein erneutes Mounten mit anderer
            // dokumentId nicht den veralteten Status sieht.
            setStatus('idle');
            setHolder(null);
        };
    }, [dokumentTyp, dokumentId, retryNonce]);

    return {
        status,
        holder,
        retry: () => setRetryNonce(n => n + 1),
        isHeld: status === 'acquired',
    };
}

function toHolder(data: AcquireResponse | null): DocumentLockHolder | null {
    if (!data) return null;
    return {
        userId: data.holderUserId,
        displayName: data.holderDisplayName,
        acquiredAt: data.acquiredAt,
        lastHeartbeatAt: data.lastHeartbeatAt,
    };
}
