/**
 * Lädt OpenCV.js + jscanify on-demand und liefert die vier erkannten
 * Eckpunkte eines Dokuments in einem Bild (in dessen Pixel-Koordinaten).
 *
 * OpenCV.js (~8.6 MB) liegt als statisches Asset unter `/scanner/opencv.js`
 * und wird nur beim ersten Aufruf nachgeladen — der initiale PWA-Bundle
 * bleibt klein. Der Service-Worker-Precache ignoriert diese Datei
 * (siehe vite.config.ts → globIgnores).
 */

export interface Point {
    x: number
    y: number
}

interface Corners {
    topLeftCorner: Point
    topRightCorner: Point
    bottomRightCorner: Point
    bottomLeftCorner: Point
}

interface JscanifyInstance {
    findPaperContour(mat: unknown): unknown | null
    getCornerPoints(contour: unknown): Corners
}

interface JscanifyCtor {
    new(): JscanifyInstance
}

interface OpenCV {
    imread(el: HTMLImageElement | HTMLCanvasElement): { delete(): void }
    onRuntimeInitialized?: () => void
}

declare global {
    interface Window {
        cv?: OpenCV
        jscanify?: JscanifyCtor
    }
}

const OPENCV_URL = `${import.meta.env.BASE_URL}scanner/opencv.js`
const JSCANIFY_URL = `${import.meta.env.BASE_URL}scanner/jscanify.js`

let loadPromise: Promise<JscanifyInstance> | null = null

function loadScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
        // Wenn das Script-Tag schon existiert, neu zu hängen ist unnötig.
        const existing = document.querySelector<HTMLScriptElement>(`script[data-edge-src="${src}"]`)
        if (existing) {
            if (existing.dataset.edgeReady === '1') {
                resolve()
            } else {
                existing.addEventListener('load', () => resolve())
                existing.addEventListener('error', () => reject(new Error(`Skript konnte nicht geladen werden: ${src}`)))
            }
            return
        }
        const s = document.createElement('script')
        s.src = src
        s.async = true
        s.dataset.edgeSrc = src
        s.onload = () => {
            s.dataset.edgeReady = '1'
            resolve()
        }
        s.onerror = () => reject(new Error(`Skript konnte nicht geladen werden: ${src}`))
        document.head.appendChild(s)
    })
}

function waitForOpenCvRuntime(): Promise<void> {
    return new Promise((resolve, reject) => {
        const cv = window.cv
        if (!cv) {
            reject(new Error('window.cv nach Laden nicht vorhanden'))
            return
        }
        // OpenCV.js signalisiert Bereitschaft per onRuntimeInitialized.
        // Falls die Runtime synchron schon fertig ist (zweiter Aufruf),
        // gibt es kein Event mehr — also direkt resolven, sobald die API
        // existiert. Wir testen das per imread-Probe in einem Microtask.
        let resolved = false
        const done = () => {
            if (resolved) return
            resolved = true
            resolve()
        }
        cv.onRuntimeInitialized = done
        // Safety-Net: nach 15 s aufgeben
        setTimeout(() => {
            if (!resolved) reject(new Error('OpenCV.js Runtime-Init-Timeout'))
        }, 15000)
        // Falls Runtime bereits initialisiert wurde, ist `imread` jetzt
        // schon eine Funktion — dann sofort resolven.
        queueMicrotask(() => {
            if (typeof cv.imread === 'function') done()
        })
    })
}

async function ensureLoaded(): Promise<JscanifyInstance> {
    if (loadPromise) return loadPromise
    loadPromise = (async () => {
        // Reihenfolge zwingend: erst OpenCV, dann jscanify (greift auf `cv` zu).
        await loadScript(OPENCV_URL)
        await waitForOpenCvRuntime()
        await loadScript(JSCANIFY_URL)
        const Ctor = window.jscanify
        if (!Ctor) throw new Error('jscanify nicht verfügbar nach Skript-Load')
        return new Ctor()
    })().catch(err => {
        // Bei Fehler erlauben wir einen zweiten Versuch beim nächsten Aufruf.
        loadPromise = null
        throw err
    })
    return loadPromise
}

/**
 * Versucht die vier Eckpunkte des Dokuments im Bild zu erkennen.
 *
 * Liefert die Ecken in der Reihenfolge **TL, TR, BR, BL** in den
 * Pixel-Koordinaten des übergebenen Bildes (`naturalWidth/Height`).
 * Bei Fehler oder fehlender Erkennung wird `null` zurückgegeben —
 * der Aufrufer muss dann auf manuelle Defaults zurückfallen.
 */
export async function detectDocumentCorners(image: HTMLImageElement): Promise<Point[] | null> {
    try {
        const scanner = await ensureLoaded()
        const cv = window.cv
        if (!cv) return null

        const mat = cv.imread(image)
        try {
            const contour = scanner.findPaperContour(mat) as { delete?: () => void } | null
            if (!contour) return null
            try {
                const c = scanner.getCornerPoints(contour)
                if (!c.topLeftCorner || !c.topRightCorner || !c.bottomRightCorner || !c.bottomLeftCorner) {
                    return null
                }
                // Plausibilität: erkannte Fläche muss > 10 % des Bildes sein,
                // sonst hat jscanify wahrscheinlich nur ein kleines Schnipsel
                // gefunden und wir sind mit den 10 %-Defaults besser dran.
                const minArea = image.naturalWidth * image.naturalHeight * 0.1
                const w = Math.hypot(c.topRightCorner.x - c.topLeftCorner.x, c.topRightCorner.y - c.topLeftCorner.y)
                const h = Math.hypot(c.bottomLeftCorner.x - c.topLeftCorner.x, c.bottomLeftCorner.y - c.topLeftCorner.y)
                if (w * h < minArea) return null
                return [c.topLeftCorner, c.topRightCorner, c.bottomRightCorner, c.bottomLeftCorner]
            } finally {
                if (contour && typeof contour.delete === 'function') contour.delete()
            }
        } finally {
            mat.delete()
        }
    } catch (err) {
        console.warn('[EdgeDetector] Erkennung fehlgeschlagen, fallback auf Defaults:', err)
        return null
    }
}

/**
 * Warm-Start: lädt OpenCV.js im Hintergrund, sobald der Scanner geöffnet
 * wird — so ist die Runtime fertig, wenn der User das erste Foto schießt.
 */
export function preloadEdgeDetector(): void {
    void ensureLoaded().catch(() => {
        // Stillschweigend ignorieren — `detectDocumentCorners` versucht es
        // bei Bedarf erneut.
    })
}

/**
 * Synchrone Variante für die Live-Erkennung im Kamera-Bild. Erwartet, dass
 * der Detektor bereits per `preloadEdgeDetector()` geladen wurde — gibt
 * andernfalls `null` zurück. Operiert auf einem Canvas mit dem aktuellen
 * Video-Frame.
 *
 * Liefert die Ecken in der Reihenfolge **TL, TR, BR, BL** in den Pixel-
 * Koordinaten des übergebenen Canvas, oder `null`, wenn keine Erkennung
 * möglich war oder die Fläche kleiner als `minAreaRatio` ist.
 */
export function detectDocumentCornersOnCanvasSync(
    canvas: HTMLCanvasElement,
    minAreaRatio: number = 0.18,
): Point[] | null {
    const cv = window.cv
    if (!cv || typeof cv.imread !== 'function') return null
    const Ctor = window.jscanify
    if (!Ctor) return null

    // jscanify ist zustandslos — Instanz pro Aufruf ist billig genug.
    const scanner = new Ctor()
    let mat: { delete(): void } | null = null
    let contour: { delete?: () => void } | null = null
    try {
        mat = cv.imread(canvas)
        contour = scanner.findPaperContour(mat) as { delete?: () => void } | null
        if (!contour) return null
        const c = scanner.getCornerPoints(contour)
        if (!c.topLeftCorner || !c.topRightCorner || !c.bottomRightCorner || !c.bottomLeftCorner) {
            return null
        }
        // Quad-Fläche per Shoelace gegen Mindestanteil prüfen.
        const pts = [c.topLeftCorner, c.topRightCorner, c.bottomRightCorner, c.bottomLeftCorner]
        let area = 0
        for (let i = 0; i < 4; i++) {
            const j = (i + 1) % 4
            area += pts[i].x * pts[j].y - pts[j].x * pts[i].y
        }
        area = Math.abs(area) / 2
        const minArea = canvas.width * canvas.height * minAreaRatio
        if (area < minArea) return null
        return pts
    } catch {
        return null
    } finally {
        if (contour && typeof contour.delete === 'function') contour.delete()
        if (mat) mat.delete()
    }
}
