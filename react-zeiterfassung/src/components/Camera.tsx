import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react'
import { acquireCameraStream, getActiveCameraStream } from '../services/cameraStreamService'

/**
 * Schlanker react-webcam-Ersatz mit folgender API:
 * - getScreenshot(): liefert das aktuelle Video-Frame als JPEG-Data-URL
 *   (vergleichbar mit `Webcam.getScreenshot()` bei
 *   `forceScreenshotSourceSize` + `screenshotQuality`).
 * - video: das underlying HTMLVideoElement (fuer Live-Detection).
 *
 * Im Gegensatz zu react-webcam holt diese Komponente den MediaStream
 * NICHT pro Mount via `getUserMedia`, sondern aus dem Modul-Cache
 * `cameraStreamService`. Dadurch entstehen iOS-PWA-Permission-Prompts
 * nur EINMAL pro Page-Besuch, nicht pro Modal-Oeffnung.
 */
export interface CameraHandle {
    getScreenshot: () => string | null
    readonly video: HTMLVideoElement | null
}

interface CameraProps {
    videoConstraints: MediaTrackConstraints
    screenshotQuality?: number
    className?: string
    onError?: (err: unknown) => void
}

const Camera = forwardRef<CameraHandle, CameraProps>(function Camera(
    { videoConstraints, screenshotQuality = 1, className, onError },
    ref,
) {
    const videoRef = useRef<HTMLVideoElement>(null)

    useEffect(() => {
        let cancelled = false
        const cached = getActiveCameraStream()
        const constraints: MediaStreamConstraints = {
            audio: false,
            video: videoConstraints,
        }

        const attach = (stream: MediaStream) => {
            const video = videoRef.current
            if (!video) return
            if (video.srcObject !== stream) {
                video.srcObject = stream
            }
            const playPromise = video.play()
            if (playPromise && typeof playPromise.catch === 'function') {
                playPromise.catch(() => { /* autoplay-policy: User-Geste war ja schon der Tap auf "Beleg scannen" */ })
            }
        }

        if (cached) {
            attach(cached)
            return () => { cancelled = true }
        }

        acquireCameraStream(constraints)
            .then(stream => {
                if (cancelled) return
                attach(stream)
            })
            .catch(err => {
                if (cancelled) return
                onError?.(err)
            })

        return () => { cancelled = true }
        // Constraints sind im Beleg-Scanner statisch (facingMode=environment,
        // fixe width/height). Nur einmal beim Mount lesen — sonst loest ein
        // neues Inline-Objekt im Parent unnoetig getUserMedia neu aus.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useImperativeHandle(ref, () => ({
        getScreenshot: () => {
            const video = videoRef.current
            if (!video) return null
            if (video.readyState < 2 || !video.videoWidth || !video.videoHeight) return null
            const canvas = document.createElement('canvas')
            canvas.width = video.videoWidth
            canvas.height = video.videoHeight
            const ctx = canvas.getContext('2d')
            if (!ctx) return null
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
            return canvas.toDataURL('image/jpeg', screenshotQuality)
        },
        get video() {
            return videoRef.current
        },
    }), [screenshotQuality])

    return (
        <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className={className}
        />
    )
})

export default Camera
