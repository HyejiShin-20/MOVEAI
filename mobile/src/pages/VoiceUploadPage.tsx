import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { routes } from '../routes'
import { reportDraftStore } from '../state/reportDraft'

const uploadIcon = 'https://www.figma.com/api/mcp/asset/e9271131-3c13-4bea-8561-016966c6d748.svg'
const fileIcon = 'https://www.figma.com/api/mcp/asset/b64a0dc3-72e0-462d-acd7-4c4a1762451d.svg'
const laterIcon = 'https://www.figma.com/api/mcp/asset/2e3edad7-2289-44ea-80f2-5623da43400d.svg'

export function VoiceUploadPage() {
  const navigate = useNavigate()

  useEffect(() => {
    reportDraftStore.patch({ stage: 'uploading' })
    const timer = window.setTimeout(() => { reportDraftStore.patch({ stage: 'transcription' }); navigate(routes.reportTranscription) }, 1800)
    return () => window.clearTimeout(timer)
  }, [navigate])

  return (
    <div className="mobile-page voice-upload-page">
      <main className="voice-upload-main">
        <section className="upload-status" aria-live="polite">
          <div className="upload-pulse" aria-hidden="true">
            <span className="upload-pulse__ring upload-pulse__ring--outer" />
            <span className="upload-pulse__ring upload-pulse__ring--middle" />
            <span className="upload-pulse__core"><img src={uploadIcon} alt="" /></span>
          </div>
          <div className="upload-status__copy">
            <h1>Uploading Voice Log</h1>
            <p>음성 기록을 안전하게 저장하고 있습니다...</p>
          </div>
        </section>

        <section className="upload-progress-card">
          <div className="upload-progress" aria-label="업로드 65%">
            <span style={{ width: '65%' }} />
          </div>
          <div className="upload-progress__metrics">
            <span>UPLOADING</span>
            <strong>65%</strong>
          </div>
          <div className="upload-file">
            <img src={fileIcon} alt="" />
            <div>
              <strong>Delivery_Note_B42.wav</strong>
              <span>2.4 MB / 4.1 MB</span>
            </div>
          </div>
        </section>

        <button className="upload-later" type="button" onClick={() => { reportDraftStore.patch({ stage: 'uploading' }); navigate(routes.home) }}>
          <img src={laterIcon} alt="" />
          <span>나중에 이어서 등록</span>
        </button>
      </main>
    </div>
  )
}
