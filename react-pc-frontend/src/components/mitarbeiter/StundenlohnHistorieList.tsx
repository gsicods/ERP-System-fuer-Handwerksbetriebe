import { useCallback, useEffect, useState } from 'react';
import { Plus, Trash2, Save, X, Edit2, Euro } from 'lucide-react';
import { Button } from '../ui/button';
import { Card } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { DatePicker } from '../ui/datepicker';
import { useToast } from '../ui/toast';
import { useConfirm } from '../ui/confirm-dialog';

interface StundenlohnEintrag {
    id?: number;
    mitarbeiterId?: number;
    stundenlohn: number | string;
    gueltigAb: string;
    bemerkung?: string;
}

export function StundenlohnHistorieList({ mitarbeiterId, onChange }: { mitarbeiterId: number; onChange?: () => void }) {
    const toast = useToast();
    const confirmDialog = useConfirm();
    const [eintraege, setEintraege] = useState<StundenlohnEintrag[]>([]);
    const [editing, setEditing] = useState<StundenlohnEintrag | null>(null);
    const [loading, setLoading] = useState(false);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetch(`/api/mitarbeiter/${encodeURIComponent(String(mitarbeiterId))}/stundenlohn-historie`);
            if (res.ok) setEintraege(await res.json());
        } finally {
            setLoading(false);
        }
    }, [mitarbeiterId]);

    useEffect(() => {
        void load();
    }, [load]);

    const save = async () => {
        if (!editing) return;
        if (!editing.stundenlohn || Number.isNaN(Number(editing.stundenlohn))) {
            toast.error('Bitte einen Stundenlohn eintragen.');
            return;
        }
        if (!editing.gueltigAb) {
            toast.error('Bitte ein Gültig-ab-Datum auswählen.');
            return;
        }
        const url = editing.id
            ? `/api/mitarbeiter/stundenlohn-historie/${encodeURIComponent(String(editing.id))}`
            : `/api/mitarbeiter/${encodeURIComponent(String(mitarbeiterId))}/stundenlohn-historie`;
        const method = editing.id ? 'PUT' : 'POST';
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(editing),
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            toast.error(err.message ?? 'Speichern fehlgeschlagen.');
            return;
        }
        setEditing(null);
        await load();
        onChange?.();
        toast.success('Stundenlohn gespeichert.');
    };

    const remove = async (id: number) => {
        const ok = await confirmDialog({
            title: 'Eintrag löschen?',
            message: 'Diesen Stundenlohn-Eintrag wirklich löschen? Vergangene Auswertungen rechnen damit anders.',
            confirmLabel: 'Löschen',
            variant: 'danger',
        });
        if (!ok) return;
        await fetch(`/api/mitarbeiter/stundenlohn-historie/${encodeURIComponent(String(id))}`, { method: 'DELETE' });
        await load();
        onChange?.();
    };

    const heute = new Date().toISOString().slice(0, 10);
    const aktuellEintrag = eintraege.find(e => e.gueltigAb <= heute);

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-start">
                <div>
                    <h3 className="text-base font-semibold text-slate-900">Stundenlohn-Verlauf</h3>
                    <p className="text-sm text-slate-500">
                        Trage hier ein, ab wann ein neuer Stundenlohn gilt. Alte Zeitbuchungen werden weiterhin mit dem damaligen Stundenlohn bewertet.
                    </p>
                </div>
                <Button
                    onClick={() => setEditing({ stundenlohn: '', gueltigAb: heute })}
                    className="bg-rose-600 text-white hover:bg-rose-700"
                >
                    <Plus className="w-4 h-4 mr-2" />Neuer Eintrag
                </Button>
            </div>

            {aktuellEintrag && (
                <Card className="p-4 bg-rose-50 border-rose-200">
                    <div className="flex items-center gap-3">
                        <Euro className="w-5 h-5 text-rose-600" />
                        <div>
                            <p className="text-xs text-rose-700 font-semibold uppercase tracking-wide">Aktuell gültig</p>
                            <p className="text-lg font-bold text-slate-900">
                                {Number(aktuellEintrag.stundenlohn).toFixed(2)} € pro Stunde
                            </p>
                            <p className="text-xs text-slate-500">seit {aktuellEintrag.gueltigAb}</p>
                        </div>
                    </div>
                </Card>
            )}

            <Card className="p-0 overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-slate-50 text-slate-600">
                        <tr>
                            <th className="text-left px-4 py-2 font-semibold">Gültig ab</th>
                            <th className="text-right px-4 py-2 font-semibold">Stundenlohn</th>
                            <th className="text-left px-4 py-2 font-semibold">Bemerkung</th>
                            <th className="text-right px-4 py-2 font-semibold">Aktionen</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading && (
                            <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Lädt...</td></tr>
                        )}
                        {!loading && eintraege.map(e => (
                            <tr key={e.id} className="border-t border-slate-100 hover:bg-slate-50">
                                <td className="px-4 py-2 font-medium text-slate-900">{e.gueltigAb}</td>
                                <td className="px-4 py-2 text-right tabular-nums">{Number(e.stundenlohn).toFixed(2)} €</td>
                                <td className="px-4 py-2 text-slate-500 max-w-md truncate">{e.bemerkung ?? '—'}</td>
                                <td className="px-4 py-2 text-right">
                                    <Button variant="ghost" size="sm" onClick={() => setEditing({ ...e })}><Edit2 className="w-4 h-4" /></Button>
                                    <Button variant="ghost" size="sm" onClick={() => e.id && remove(e.id)} className="text-red-600 hover:text-red-700"><Trash2 className="w-4 h-4" /></Button>
                                </td>
                            </tr>
                        ))}
                        {!loading && eintraege.length === 0 && (
                            <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Noch keine Stundenlöhne erfasst.</td></tr>
                        )}
                    </tbody>
                </table>
            </Card>

            {editing && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setEditing(null)}>
                    <Card className="w-full max-w-md p-6 space-y-4" onClick={ev => ev.stopPropagation()}>
                        <div className="flex justify-between items-center">
                            <h3 className="text-lg font-semibold text-slate-900">{editing.id ? 'Eintrag bearbeiten' : 'Neuer Stundenlohn'}</h3>
                            <Button variant="ghost" size="sm" onClick={() => setEditing(null)}><X className="w-4 h-4" /></Button>
                        </div>
                        <div className="space-y-3">
                            <div className="space-y-1.5">
                                <Label>Gültig ab *</Label>
                                <DatePicker value={editing.gueltigAb} onChange={v => setEditing({ ...editing, gueltigAb: v })} />
                            </div>
                            <div className="space-y-1.5">
                                <Label>Brutto-Stundenlohn (€) *</Label>
                                <Input type="number" step="0.01" min={0} value={editing.stundenlohn} onChange={e => setEditing({ ...editing, stundenlohn: e.target.value })} />
                            </div>
                            <div className="space-y-1.5">
                                <Label>Bemerkung (optional)</Label>
                                <Input value={editing.bemerkung ?? ''} onChange={e => setEditing({ ...editing, bemerkung: e.target.value })} placeholder="z.B. Tariferhöhung 2026" />
                            </div>
                        </div>
                        <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                            <Button variant="outline" onClick={() => setEditing(null)} className="border-rose-300 text-rose-700 hover:bg-rose-50">Abbrechen</Button>
                            <Button onClick={save} className="bg-rose-600 text-white hover:bg-rose-700"><Save className="w-4 h-4 mr-2" />Speichern</Button>
                        </div>
                    </Card>
                </div>
            )}
        </div>
    );
}
