import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { detectDocumentCornersOnCanvasSync } from './DocumentEdgeDetector'

type CvWindow = Window & typeof globalThis & {
    cv?: unknown
    jscanify?: unknown
}

function makeCanvas(width = 480, height = 640): HTMLCanvasElement {
    const c = document.createElement('canvas')
    c.width = width
    c.height = height
    return c
}

describe('detectDocumentCornersOnCanvasSync', () => {
    const w = window as CvWindow
    let originalCv: unknown
    let originalJscanify: unknown

    beforeEach(() => {
        originalCv = w.cv
        originalJscanify = w.jscanify
    })

    afterEach(() => {
        w.cv = originalCv
        w.jscanify = originalJscanify
    })

    it('liefert null, wenn OpenCV nicht geladen ist', () => {
        delete w.cv
        delete w.jscanify
        expect(detectDocumentCornersOnCanvasSync(makeCanvas())).toBeNull()
    })

    it('liefert null, wenn jscanify fehlt, auch wenn cv vorhanden ist', () => {
        w.cv = { imread: () => ({ delete: () => {} }) }
        delete w.jscanify
        expect(detectDocumentCornersOnCanvasSync(makeCanvas())).toBeNull()
    })

    it('liefert null, wenn jscanify keine Kontur findet', () => {
        const matDelete = vi.fn()
        w.cv = { imread: vi.fn(() => ({ delete: matDelete })) }
        w.jscanify = class { findPaperContour() { return null }; getCornerPoints() { return {} } }
        expect(detectDocumentCornersOnCanvasSync(makeCanvas())).toBeNull()
        expect(matDelete).toHaveBeenCalledTimes(1)
    })

    it('liefert die Ecken in der Reihenfolge TL, TR, BR, BL', () => {
        const matDelete = vi.fn()
        const contourDelete = vi.fn()
        w.cv = { imread: () => ({ delete: matDelete }) }
        w.jscanify = class {
            findPaperContour() { return { delete: contourDelete } }
            getCornerPoints() {
                return {
                    topLeftCorner: { x: 50, y: 60 },
                    topRightCorner: { x: 430, y: 70 },
                    bottomRightCorner: { x: 440, y: 580 },
                    bottomLeftCorner: { x: 40, y: 590 },
                }
            }
        }
        const corners = detectDocumentCornersOnCanvasSync(makeCanvas(480, 640))
        expect(corners).toEqual([
            { x: 50, y: 60 },
            { x: 430, y: 70 },
            { x: 440, y: 580 },
            { x: 40, y: 590 },
        ])
        expect(matDelete).toHaveBeenCalledTimes(1)
        expect(contourDelete).toHaveBeenCalledTimes(1)
    })

    it('liefert null, wenn die erkannte Fläche kleiner als minAreaRatio ist', () => {
        w.cv = { imread: () => ({ delete: () => {} }) }
        // Winzig: 10×10 in einer 480×640-Fläche = 100 / 307200 ≈ 0.0003 → unter 0.18
        w.jscanify = class {
            findPaperContour() { return { delete: () => {} } }
            getCornerPoints() {
                return {
                    topLeftCorner: { x: 0, y: 0 },
                    topRightCorner: { x: 10, y: 0 },
                    bottomRightCorner: { x: 10, y: 10 },
                    bottomLeftCorner: { x: 0, y: 10 },
                }
            }
        }
        expect(detectDocumentCornersOnCanvasSync(makeCanvas(480, 640), 0.18)).toBeNull()
    })

    it('räumt mat & contour auch dann auf, wenn getCornerPoints wirft', () => {
        const matDelete = vi.fn()
        const contourDelete = vi.fn()
        w.cv = { imread: () => ({ delete: matDelete }) }
        w.jscanify = class {
            findPaperContour() { return { delete: contourDelete } }
            getCornerPoints() { throw new Error('boom') }
        }
        expect(detectDocumentCornersOnCanvasSync(makeCanvas())).toBeNull()
        expect(matDelete).toHaveBeenCalledTimes(1)
        expect(contourDelete).toHaveBeenCalledTimes(1)
    })

    it('liefert null, wenn ein Eck-Punkt fehlt', () => {
        w.cv = { imread: () => ({ delete: () => {} }) }
        w.jscanify = class {
            findPaperContour() { return { delete: () => {} } }
            getCornerPoints() {
                return {
                    topLeftCorner: { x: 0, y: 0 },
                    topRightCorner: { x: 100, y: 0 },
                    bottomRightCorner: { x: 100, y: 100 },
                    // bottomLeftCorner fehlt
                }
            }
        }
        expect(detectDocumentCornersOnCanvasSync(makeCanvas())).toBeNull()
    })
})
