import { useCallback, useEffect, useMemo, useState } from 'react';
import { BarChart3, CheckCircle2, Euro, RefreshCw, Wallet } from 'lucide-react';
import { PageLayout } from '../components/layout/PageLayout';
import { Card } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select } from '../components/ui/select-custom';
import { useToast } from '../components/ui/toast';

interface OffenerPosten {
  typ: 'AUSGANGSRECHNUNG' | 'EINGANGSBELEG';
  id: number;
  nummer: string | null;
  name: string | null;
  datum: string | null;
  faelligAm: string | null;
  brutto: number;
  bezahlt: number;
  offen: number;
  ueberfaellig: boolean;
}

interface Dashboard {
  von: string;
  bis: string;
  umsatzBrutto: number;
  eingangsKostenBrutto: number;
  zahlungseingaenge: number;
  zahlungsausgaenge: number;
  offenerAusgangBrutto: number;
  offeneEingangsBelegeBrutto: number;
  liquiditaet: number;
  ergebnisBrutto: number;
  offeneAusgangsrechnungen: number;
  offeneEingangsbelege: number;
  offenePosten: OffenerPosten[];
}

const currentYear = new Date().getFullYear();
const yearOptions = Array.from({ length: 8 }, (_, index) => {
  const year = String(currentYear - index);
  return { value: year, label: year };
});

const monthOptions = [
  { value: '', label: 'Ganzes Jahr' },
  { value: '1', label: 'Januar' },
  { value: '2', label: 'Februar' },
  { value: '3', label: 'März' },
  { value: '4', label: 'April' },
  { value: '5', label: 'Mai' },
  { value: '6', label: 'Juni' },
  { value: '7', label: 'Juli' },
  { value: '8', label: 'August' },
  { value: '9', label: 'September' },
  { value: '10', label: 'Oktober' },
  { value: '11', label: 'November' },
  { value: '12', label: 'Dezember' },
];

const formatEuro = (value: number | null | undefined) =>
  new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value ?? 0);

const formatDate = (value: string | null | undefined) => {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleDateString('de-DE');
};

export default function FinanzenDashboard() {
  const toast = useToast();
  const [jahr, setJahr] = useState(String(currentYear));
  const [monat, setMonat] = useState('');
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(false);
  const [payingId, setPayingId] = useState<string | null>(null);
  const [zahlung, setZahlung] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ jahr });
      if (monat) params.set('monat', monat);
      const response = await fetch(`/api/finanzen/dashboard?${params.toString()}`);
      if (!response.ok) throw new Error('Finanzdaten konnten nicht geladen werden.');
      setDashboard(await response.json());
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Finanzdaten konnten nicht geladen werden.');
    } finally {
      setLoading(false);
    }
  }, [jahr, monat, toast]);

  useEffect(() => {
    void load();
  }, [load]);

  const kpis = useMemo(() => {
    if (!dashboard) return [];
    return [
      { label: 'Umsatz brutto', value: dashboard.umsatzBrutto, icon: Euro },
      { label: 'Kosten brutto', value: dashboard.eingangsKostenBrutto, icon: Wallet },
      { label: 'Ergebnis brutto', value: dashboard.ergebnisBrutto, icon: BarChart3 },
      { label: 'Liquidität erfasst', value: dashboard.liquiditaet, icon: CheckCircle2 },
    ];
  }, [dashboard]);

  const submitPayment = async (posten: OffenerPosten) => {
    const key = `${posten.typ}-${posten.id}`;
    const betrag = Number(zahlung[key] || posten.offen);
    if (!Number.isFinite(betrag) || betrag <= 0) {
      toast.error('Bitte einen gültigen Betrag eintragen.');
      return;
    }
    setPayingId(key);
    try {
      const url = posten.typ === 'AUSGANGSRECHNUNG'
        ? `/api/finanzen/ausgangs-dokumente/${posten.id}/zahlungen`
        : `/api/finanzen/belege/${posten.id}/zahlungen`;
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          betrag,
          zahlungsdatum: new Date().toISOString().slice(0, 10),
          zahlungsart: posten.typ === 'AUSGANGSRECHNUNG' ? 'Ueberweisung' : 'Bezahlt',
          verwendungszweck: posten.nummer || posten.name || '',
        }),
      });
      if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.message || 'Zahlung konnte nicht gespeichert werden.');
      }
      toast.success('Zahlung gespeichert.');
      setZahlung((prev) => ({ ...prev, [key]: '' }));
      await load();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Zahlung konnte nicht gespeichert werden.');
    } finally {
      setPayingId(null);
    }
  };

  return (
    <PageLayout
      ribbonCategory="Finanzen & Controlling"
      title="Finanzen"
      subtitle="Zentrale Übersicht für Rechnungen, Belege, offene Posten und Zahlungen"
      actions={<Button variant="outline" onClick={() => void load()} disabled={loading}><RefreshCw className="w-4 h-4" />Aktualisieren</Button>}
    >
      <div className="grid grid-cols-1 md:grid-cols-[180px_180px_1fr] gap-3">
        <Select options={yearOptions} value={jahr} onChange={setJahr} />
        <Select options={monthOptions} value={monat} onChange={setMonat} />
        {dashboard && (
          <div className="text-sm text-slate-500 flex items-center">
            Zeitraum {formatDate(dashboard.von)} bis {formatDate(dashboard.bis)}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        {kpis.map((kpi) => {
          const Icon = kpi.icon;
          return (
            <Card key={kpi.label} className="p-4">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-500">{kpi.label}</p>
                <Icon className="w-5 h-5 text-rose-600" />
              </div>
              <p className="mt-3 text-2xl font-semibold text-slate-900">{formatEuro(kpi.value)}</p>
            </Card>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500">Offene Ausgangsrechnungen</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{formatEuro(dashboard?.offenerAusgangBrutto)}</p>
          <p className="mt-1 text-sm text-slate-500">{dashboard?.offeneAusgangsrechnungen ?? 0} Vorgänge</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500">Offene Eingangsbelege</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{formatEuro(dashboard?.offeneEingangsBelegeBrutto)}</p>
          <p className="mt-1 text-sm text-slate-500">{dashboard?.offeneEingangsbelege ?? 0} Vorgänge</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500">Zahlungsfluss</p>
          <p className="mt-2 text-sm text-slate-700">Eingang: {formatEuro(dashboard?.zahlungseingaenge)}</p>
          <p className="mt-1 text-sm text-slate-700">Ausgang: {formatEuro(dashboard?.zahlungsausgaenge)}</p>
        </Card>
      </div>

      <Card className="overflow-hidden">
        <div className="px-4 py-3 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-slate-900">Offene Posten</h2>
            <p className="text-sm text-slate-500">Zahlungen direkt erfassen und offene Beträge reduzieren</p>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-slate-500">
              <tr>
                <th className="text-left px-4 py-3 font-medium">Typ</th>
                <th className="text-left px-4 py-3 font-medium">Nummer</th>
                <th className="text-left px-4 py-3 font-medium">Name</th>
                <th className="text-left px-4 py-3 font-medium">Fällig</th>
                <th className="text-right px-4 py-3 font-medium">Brutto</th>
                <th className="text-right px-4 py-3 font-medium">Offen</th>
                <th className="text-left px-4 py-3 font-medium">Zahlung</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(dashboard?.offenePosten ?? []).map((posten) => {
                const key = `${posten.typ}-${posten.id}`;
                return (
                  <tr key={key} className={posten.ueberfaellig ? 'bg-rose-50/40' : undefined}>
                    <td className="px-4 py-3 text-slate-700">{posten.typ === 'AUSGANGSRECHNUNG' ? 'Ausgang' : 'Eingang'}</td>
                    <td className="px-4 py-3 text-slate-900 font-medium">{posten.nummer || '-'}</td>
                    <td className="px-4 py-3 text-slate-700">{posten.name || '-'}</td>
                    <td className="px-4 py-3 text-slate-700">{formatDate(posten.faelligAm)}</td>
                    <td className="px-4 py-3 text-right text-slate-700">{formatEuro(posten.brutto)}</td>
                    <td className="px-4 py-3 text-right font-semibold text-slate-900">{formatEuro(posten.offen)}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2 min-w-[220px]">
                        <Input
                          type="number"
                          step="0.01"
                          min="0"
                          value={zahlung[key] ?? String(posten.offen)}
                          onChange={(event) => setZahlung((prev) => ({ ...prev, [key]: event.target.value }))}
                        />
                        <Button size="sm" onClick={() => void submitPayment(posten)} disabled={payingId === key}>
                          <CheckCircle2 className="w-4 h-4" />Buchen
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {!loading && (dashboard?.offenePosten.length ?? 0) === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500">Keine offenen Posten vorhanden.</td>
                </tr>
              )}
              {loading && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500">Lade Finanzdaten...</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </PageLayout>
  );
}
