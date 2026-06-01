import { useCallback, useRef, useEffect, useState } from 'react';
import { ZoomIn, ZoomOut, Maximize2 } from 'lucide-react';

/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Prüft ob PDF.js (v3 UMD) als window.pdfjsLib verfügbar ist.
 */
function getPdfjsLib(): any | null {
    const lib = (window as any).pdfjsLib;
    if (lib && typeof lib.getDocument === 'function') return lib;
    return null;
}

interface PdfCanvasViewerProps {
    url: string;
    className?: string;
    /** Zoom-Steuerung ein-/ausblenden (Standard: an). */
    showZoomControls?: boolean;
}

const ZOOM_MIN = 0.5;
const ZOOM_MAX = 4;
const ZOOM_STEP = 0.25;

function clampZoom(z: number): number {
    return Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, Math.round(z * 100) / 100));
}

/**
 * Canvas-basierte PDF-Vorschau ohne Browser-PDF-Viewer.
 * Rendert alle Seiten als Canvas-Elemente mit HiDPI-Support.
 *
 * Zoom: Buttons in der schwebenden Toolbar oben links sowie Strg + Mausrad.
 * Das Dokument wird nur einmal pro URL geladen (fetch + parse); ein Zoom-Wechsel
 * rendert lediglich neu – ohne erneuten Netzwerk-Request.
 *
 * Fallback auf iframe mit ausgeblendetem Toolbar falls PDF.js nicht verfügbar.
 */
export function PdfCanvasViewer({ url, className, showZoomControls = true }: PdfCanvasViewerProps) {
    const scrollContainerRef = useRef<HTMLDivElement>(null);
    const canvasContainerRef = useRef<HTMLDivElement>(null);
    const pdfDocRef = useRef<any>(null);
    const renderingRef = useRef(false);
    const rerenderPendingRef = useRef(false);
    const [pageCount, setPageCount] = useState(0);
    const [useFallback, setUseFallback] = useState(false);
    const [fallbackBlobUrl, setFallbackBlobUrl] = useState<string | null>(null);
    const [zoom, setZoom] = useState(1);
    const zoomRef = useRef(1);
    zoomRef.current = zoom;

    /** Rendert alle Seiten des bereits geladenen Dokuments auf Basis von Container-Breite × Zoom. */
    const renderPages = useCallback(async () => {
        const doc = pdfDocRef.current;
        const container = canvasContainerRef.current;
        const scroller = scrollContainerRef.current;
        if (!doc || !container || !scroller) return;

        const containerWidth = scroller.clientWidth;
        if (containerWidth <= 0) return;

        // Läuft schon ein Render? Dann nur vormerken – verhindert überlappende Renders bei schnellem Zoomen.
        if (renderingRef.current) { rerenderPendingRef.current = true; return; }
        renderingRef.current = true;

        try {
            const z = zoomRef.current;
            // In ein Fragment rendern und erst am Ende atomar einhängen → kein Flackern.
            const frag = document.createDocumentFragment();
            for (let i = 1; i <= doc.numPages; i++) {
                const page = await doc.getPage(i);
                const defaultViewport = page.getViewport({ scale: 1 });
                const fitScale = containerWidth / defaultViewport.width;
                const viewport = page.getViewport({ scale: fitScale * z });

                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');
                if (!ctx) continue;

                const dpr = window.devicePixelRatio || 1;
                canvas.width = Math.floor(viewport.width * dpr);
                canvas.height = Math.floor(viewport.height * dpr);
                canvas.style.width = `${Math.floor(viewport.width)}px`;
                canvas.style.height = `${Math.floor(viewport.height)}px`;
                canvas.style.display = 'block';
                ctx.scale(dpr, dpr);

                const wrapper = document.createElement('div');
                wrapper.style.background = 'white';
                wrapper.style.lineHeight = '0';
                // fit-content + margin auto: zentriert bei Zoom < 100%, links bündig + Scrollbar bei Überbreite.
                wrapper.style.width = 'fit-content';
                wrapper.style.margin = '0 auto';
                if (i < doc.numPages) {
                    wrapper.style.marginBottom = '8px';
                }
                wrapper.appendChild(canvas);
                frag.appendChild(wrapper);

                await page.render({ canvasContext: ctx, viewport }).promise;
            }
            container.replaceChildren(frag);
        } catch {
            setUseFallback(true);
        } finally {
            renderingRef.current = false;
            if (rerenderPendingRef.current) {
                rerenderPendingRef.current = false;
                void renderPages();
            }
        }
    }, []);

    /** Lädt das Dokument (fetch + parse) und rendert es anschließend. */
    const loadDocument = useCallback(async (pdfUrl: string) => {
        const pdfjsLib = getPdfjsLib();
        if (!pdfjsLib) {
            setUseFallback(true);
            return;
        }
        try {
            const response = await fetch(pdfUrl);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const data = await response.arrayBuffer();

            if (pdfDocRef.current) {
                try { pdfDocRef.current.destroy(); } catch { /* ok */ }
            }

            const loadingTask = pdfjsLib.getDocument({ data });
            const doc = await loadingTask.promise;
            pdfDocRef.current = doc;
            setPageCount(doc.numPages);
            await renderPages();
        } catch {
            setUseFallback(true);
        }
    }, [renderPages]);

    // Dokument (neu) laden, wenn sich die URL ändert – Zoom dabei zurücksetzen.
    useEffect(() => {
        if (!url) return;
        if (useFallback) return;
        setZoom(1);
        void loadDocument(url);
    }, [url, loadDocument, useFallback]);

    // Bei Zoom-Änderung nur neu rendern (kein erneuter Fetch).
    useEffect(() => {
        if (useFallback) return;
        if (!pdfDocRef.current) return;
        void renderPages();
    }, [zoom, renderPages, useFallback]);

    // Resize-Handler – Fit-Breite an neue Container-Größe anpassen.
    useEffect(() => {
        if (useFallback) return;
        let timer: ReturnType<typeof setTimeout>;
        const onResize = () => {
            clearTimeout(timer);
            timer = setTimeout(() => { void renderPages(); }, 300);
        };
        window.addEventListener('resize', onResize);
        return () => { clearTimeout(timer); window.removeEventListener('resize', onResize); };
    }, [renderPages, useFallback]);

    // Strg + Mausrad zoomt (native, nicht-passiver Listener wegen preventDefault).
    useEffect(() => {
        const el = scrollContainerRef.current;
        if (!el || useFallback) return;
        const onWheel = (e: WheelEvent) => {
            if (!e.ctrlKey) return;
            e.preventDefault();
            setZoom(z => clampZoom(e.deltaY < 0 ? z + ZOOM_STEP : z - ZOOM_STEP));
        };
        el.addEventListener('wheel', onWheel, { passive: false });
        return () => el.removeEventListener('wheel', onWheel);
    }, [useFallback]);

    // Fallback-Pfad: PDF als Blob laden, damit das iframe auch extern gehostete PDFs
    // mit X-Frame-Options anzeigen kann (blob:// ist immer same-origin). Erst danach iframe.
    useEffect(() => {
        if (!useFallback || !url) return;
        let createdUrl: string | null = null;
        let cancelled = false;
        fetch(url)
            .then(res => res.ok ? res.blob() : Promise.reject(new Error(`HTTP ${res.status}`)))
            .then(blob => {
                if (cancelled) return;
                createdUrl = URL.createObjectURL(blob);
                setFallbackBlobUrl(createdUrl);
            })
            .catch(() => { if (!cancelled) setFallbackBlobUrl(null); });
        return () => {
            cancelled = true;
            if (createdUrl) URL.revokeObjectURL(createdUrl);
            setFallbackBlobUrl(null);
        };
    }, [useFallback, url]);

    // Cleanup
    useEffect(() => {
        return () => {
            if (pdfDocRef.current) {
                try { pdfDocRef.current.destroy(); } catch { /* ok */ }
                pdfDocRef.current = null;
            }
        };
    }, []);

    if (useFallback) {
        return (
            <iframe
                src={`${fallbackBlobUrl ?? url}#toolbar=0&navpanes=0&view=FitH`}
                className={className || "w-full h-[70vh] rounded-lg border border-slate-200"}
                style={{ background: 'white' }}
                title="PDF Vorschau"
            />
        );
    }

    const zoomLabel = `${Math.round(zoom * 100)}%`;
    const btnClass = "p-1 rounded-full text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:hover:bg-transparent transition-colors";

    return (
        <div
            ref={scrollContainerRef}
            className={className || "w-full h-[70vh] rounded-lg overflow-y-auto"}
            // overflowX inline überschreibt evtl. mitgegebenes overflow-x-hidden – nötig für horizontales Scrollen beim Zoomen.
            style={{ background: '#f8fafc', overflowX: 'auto' }}
        >
            {(showZoomControls || pageCount > 0) && (
                <div className="sticky top-0 left-0 z-20 flex items-center justify-between px-3 py-1.5 pointer-events-none">
                    {showZoomControls ? (
                        <div className="flex items-center gap-0.5 bg-white/90 backdrop-blur-sm rounded-full border border-slate-200 shadow-sm px-1 py-0.5 pointer-events-auto">
                            <button
                                type="button"
                                onClick={() => setZoom(z => clampZoom(z - ZOOM_STEP))}
                                disabled={zoom <= ZOOM_MIN}
                                title="Verkleinern"
                                aria-label="Verkleinern"
                                className={btnClass}
                            >
                                <ZoomOut className="w-4 h-4" />
                            </button>
                            <span className="text-[11px] tabular-nums text-slate-600 w-10 text-center select-none">
                                {zoomLabel}
                            </span>
                            <button
                                type="button"
                                onClick={() => setZoom(z => clampZoom(z + ZOOM_STEP))}
                                disabled={zoom >= ZOOM_MAX}
                                title="Vergrößern"
                                aria-label="Vergrößern"
                                className={btnClass}
                            >
                                <ZoomIn className="w-4 h-4" />
                            </button>
                            <button
                                type="button"
                                onClick={() => setZoom(1)}
                                title="An Breite anpassen"
                                aria-label="An Breite anpassen"
                                className={btnClass}
                            >
                                <Maximize2 className="w-4 h-4" />
                            </button>
                        </div>
                    ) : <span />}
                    {pageCount > 0 && (
                        <span className="text-[11px] text-slate-400 bg-white/80 backdrop-blur-sm px-2 py-0.5 rounded-full border border-slate-100 pointer-events-auto">
                            {pageCount} {pageCount === 1 ? 'Seite' : 'Seiten'}
                        </span>
                    )}
                </div>
            )}
            <div ref={canvasContainerRef} style={{ lineHeight: 0 }} />
        </div>
    );
}
