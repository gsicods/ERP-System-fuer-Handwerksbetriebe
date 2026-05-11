import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
    acquireCameraStream,
    getActiveCameraStream,
    releaseCameraStream,
} from './cameraStreamService'

interface FakeTrack {
    readyState: MediaStreamTrackState
    stop: () => void
    kind: 'video' | 'audio'
}

function makeFakeStream(): MediaStream {
    const tracks: FakeTrack[] = [
        {
            readyState: 'live',
            kind: 'video',
            stop() { this.readyState = 'ended' },
        },
    ]
    const stream = {
        getTracks: () => tracks as unknown as MediaStreamTrack[],
        getVideoTracks: () => tracks.filter(t => t.kind === 'video') as unknown as MediaStreamTrack[],
    } as unknown as MediaStream
    return stream
}

describe('cameraStreamService', () => {
    let getUserMedia: ReturnType<typeof vi.fn>

    beforeEach(() => {
        releaseCameraStream()
        getUserMedia = vi.fn(async () => makeFakeStream())
        vi.stubGlobal('navigator', {
            mediaDevices: { getUserMedia },
        })
    })

    afterEach(() => {
        releaseCameraStream()
        vi.unstubAllGlobals()
    })

    it('ruft getUserMedia beim ersten acquire genau einmal auf', async () => {
        await acquireCameraStream({ video: true })
        expect(getUserMedia).toHaveBeenCalledTimes(1)
    })

    it('liefert beim zweiten acquire denselben Stream ohne neuen getUserMedia-Aufruf', async () => {
        const first = await acquireCameraStream({ video: true })
        const second = await acquireCameraStream({ video: true })
        expect(second).toBe(first)
        expect(getUserMedia).toHaveBeenCalledTimes(1)
    })

    it('parallele acquire-Aufrufe loesen nur einmal getUserMedia aus (Race-Guard)', async () => {
        const [a, b, c] = await Promise.all([
            acquireCameraStream({ video: true }),
            acquireCameraStream({ video: true }),
            acquireCameraStream({ video: true }),
        ])
        expect(a).toBe(b)
        expect(b).toBe(c)
        expect(getUserMedia).toHaveBeenCalledTimes(1)
    })

    it('getActiveCameraStream liefert null, solange noch kein Stream gecacht ist', () => {
        expect(getActiveCameraStream()).toBeNull()
    })

    it('getActiveCameraStream liefert den gecachten Stream nach acquire', async () => {
        const stream = await acquireCameraStream({ video: true })
        expect(getActiveCameraStream()).toBe(stream)
    })

    it('release stoppt alle Tracks und leert den Cache', async () => {
        const stream = await acquireCameraStream({ video: true })
        const track = stream.getTracks()[0]
        releaseCameraStream()
        expect(track.readyState).toBe('ended')
        expect(getActiveCameraStream()).toBeNull()
    })

    it('acquire nach release loest erneut getUserMedia aus', async () => {
        await acquireCameraStream({ video: true })
        releaseCameraStream()
        await acquireCameraStream({ video: true })
        expect(getUserMedia).toHaveBeenCalledTimes(2)
    })

    it('propagiert Fehler aus getUserMedia (Permission-Deny)', async () => {
        getUserMedia.mockRejectedValueOnce(new Error('Permission denied'))
        await expect(acquireCameraStream({ video: true })).rejects.toThrow(/permission denied/i)
        expect(getActiveCameraStream()).toBeNull()
    })

    it('retried nach einem fehlgeschlagenen acquire mit einem neuen getUserMedia-Aufruf', async () => {
        getUserMedia.mockRejectedValueOnce(new Error('Permission denied'))
        await expect(acquireCameraStream({ video: true })).rejects.toThrow()
        const stream = await acquireCameraStream({ video: true })
        expect(stream).toBeDefined()
        expect(getUserMedia).toHaveBeenCalledTimes(2)
    })
})
