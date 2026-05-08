import { Lock, RefreshCw, X } from 'lucide-react';
import { Button } from './ui/button';
import type { DocumentLockHolder } from './useDocumentLock';

interface DocumentLockedModalProps {
    holder: DocumentLockHolder | null;
    /** "Erneut versuchen"-Button — z.B. wenn der Halter inzwischen geschlossen hat. */
    onRetry: () => void;
    /** Schliesst den Editor / das Modal. */
    onClose: () => void;
    /** Optionale erweiterte Nachricht (z.B. bei Netzwerkfehler). */
    errorMessage?: string;
}

export default function DocumentLockedModal({
    holder,
    onRetry,
    onClose,
    errorMessage,
}: DocumentLockedModalProps) {
    const seitText = holder ? formatBearbeitetSeit(holder.acquiredAt) : null;
    const name = holder?.displayName?.trim() || 'einem anderen Benutzer';

    return (
        <div className="fixed inset-0 z-[80] bg-black/40 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl border border-slate-100">
                <div className="flex items-center gap-3 mb-4">
                    <div className="p-2.5 bg-rose-50 rounded-xl flex-shrink-0">
                        <Lock className="w-5 h-5 text-rose-500" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900">
                            Dokument wird bearbeitet
                        </h3>
                        <p className="text-xs text-slate-400 mt-0.5">
                            Damit nichts ueberschrieben wird, ist der Editor gerade gesperrt.
                        </p>
                    </div>
                </div>

                {errorMessage ? (
                    <p className="text-sm text-rose-600 mb-6 leading-relaxed">{errorMessage}</p>
                ) : (
                    <p className="text-sm text-slate-600 mb-6 leading-relaxed">
                        Dieses Dokument ist gerade von <span className="font-semibold text-slate-900">{name}</span> geoeffnet
                        {seitText ? <> (seit {seitText})</> : null}. Bitte warten Sie kurz oder fragen Sie den Kollegen,
                        ob er den Editor schliessen kann. Sobald er fertig ist, koennen Sie das Dokument oeffnen.
                    </p>
                )}

                <div className="flex gap-2.5">
                    <Button
                        variant="outline"
                        onClick={onClose}
                        className="flex-1 border-slate-200 text-slate-600"
                    >
                        <X className="w-4 h-4 mr-1.5" />
                        Schliessen
                    </Button>
                    <Button
                        onClick={onRetry}
                        className="flex-1 bg-rose-600 hover:bg-rose-700 text-white shadow-sm"
                    >
                        <RefreshCw className="w-4 h-4 mr-1.5" />
                        Erneut versuchen
                    </Button>
                </div>
            </div>
        </div>
    );
}

function formatBearbeitetSeit(acquiredAtIso: string): string | null {
    const acquiredAt = Date.parse(acquiredAtIso);
    if (Number.isNaN(acquiredAt)) return null;
    const diffSec = Math.max(0, Math.floor((Date.now() - acquiredAt) / 1000));
    if (diffSec < 60) return 'wenigen Sekunden';
    const minutes = Math.floor(diffSec / 60);
    if (minutes < 60) return `${minutes} Min.`;
    const hours = Math.floor(minutes / 60);
    const restMin = minutes % 60;
    if (restMin === 0) return `${hours} Std.`;
    return `${hours} Std. ${restMin} Min.`;
}
