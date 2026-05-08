import { useSearchParams, useNavigate } from 'react-router-dom';
import DocumentEditor from '../components/DocumentEditor';
import DocumentLockedModal from '../components/DocumentLockedModal';
import { KiHilfeChat } from '../components/KiHilfeChat';
import { useDocumentLock } from '../components/useDocumentLock';

/**
 * Page wrapper for DocumentEditor that reads projektId/anfrageId from URL params.
 * Opens as fullscreen page (no MainLayout sidebar).
 *
 * Soft-Lock: Ein bestehendes (gespeichertes) Dokument darf nur ein User
 * gleichzeitig oeffnen. Solange ein Kollege das Lock haelt, sieht der Caller
 * den DocumentLockedModal statt den Editor. Neu erstellte (noch nicht
 * gespeicherte) Dokumente brauchen kein Lock — der Editor laedt sofort.
 */
export default function DocumentEditorPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const projektId = searchParams.get('projektId');
    const anfrageId = searchParams.get('anfrageId');
    const dokumentId = searchParams.get('dokumentId');
    const dokumentTyp = searchParams.get('dokumentTyp');

    const dokumentIdNum = parseDokumentId(dokumentId);
    const lock = useDocumentLock('AUSGANG', dokumentIdNum);

    const handleClose = () => {
        // Navigate back or close tab
        if (window.history.length > 1) {
            navigate(-1);
        } else {
            window.close();
        }
    };

    if (lock.status === 'locked-by-other') {
        return (
            <DocumentLockedModal
                holder={lock.holder}
                onRetry={lock.retry}
                onClose={handleClose}
            />
        );
    }

    if (lock.status === 'error') {
        return (
            <DocumentLockedModal
                holder={null}
                onRetry={lock.retry}
                onClose={handleClose}
                errorMessage="Verbindung zum Server fehlgeschlagen. Bitte Internetverbindung pruefen und erneut versuchen."
            />
        );
    }

    // Lock wird gerade angefragt — Editor noch nicht zeigen, damit der Nutzer
    // bei einem fremden Lock nicht eine Sekunde lang denkt, das Dokument waere
    // gleich da.
    if (lock.status === 'loading') {
        return (
            <div className="fixed inset-0 flex items-center justify-center bg-slate-50 text-slate-500 text-sm">
                Dokument wird geoeffnet ...
            </div>
        );
    }

    return (
        <>
            <DocumentEditor
                projektId={parseDokumentId(projektId)}
                anfrageId={parseDokumentId(anfrageId)}
                dokumentId={dokumentIdNum}
                initialDokumentTyp={dokumentTyp as import('../types').AusgangsGeschaeftsDokumentTyp | undefined}
                onClose={handleClose}
            />
            <KiHilfeChat />
        </>
    );
}

function parseDokumentId(raw: string | null): number | undefined {
    if (raw == null || raw === '') return undefined;
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
}
